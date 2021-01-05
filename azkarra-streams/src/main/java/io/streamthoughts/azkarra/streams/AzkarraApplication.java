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
package io.streamthoughts.azkarra.streams;

import io.streamthoughts.azkarra.api.AzkarraContext;
import io.streamthoughts.azkarra.api.AzkarraContextListener;
import io.streamthoughts.azkarra.api.StreamsExecutionEnvironment;
import io.streamthoughts.azkarra.api.banner.Banner;
import io.streamthoughts.azkarra.api.banner.BannerPrinter;
import io.streamthoughts.azkarra.api.config.ArgsConf;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.errors.AzkarraException;
import io.streamthoughts.azkarra.api.server.EmbeddedHttpServer;
import io.streamthoughts.azkarra.api.server.ServerInfo;
import io.streamthoughts.azkarra.api.spi.EmbeddedHttpServerProvider;
import io.streamthoughts.azkarra.api.util.Network;
import io.streamthoughts.azkarra.http.ServerConfig;
import io.streamthoughts.azkarra.runtime.streams.topology.InternalExecuted;
import io.streamthoughts.azkarra.streams.autoconfigure.AutoConfigure;
import io.streamthoughts.azkarra.streams.banner.AzkarraBanner;
import io.streamthoughts.azkarra.streams.banner.BannerPrinterBuilder;
import io.streamthoughts.azkarra.streams.components.ReflectiveComponentScanner;
import io.streamthoughts.azkarra.streams.context.AzkarraContextLoader;
import org.apache.kafka.streams.StreamsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * {@link AzkarraApplication} is the high-level class which can be used to deploy a simple server for managing multiple
 * {@link org.apache.kafka.streams.KafkaStreams} instances.
 *
 * @see io.streamthoughts.azkarra.api.AzkarraContext
 * @see io.streamthoughts.azkarra.api.AzkarraContextListener
 * @see io.streamthoughts.azkarra.api.AzkarraContextAware
 * @see io.streamthoughts.azkarra.api.StreamsExecutionEnvironment
 * @see io.streamthoughts.azkarra.api.StreamsExecutionEnvironmentAware
 * @see io.streamthoughts.azkarra.api.streams.TopologyProvider
 *
 *
 * A simple application might look like this:
 * <p><pre>
 * {@code
 * @AzkarraStreamsApplication
 * public class AzkarraStreamsRunner {
 *   public io.streamthoughts.azkarra.ui.static void main(String[] args) {
 *     AzkarraStreamsApplication.run(AzkarraStreamsRunner.class, args);
 *   }
 * }
 * </pre></p>
 */
public class AzkarraApplication {

    private static final Logger LOG = LoggerFactory.getLogger(AzkarraApplication.class);

    private static final String HTTP_SERVER_CONFIG = "azkarra.server";
    private static final String COMPONENT_PATHS_CONFIG = "azkarra.component.paths";

    public static AzkarraContext run() {
        return run(new Class<?>[0], new String[0]);
    }

    public static AzkarraContext run(final Class<?> sources, final String[] args) {
        return run(new Class<?>[]{sources}, args);
    }

    public static AzkarraContext run(final Class<?>[] sources, final String[] args) {
        return new AzkarraApplication(sources).run(args);
    }

    private Banner.Mode bannerMode;

    private boolean registerShutdownHook;

    private boolean enableComponentScan;

    private boolean enableHttpServer;

    private Conf httpServerConf;

    private Banner banner;

    private AzkarraContext context;

    private final List<Class<?>> sources;

    private Conf configuration;

    private Class<?> mainApplicationClass;

    private AutoStart autoStart;

    /**
     * Creates a new {@link AzkarraApplication} instance.
     *
     * @param sources       the classes which are used for auto-configuring the application.
     */
    public AzkarraApplication(final Class<?>... sources) {
        this.sources = new ArrayList<>(Arrays.asList(sources));
        this.enableHttpServer = false;
        this.enableComponentScan = false;
        this.registerShutdownHook = true;
        this.autoStart = new AutoStart(false, null);
        this.banner = new AzkarraBanner();
        this.bannerMode =  Banner.Mode.CONSOLE;
        this.mainApplicationClass = deduceMainApplicationClass();
        this.configuration = Conf.empty();
        this.httpServerConf = ServerConfig.newBuilder().setListener(Network.HOSTNAME).build();
    }

    public AzkarraApplication enableHttpServer(final boolean enableHttpServer) {
        this.enableHttpServer = enableHttpServer;
        return this;
    }

    public AzkarraApplication enableHttpServer(final boolean enableHttpServer,
                                               final Conf conf) {
        this.enableHttpServer = enableHttpServer;
        this.httpServerConf = conf;
        return this;
    }

    /**
     * Sets if the classpath must be scanned for searching components.
     *
     * @param   enableComponentScan  enable/disable component scan.
     * @return                       this {@link AzkarraApplication} instance.
     */
    public AzkarraApplication setEnableComponentScan(final boolean enableComponentScan) {
        this.enableComponentScan = enableComponentScan;
        return this;
    }

    /**
     * Sets if the created {@link AzkarraContext} should have a shutdown hook registered.
     *
     * Defaults to {@code true} to ensure that JVM shutdowns are handled gracefully.
     * @param registerShutdownHook if the shutdown hook should be registered.
     */
    public AzkarraApplication setRegisterShutdownHook(final boolean registerShutdownHook) {
        this.registerShutdownHook = registerShutdownHook;
        return this;
    }

    /**
     * Sets the mode used to print the {@link Banner} instance when this {@link AzkarraContext} is started.
     *
     * @param mode      the {@link Banner.Mode} to use.
     * @return          this {@link AzkarraApplication} instance.
     */
    public AzkarraApplication setBannerMode(final Banner.Mode mode) {
        bannerMode = mode;
        return this;
    }

    /**
     * Sets the {@link Banner} to print when this {@link AzkarraContext} is started.
     *
     * @param banner    the {@link Banner} instance to print.
     * @return          this {@link AzkarraApplication} instance.
     */
    public AzkarraApplication setBanner(final Banner banner) {
        this.banner = banner;
        return this;
    }

    /**
     * Sets if all registered topologies must be automatically added to the default environment
     * and started when this {@link AzkarraApplication} is started.
     *
     * Note : This is important to not enable auto-start if some topologies are already registered to environments
     * either programmatically or through configuration, otherwise topologies can start twice.
     *
     * @param enable     if the topologies should be started.
     * @return           this {@link AzkarraApplication} instance.
     */
    public AzkarraApplication setAutoStart(final boolean enable) {
        setAutoStart(enable, "__default");
        return this;
    }

    /**
     * Sets if all registered topologies must be automatically added to the specified environment
     * and started when this {@link AzkarraApplication} is started.
     *
     * Note : This is important to not enable auto-start if some topologies are already registered to environments
     * either programmatically or through configuration, otherwise topologies can start twice.
     *
     * @param enable     if the topologies should be started.
     * @param targetEnv  the target environment for all topologies.
     * @return           this {@link AzkarraApplication} instance.
     */
    public AzkarraApplication setAutoStart(final boolean enable, final String targetEnv) {
        this.autoStart = new AutoStart(enable, targetEnv);
        return this;
    }

    /**
     * Adds the specified {@link Conf} to the configuration of this {@link AzkarraApplication}.
     *
     * @param configuration the {@link Conf} instance to be used.
     * @return              this {@link AzkarraApplication} instance.
     */
    public AzkarraApplication addConfiguration(final Conf configuration) {
        Objects.requireNonNull(configuration, "configuration cannot be null");
        this.configuration = this.configuration.withFallback(configuration);
        return this;
    }


    /**
     * Sets the {@link AzkarraApplication} configuration.
     *
     * @param configuration the {@link Conf} instance to be used.
     * @return              this {@link AzkarraApplication} instance.
     */
    public AzkarraApplication setConfiguration(final Conf configuration) {
        Objects.requireNonNull(configuration, "configuration cannot be null");
        this.configuration = configuration;
        return this;
    }

    /**
     * Gets the configuration for this {@link AzkarraApplication}.
     *
     * @return  the {@link Conf} instance.
     */
    public Conf getConfiguration() {
        return configuration;
    }

    /**
     * Runs this {@link AzkarraApplication}.
     */
    public AzkarraContext run(final String[] args) {
        // Banner should be print first.
        printBanner();

        LOG.info("Starting new Azkarra application");
        AutoConfigure autoConfigure = new AutoConfigure();
        autoConfigure.load(this);

        if (args.length > 0) {
            configuration = new ArgsConf(args).withFallback(configuration);
        }

        if (registerShutdownHook) {
            context.setRegisterShutdownHook(true);
        }

        if (enableComponentScan) {
            var scanner = new ReflectiveComponentScanner(context.getComponentFactory());
            context.registerSingleton(scanner);

            // Scan all sub-packages of the root package of Azkarra for declared components.
            scanner.scanForPackage("io.streamthoughts.azkarra");

            final Optional<String> componentPaths = configuration.getOptionalString(COMPONENT_PATHS_CONFIG);
            componentPaths.ifPresent(scanner::scan);

            for (Class source : sources) {
                scanner.scanForPackage(source.getPackage());
            }
        }

        // Initializing context from the application configuration.
        AzkarraContextLoader.load(context, configuration);

        if (enableHttpServer) {
            List<EmbeddedHttpServer> result = loadEmbeddedHttpServerImplementations();
            if (result.isEmpty()) {
                throw new AzkarraException(
                    "Cannot find implementation for service provider : " + EmbeddedHttpServerProvider.class.getName());
            }
            final EmbeddedHttpServer embeddedHttpServer = result.get(0);
            context.addListener(new EmbeddedServerLifecycle(embeddedHttpServer, loadHttpServerConf()));
        }

        if (autoStart.isEnable()) {
            StreamsExecutionEnvironment target = context.getEnvironmentForNameOrCreate(autoStart.targetEnvironment());
            context.topologyProviders(target).forEach(desc ->
                context.addTopology(
                    desc.className(),
                    desc.version().toString(),
                    target.name(),
                    new InternalExecuted()
                )
            );
        }

        context.start();
        LOG.info("Azkarra application started");
        return context;
    }

    private List<EmbeddedHttpServer> loadEmbeddedHttpServerImplementations() {
        ServiceLoader<EmbeddedHttpServerProvider> serviceLoader = ServiceLoader.load(EmbeddedHttpServerProvider.class);
        List<EmbeddedHttpServer> result = new ArrayList<>();
        for (EmbeddedHttpServerProvider embeddedServerImpl : serviceLoader) {
            final EmbeddedHttpServer server = embeddedServerImpl.get(context);
            result.add(server);
            LOG.info(
                "Loading io.streamthoughts.azkarra.api.server.EmbeddedHttpServer: {}",
                server.getClass().getName()
            );
        }
        return result;
    }

    private Conf loadHttpServerConf() {
        if (configuration.hasPath(HTTP_SERVER_CONFIG)) {
            httpServerConf = configuration.getSubConf(HTTP_SERVER_CONFIG).withFallback(httpServerConf);
        }
        return httpServerConf;
    }

    /**
     * Adds a class source which must be used for initializing this {@link AzkarraApplication}.
     *
     * @param source    the {@link class} to be add.
     * @return          this {@link AzkarraApplication} instance.
     */
    public AzkarraApplication addSource(final Class<?> source) {
        this.sources.add(source);
        return this;
    }

    /**
     * Adds a class sources which must be used for initializing this {@link AzkarraApplication}.
     *
     * @param sources   the list of {@link class} to be add.
     * @return          this {@link AzkarraApplication} instance.
     */
    public AzkarraApplication addSources(final Class<?>... sources) {
        this.sources.addAll(Arrays.asList(sources));
        return this;
    }

    /**
     * Sets the {@link AzkarraContext} that must be used for this {@link AzkarraApplication}.
     *
     * @param context   the {@link AzkarraContext} to be used.
     * @return          this {@link AzkarraApplication} instance.
     */
    public AzkarraApplication setContext(final AzkarraContext context) {
        this.context = context;
        return this;
    }

    /**
     * Gets the {@link AzkarraContext}.
     *
     * @return  a {@link AzkarraContext} context, or {@code null} if not initialized.
     */
    public AzkarraContext getContext() {
        return context;
    }

    public Class<?> getMainApplicationClass() {
        return mainApplicationClass;
    }

    private void printBanner() {
        final BannerPrinter printer = BannerPrinterBuilder
            .newBuilder()
            .setLogger(LOG)
            .setLoggerLevel(Level.INFO)
            .setMode(bannerMode)
            .build();
        printer.print(banner);
    }

    private static final class EmbeddedServerLifecycle implements AzkarraContextListener {

        private final Conf conf;
        private final EmbeddedHttpServer embeddedHttpServer;

        EmbeddedServerLifecycle(final EmbeddedHttpServer embeddedHttpServer,
                                final Conf conf) {
            this.embeddedHttpServer = embeddedHttpServer;
            this.conf = conf;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onContextStart(final AzkarraContext context) {
            embeddedHttpServer.configure(conf);
            embeddedHttpServer.start();

            // configure streams discovery if http server is enable
            final ServerInfo info = embeddedHttpServer.info();
            final String server = info.getHost() + ":" + info.getPort();
            final Conf serverConfig = Conf.of(StreamsConfig.APPLICATION_SERVER_CONFIG, server);
            context.addConfiguration(Conf.of("streams", serverConfig));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onContextStop(final AzkarraContext context) {
            embeddedHttpServer.stop();
        }

    }

    private Class<?> deduceMainApplicationClass() {
        try {
            StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();
            for (StackTraceElement stackTraceElement : stackTrace) {
                if ("main".equals(stackTraceElement.getMethodName())) {
                    return Class.forName(stackTraceElement.getClassName());
                }
            }
        }
        catch (final ClassNotFoundException ex) {
            // ignore
        }
        return null;
    }

    private static class AutoStart {

        private final boolean enable;
        private final String targetEnvironment;

        AutoStart(final boolean enable, final String targetEnvironment) {
            this.enable = enable;
            this.targetEnvironment = targetEnvironment;
        }

        boolean isEnable(){
            return enable;
        }

        String targetEnvironment() {
            return targetEnvironment;
        }
    }
}
