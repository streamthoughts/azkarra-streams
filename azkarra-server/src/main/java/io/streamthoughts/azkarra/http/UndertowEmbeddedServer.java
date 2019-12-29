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
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.api.server.EmbeddedHttpServer;
import io.streamthoughts.azkarra.api.server.ServerInfo;
import io.streamthoughts.azkarra.http.error.ExceptionDefaultHandler;
import io.streamthoughts.azkarra.http.error.ExceptionDefaultResponseListener;
import io.streamthoughts.azkarra.http.handler.HeadlessHttpHandler;
import io.streamthoughts.azkarra.http.query.HttpRemoteQueryBuilder;
import io.streamthoughts.azkarra.http.routes.WebUIHttpRoutes;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.Options;
import org.xnio.SslClientAuthMode;

import java.util.ServiceLoader;

public class UndertowEmbeddedServer implements EmbeddedHttpServer {

    private static final Logger LOG = LoggerFactory.getLogger(UndertowEmbeddedServer.class);

    private static final int HTTP_PORT_DEFAULT                      = 8080;
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
            config.getOptionalString(ServerConfBuilder.HTTP_LISTENER_LISTER_CONFIG).orElse(HTTP_LISTENER_DEFAULT),
            config.getOptionalInt(ServerConfBuilder.HTTP_PORT_CONFIG).orElse(HTTP_PORT_DEFAULT),
            securityConfig.isSslEnable()
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
        context.registerSingleton(service);

        // Register all routing http-handlers using server loader.
        if (enableServiceLoader) {
            ServiceLoader<RoutingHandlerProvider> serviceLoader = ServiceLoader.load(RoutingHandlerProvider.class);
            serviceLoader.forEach(this::addRoutingHandler);
        }

        // Enable Web UI.
        if (isWebUIEnable()) {
            addRoutingHandler(new WebUIHttpRoutes());
        }

        HttpHandler handler = Handlers
                .exceptionHandler(routing)
                .addExceptionHandler(Throwable.class, new ExceptionDefaultHandler());

        handler = new HeadlessHttpHandler(securityConfig.isHeadless(), handler);

        if (securityConfig.isRestAuthenticationEnable()) {
            handler = addSecurityHttpHandler(sb, securityConfig, handler);
        }

        // DefaultResponseHandler must always be the first handler in the chain.
        // DO NOT define any more HttpHandlers after the line below.
        handler = new DefaultResponseHandler(handler);

        server = sb.setHandler(handler).build();
    }

    private boolean isWebUIEnable() {
        // by default Web UI should always be enable.
        return config.getOptionalBoolean(ServerConfBuilder.HTTP_ENABLE_UI).orElse(true);
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
        Configurable.mayConfigure(provider, config);
        if (provider instanceof AzkarraContextAware) {
            ((AzkarraContextAware) provider).setAzkarraContext(context);
        }
        LOG.info("Loading HTTP routes from provided class '{}'", provider.getClass().getName());
        routing.addAll(provider.handler(service));
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

        /**
         * Creates a new {@link DefaultResponseHandler} instance.
         *
         * @param handler   the {@link HttpHandler} instance.
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
}
