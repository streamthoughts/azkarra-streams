/*
 * Copyright 2019 StreamThoughts.
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

import io.streamthoughts.azkarra.api.AzkarraContext;
import io.streamthoughts.azkarra.api.AzkarraContextAware;
import io.streamthoughts.azkarra.api.AzkarraStreamsService;
import io.streamthoughts.azkarra.api.annotations.VisibleForTesting;
import io.streamthoughts.azkarra.api.components.ComponentFactory;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.api.server.EmbeddedHttpServer;
import io.streamthoughts.azkarra.api.server.ServerInfo;
import io.streamthoughts.azkarra.http.data.ErrorMessage;
import io.streamthoughts.azkarra.http.error.ExceptionDefaultHandler;
import io.streamthoughts.azkarra.http.error.ExceptionDefaultResponseListener;
import io.streamthoughts.azkarra.http.query.HttpRemoteQueryBuilder;
import io.streamthoughts.azkarra.http.security.SSLContextFactory;
import io.streamthoughts.azkarra.http.security.SecurityConfig;
import io.streamthoughts.azkarra.http.security.SecurityMechanism;
import io.streamthoughts.azkarra.http.security.handler.SecurityHandler;
import io.streamthoughts.azkarra.http.security.handler.SecurityHandlerFactory;
import io.streamthoughts.azkarra.http.spi.RoutingHandlerProvider;
import io.streamthoughts.azkarra.runtime.service.LocalAzkarraStreamsService;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.Options;
import org.xnio.SslClientAuthMode;

import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;

public class UndertowEmbeddedServer implements EmbeddedHttpServer {

    private static final Logger LOG = LoggerFactory.getLogger(UndertowEmbeddedServer.class);

    private static final String HTTP_PORT_CONFIG                    = "port";
    private static final int HTTP_PORT_DEFAULT                      = 8080;
    private static final String HTTP_LISTENER_LISTER_CONFIG         = "listener";
    private static final String HTTP_LISTENER_DEFAULT               = "localhost";

    private final AzkarraContext context;

    private ServerInfo serverInfo;

    private AzkarraStreamsService service;

    private Undertow server;

    private RoutingHandler routing;

    private final boolean enableServiceLoader;

    private Conf config;

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
        final SecurityConfig securityConfig = new SecurityConfig(config);

        serverInfo = new ServerInfo(
            config.getOptionalString(HTTP_LISTENER_LISTER_CONFIG).orElse(HTTP_LISTENER_DEFAULT),
            config.getOptionalInt(HTTP_PORT_CONFIG).orElse(HTTP_PORT_DEFAULT)
        );

        HttpRemoteQueryBuilder httpRemoteQueryBuilder = new HttpRemoteQueryBuilder()
            .setBasePath(APIVersions.PATH_V1);

        final Undertow.Builder sb = Undertow.builder()
            .setServerOption(UndertowOptions.ENABLE_HTTP2, true);

        if (securityConfig.isSslEnable()) {
            final SSLContextFactory sslContextFactory = new SSLContextFactory(securityConfig);
            httpRemoteQueryBuilder.setSSLContextFactory(sslContextFactory);
            httpRemoteQueryBuilder.setIgnoreHostnameVerification(securityConfig.isHostnameVerificationIgnored());

            sb.addHttpsListener(serverInfo.getPort(), serverInfo.getHost(), sslContextFactory.getSSLContext());
        } else {
            sb.addHttpListener(serverInfo.getPort(), serverInfo.getHost());
        }

        if (securityConfig.isRestAuthenticationEnable()) {
            httpRemoteQueryBuilder.enablePasswordAuthentication(true);
        }

        service = new LocalAzkarraStreamsService(context, httpRemoteQueryBuilder.build());
        context.addComponent(ComponentFactory.singletonOf(service));

        if (enableServiceLoader) {
            loadRoutingHandlersFromServiceProviders();
        }

        HttpHandler handler = Handlers
                .exceptionHandler(routing)
                .addExceptionHandler(Throwable.class, new ExceptionDefaultHandler());

        handler = new DefaultResponseHandler(handler, securityConfig.isHeadless());

        if (securityConfig.isRestAuthenticationEnable()) {
            handler = addSecurityHttpHandler(sb, securityConfig, handler);
        }

        server = sb.setHandler(handler).build();
    }

    private HttpHandler addSecurityHttpHandler(final Undertow.Builder builder,
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

    @VisibleForTesting
    void addRoutingHandler(final RoutingHandlerProvider provider) {
        if (provider instanceof AzkarraContextAware) {
            ((AzkarraContextAware) provider).setAzkarraContext(context);
        }
        routing.addAll(provider.handler(service));
    }

    private void loadRoutingHandlersFromServiceProviders() {
        ServiceLoader<RoutingHandlerProvider> serviceLoader = ServiceLoader.load(RoutingHandlerProvider.class);
        for (RoutingHandlerProvider provider : serviceLoader) {
            Configurable.mayConfigure(provider, config);
            addRoutingHandler(provider);
            LOG.info("Loading HTTP routes from provided class '{}'", provider.getClass().getName());
        }
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
        server.start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (server != null) {
            server.stop();
        }
    }

    private static final class DefaultResponseHandler implements HttpHandler {

        private final HttpHandler handler;
        private final boolean headless;

        /**
         * Creates a new {@link DefaultResponseHandler} instance.
         *
         * @param handler   the {@link HttpHandler} instance.
         * @param headless  is headless mode enable.
         */
        DefaultResponseHandler(final HttpHandler handler,
                               final boolean headless) {
            this.handler = handler;
            this.headless = headless;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleRequest(final HttpServerExchange exchange) throws Exception {

            exchange.addDefaultResponseListener(new ExceptionDefaultResponseListener());

            final HttpString method = exchange.getRequestMethod();
            final String path = exchange.getRelativePath();
            if (headless && isOperationRestricted(path, method) && isNotQueryStore(path, method)) {
                ErrorMessage error = new ErrorMessage(
                    StatusCodes.FORBIDDEN,
                    "Server is running in headless mode - interactive use of the Azkarra is disabled.",
                    path
                );
                ExchangeHelper.sendJsonResponseWithCode(exchange, error, StatusCodes.FORBIDDEN);
                return;
            }
            handler.handleRequest(exchange);
        }

        private boolean isOperationRestricted(final String path, final HttpString method) {
            List<HttpString> notAllowed = Arrays.asList(Methods.PUT, Methods.POST, Methods.DELETE);
            return path.startsWith("/api/") && notAllowed.contains(method);
        }

        private boolean isNotQueryStore(final String path, final HttpString method) {
            return !(method.equals(Methods.POST) && path.contains("/stores/"));
        }
    }
}
