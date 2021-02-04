/*
 * Copyright 2019-2020 StreamThoughts.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.streamthoughts.azkarra.http;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import io.streamthoughts.azkarra.api.AzkarraContext;
import io.streamthoughts.azkarra.api.AzkarraContextAware;
import io.streamthoughts.azkarra.api.AzkarraStreamsService;
import io.streamthoughts.azkarra.api.annotations.VisibleForTesting;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.api.errors.AzkarraException;
import io.streamthoughts.azkarra.api.server.AzkarraRestExtension;
import io.streamthoughts.azkarra.api.server.AzkarraRestExtensionContext;
import io.streamthoughts.azkarra.api.server.EmbeddedHttpServer;
import io.streamthoughts.azkarra.api.server.ServerInfo;
import io.streamthoughts.azkarra.client.HttpClientBuilder;
import io.streamthoughts.azkarra.client.authentication.BasicAuthAuthenticator;
import io.streamthoughts.azkarra.client.security.SSLContextFactory;
import io.streamthoughts.azkarra.client.security.SSLUtils;
import io.streamthoughts.azkarra.http.error.AzkarraExceptionMapper;
import io.streamthoughts.azkarra.http.error.ExceptionDefaultHandler;
import io.streamthoughts.azkarra.http.error.ExceptionDefaultResponseListener;
import io.streamthoughts.azkarra.http.handler.HeadlessHttpHandler;
import io.streamthoughts.azkarra.http.query.RestApiQueryCallFactory;
import io.streamthoughts.azkarra.http.routes.WebUIHttpRoutes;
import io.streamthoughts.azkarra.http.security.SecurityConfig;
import io.streamthoughts.azkarra.http.security.SecurityMechanism;
import io.streamthoughts.azkarra.http.security.auth.Authentication;
import io.streamthoughts.azkarra.http.security.auth.AuthenticationContext;
import io.streamthoughts.azkarra.http.security.auth.AuthenticationContextHolder;
import io.streamthoughts.azkarra.http.security.auth.PasswordCredentials;
import io.streamthoughts.azkarra.http.security.handler.SecurityHandler;
import io.streamthoughts.azkarra.http.security.handler.SecurityHandlerFactory;
import io.streamthoughts.azkarra.http.spi.RoutingHandlerProvider;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import okhttp3.OkHttpClient;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.Options;
import org.xnio.SslClientAuthMode;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.servlet.ServletException;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.ServiceLoader;

/**
 * The {@link EmbeddedHttpServer} implementation using <a href="http://undertow.io/">Undertow framework</a>.
 */
public class UndertowEmbeddedServer implements EmbeddedHttpServer {

    private static final Logger LOG = LoggerFactory.getLogger(UndertowEmbeddedServer.class);

    private final Object monitor = new Object();

    private final AzkarraContext context;

    private final boolean enableServiceLoader;

    private ServerInfo serverInfo;

    private Undertow server;

    private final RoutingHandler routing;

    private DeploymentManager manager;

    private volatile boolean started = false;

    private Conf config;

    private ServerConfig serverConfig;

    private SSLContext sslContext;

    private final Collection<AzkarraRestExtension> registeredExtensions = new LinkedList<>();

    /**
     * Creates a new {@link UndertowEmbeddedServer} instance.
     *
     * @param context             the {@link AzkarraContext} instance.
     * @param enableServiceLoader should load {@link RoutingHandler} using service loader.
     */
    UndertowEmbeddedServer(final AzkarraContext context, final boolean enableServiceLoader) {
        this.context = context;
        this.enableServiceLoader = enableServiceLoader;
        this.routing = Handlers.routing();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(final Conf configuration) {
        config = configuration;
        serverConfig = ServerConfig.of(config);
        serverInfo = new ServerInfo(serverConfig.getListener(), serverConfig.getPort(), serverConfig.isSslEnable());

        // Find and register all user-specified Jackson modules
        Collection<Module> jacksonModules = context.getAllComponents(Module.class);
        ExchangeHelper.JSON.registerModules(jacksonModules);

        initializeSslContext();
        registerHttpRemoteStateStoreClient();
    }

    private void initializeSslContext() {
        if (serverConfig.isSslEnable()) {
            TrustManager[] trustManagers;
            try {
                trustManagers = SSLUtils.createTrustManagers(
                        serverConfig.getTrustStoreLocation(),
                        serverConfig.getTruststorePassword(),
                        serverConfig.getTruststoreType(),
                        KeyManagerFactory.getDefaultAlgorithm()
                );
            } catch (CertificateException |
                    NoSuchAlgorithmException |
                    KeyStoreException |
                    IOException e) {
                LOG.error("Could not create trust managers for Client Certificate authentication.", e);
                throw new AzkarraException(e);
            }
            KeyManager[] keyManagers;
            try {
                keyManagers = SSLUtils.createKeyManagers(
                        serverConfig.getKeystoreLocation(),
                        serverConfig.getKeystorePassword(),
                        serverConfig.getKeystoreType(),
                        KeyManagerFactory.getDefaultAlgorithm()
                );
            } catch (CertificateException |
                    NoSuchAlgorithmException |
                    UnrecoverableKeyException |
                    KeyStoreException |
                    IOException e) {
                LOG.error("Could not create key managers for Client Certificate authentication.", e);
                throw new AzkarraException(e);
            }
            SSLContextFactory sslContextFactory = new SSLContextFactory();
            sslContext = sslContextFactory.getSSLContext(keyManagers, trustManagers);
        }
    }

    private void registerHttpRemoteStateStoreClient() {
        final HttpClientBuilder httpClientBuilder = HttpClientBuilder.newBuilder();
        if (serverConfig.isSslEnable()) {
            httpClientBuilder
                .sslContext(sslContext)
                .verifyingSsl(serverConfig.isHostnameVerificationIgnored());
        }
        if (serverConfig.isRestAuthenticationEnable()) {
            httpClientBuilder
                .authenticator(new BasicAuthAuthenticator(() -> {
                    AuthenticationContext context = AuthenticationContextHolder.getAuthenticationContext();
                    SecurityMechanism securityMechanism = context.getSecurityMechanism();
                    if (securityMechanism == SecurityMechanism.BASIC_AUTH) {
                        Authentication authentication = context.getAuthentication();
                        final String password = ((PasswordCredentials) authentication.getCredentials()).password();
                        final String username = authentication.getPrincipal().getName();
                        return new BasicAuthAuthenticator.Credential(username, password);
                    }
                    return BasicAuthAuthenticator.Credential.empty();
                }));
        }
        final OkHttpClient httpClient = httpClientBuilder.build();
        final String protocol = serverConfig.isSslEnable() ? "https" : "http";
        context.registerSingleton(new RestApiQueryCallFactory(httpClient, protocol));
    }

    private Undertow buildUndertowServer() {
        final Undertow.Builder sb = Undertow.builder()
            .setServerOption(UndertowOptions.ENABLE_HTTP2, true);

        if (sslContext != null)
            sb.addHttpsListener(serverInfo.getPort(), serverInfo.getHost(), sslContext);
        else
            sb.addHttpListener(serverInfo.getPort(), serverInfo.getHost());

        HttpHandler handler;

        if (serverConfig.isRestExtensionEnable()) {
            // fallback to the ServletHandler when no route was found
            handler = initializeServletPathHandler(this::initializeRouterPathHandler);
        } else {
            handler = initializeRouterPathHandler(ResponseCodeHandler.HANDLE_404);
        }

        if (serverConfig.isHeadless()) {
            handler = new HeadlessHttpHandler(handler);
        }

        if (serverConfig.isRestAuthenticationEnable()) {
            handler = initializeSecurityPathHandler(sb, serverConfig, handler);
        }

        // DefaultResponseHandler must always be the first handler in the chain.
        // DO NOT define any more HttpHandlers after the line below.
        handler = new DefaultResponseHandler(handler);

        return sb.setHandler(handler).build();
    }

    private HttpHandler initializeRouterPathHandler(final HttpHandler fallbackHandler) {
        // Register all routing http-handlers using server loader.
        if (enableServiceLoader) {
            ServiceLoader<RoutingHandlerProvider> serviceLoader = ServiceLoader.load(RoutingHandlerProvider.class);
            serviceLoader.forEach(this::addRoutingHandler);
        }
        // Add handler to serve static resources for WebUI.
        if (serverConfig.isUIEnable()) {
            addRoutingHandler(new WebUIHttpRoutes());
        }

        return Handlers
                .exceptionHandler(routing.setFallbackHandler(fallbackHandler))
                .addExceptionHandler(Throwable.class, new ExceptionDefaultHandler());
    }

    private HttpHandler initializeSecurityPathHandler(final Undertow.Builder builder,
                                                      final SecurityConfig securityConfig,
                                                      final HttpHandler handler) {

        SecurityHandlerFactory factory = new SecurityHandlerFactory(context);

        SecurityHandler securityHandler = factory.make(securityConfig, handler);

        // enable two-way authentication
        if (securityHandler.getSecurityMechanism() == SecurityMechanism.CLIENT_CERT_AUTH) {
            builder.setSocketOption(Options.SSL_CLIENT_AUTH_MODE, SslClientAuthMode.REQUIRED);
        }

        return securityHandler;
    }

    /**
     * Initialize the servlet path {@link HttpHandler}.
     *
     * @param initialHandlerChainWrapper the {@link HandlerWrapper} to invoke before any Servlet handlers.
     * @return the servlet path {@link HttpHandler}.
     */
    private HttpHandler initializeServletPathHandler(final HandlerWrapper initialHandlerChainWrapper) {
        LOG.info("Initializing ServletHandler");
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(new JacksonJsonProvider(ExchangeHelper.JSON.unwrap()));
        resourceConfig.register(new AzkarraExceptionMapper());
        resourceConfig.property(ServerProperties.WADL_FEATURE_DISABLE, true);
        registerRestExtensions(resourceConfig);

        final ServletContainer servletContainer = new ServletContainer(resourceConfig);

        final ImmediateInstanceFactory<ServletContainer> servlet = new ImmediateInstanceFactory<>(servletContainer);
        DeploymentInfo servletBuilder = Servlets.deployment()
                .setDeploymentName("azkarraDeployment")
                .setClassLoader(UndertowEmbeddedServer.class.getClassLoader())
                .setContextPath("/")
                .addServlet(
                        Servlets.servlet("jerseyServlet", ServletContainer.class, servlet)
                                .setLoadOnStartup(1)
                                .addMapping("/*")
                )
                .addInitialHandlerChainWrapper(initialHandlerChainWrapper);
        manager = Servlets.defaultContainer().addDeployment(servletBuilder);
        manager.deploy();

        try {
            return manager.start();
        } catch (ServletException e) {
            throw new AzkarraException("Unable to start servlet container", e);
        }
    }

    private void registerRestExtensions(final ResourceConfig resourceConfig) {
        LOG.info("Initializing JAX-RS resources");
        var extensionContext = new InternalRestExtensionContext(resourceConfig, context);
        LOG.debug("Loading extensions using ClassLoaders: {}", context.getComponentFactory().getAllClassLoaders());
        context.getComponentFactory()
            .loadAllServices(AzkarraRestExtension.class)
            .forEach(extension -> {
                LOG.info("Registering AzkarraRestExtension: {}", extension.getClass().getName());
                registeredExtensions.add(extension);
                extension.configure(config);
                extension.register(extensionContext);
            });
    }

    @VisibleForTesting
    void addRoutingHandler(final RoutingHandlerProvider provider) {
        Configurable.mayConfigure(provider, config);
        if (provider instanceof AzkarraContextAware) {
            ((AzkarraContextAware) provider).setAzkarraContext(context);
        }
        LOG.info("Loading HTTP routes from provided class '{}'", provider.getClass().getName());
        final AzkarraStreamsService service = context.getComponent(AzkarraStreamsService.class);
        routing.addAll(provider.handler(service));
    }

    @VisibleForTesting
    Collection<AzkarraRestExtension> getRegisteredExtensions() {
        return registeredExtensions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServerInfo info() {
        return serverInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() {
        synchronized (this.monitor) {
            LOG.info("Starting Undertow embedded REST server");
            try {
                server = buildUndertowServer();
                server.start();
                started = true;
                LOG.info("Undertow embedded REST server is started and listening at {}", serverInfo);
            } catch (Exception e){
                try {
                    throw new AzkarraException("Unable to start Undertow embedded REST server", e);
                } finally {
                    stopServerSilently();
                }
            }
        }
    }

    private void stopServerSilently() {
        if (server != null) {
            try {
                server.stop();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        synchronized (this.monitor) {
            if (!started) return;
            LOG.info("Stopping Undertow embedded REST server");
            started = false;
            if (server != null) {
                for (AzkarraRestExtension extension : registeredExtensions) {
                    try {
                        extension.close();
                    } catch (Exception ex) {
                        var className = extension.getClass().getName();
                        LOG.error("Error happens while closing AzkarraRestExtension: {}", className, ex);
                    }
                }
                try {
                    if (manager != null) {
                        manager.stop();
                        manager.undeploy();
                    }
                    server.stop();
                    LOG.info("Undertow embedded REST server stopped");
                } catch (Exception ex) {
                    LOG.error("Failed to stop Undertow embedded REST server", ex);
                }
            }
        }
    }

    private static final class DefaultResponseHandler implements HttpHandler {

        private final HttpHandler handler;

        /**
         * Creates a new {@link DefaultResponseHandler} instance.
         *
         * @param handler the {@link HttpHandler} instance.
         */
        DefaultResponseHandler(final HttpHandler handler) {
            this.handler = handler;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleRequest(final HttpServerExchange exchange) throws Exception {

            exchange.addDefaultResponseListener(new ExceptionDefaultResponseListener());
            handler.handleRequest(exchange);
        }
    }

    public static class InternalRestExtensionContext implements AzkarraRestExtensionContext {

        private final ResourceConfig resourceConfig;
        private final AzkarraContext context;

        InternalRestExtensionContext(final ResourceConfig resourceConfig,
                                     final AzkarraContext context) {
            this.resourceConfig = resourceConfig;
            this.context = context;
        }

        @Override
        public javax.ws.rs.core.Configurable<? extends javax.ws.rs.core.Configurable> configurable() {
            return resourceConfig;
        }

        @Override
        public AzkarraContext context() {
            return context;
        }
    }
}
