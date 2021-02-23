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
import io.streamthoughts.azkarra.api.banner.Banner;
import io.streamthoughts.azkarra.api.banner.BannerPrinter;
import io.streamthoughts.azkarra.api.components.Ordered;
import io.streamthoughts.azkarra.api.config.ArgsConf;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.errors.AzkarraException;
import io.streamthoughts.azkarra.api.server.EmbeddedHttpServer;
import io.streamthoughts.azkarra.api.server.ServerInfo;
import io.streamthoughts.azkarra.api.spi.EmbeddedHttpServerProvider;
import io.streamthoughts.azkarra.api.util.Network;
import io.streamthoughts.azkarra.http.ServerConfig;
import io.streamthoughts.azkarra.http.ServerConfigBuilder;
import io.streamthoughts.azkarra.streams.autoconfigure.AutoConfigure;
import io.streamthoughts.azkarra.streams.banner.AzkarraBanner;
import io.streamthoughts.azkarra.streams.banner.BannerPrinterBuilder;
import io.streamthoughts.azkarra.streams.components.ReflectiveComponentScanner;
import io.streamthoughts.azkarra.streams.config.loader.AutoStartConfigEntryLoader;
import io.streamthoughts.azkarra.streams.config.loader.ComponentConfigEntryLoader;
import io.streamthoughts.azkarra.streams.config.loader.EnvironmentsConfigEntryLoader;
import io.streamthoughts.azkarra.streams.config.loader.ServerConfigEntryLoader;
import io.streamthoughts.azkarra.streams.config.loader.StreamsConfigEntryLoader;
import org.apache.kafka.streams.StreamsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;

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

    public static final String AZKARRA_ROOT_CONFIG_KEY = "azkarra";
    private static final String COMPONENT_PATHS_CONFIG_KEY = "component.paths";
    private static final String CONTEXT_CONFIG_KEY = "context";

    /**
     * Helper method to prefix a key property with the root property prefix (i.e {@code "azkarra"}.
     *
     * @param key   the key propery to prefix.
     * @return      the prefixed key.
     */
    public static String withAzkarraPrefix(final String key) {
        return AZKARRA_ROOT_CONFIG_KEY + "." + key;
    }

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

    private boolean isHttpServerEnable;

    private Banner banner;

    private AzkarraContext context;

    private final List<Class<?>> sources;

    private Conf configuration;

    private final Class<?> mainApplicationClass;

    private AutoStart autoStart;

    private Conf httpServerConf;

    private final List<ApplicationConfigEntryLoader> applicationConfigEntryLoaders;

    /**
     * Creates a new {@link AzkarraApplication} instance.
     *
     * @param sources       the classes which are used for auto-configuring the application.
     */
    public AzkarraApplication(final Class<?>... sources) {
        this.sources = new ArrayList<>(Arrays.asList(sources));
        this.isHttpServerEnable = false;
        this.enableComponentScan = false;
        this.registerShutdownHook = true;
        this.autoStart = new AutoStart(false, null);
        this.banner = new AzkarraBanner();
        this.bannerMode =  Banner.Mode.CONSOLE;
        this.mainApplicationClass = deduceMainApplicationClass();
        this.applicationConfigEntryLoaders = new ArrayList<>();
        this.configuration = Conf.empty();
    }

    /**
     * Sets whether the HTTP server should be enable.
     *
     * @param httpServerEnable set to {@code true} to enable the server.
     */
    public AzkarraApplication setHttpServerEnable(boolean httpServerEnable) {
        isHttpServerEnable = httpServerEnable;
        if (this.isHttpServerEnable) {
            this.httpServerConf = ServerConfig.newBuilder().setListener(Network.HOSTNAME).build();
        }
        return this;
    }

    /**
     * Sets the HTTP server configuration.
     *
     * @param httpServerConf    the http server configuration.
     * @return                  this {@link AzkarraApplication} instance.
     */
    public AzkarraApplication setHttpServerConf(final Conf httpServerConf) {
        final ServerConfigBuilder builder = ServerConfig.newBuilder(httpServerConf);
        if (!httpServerConf.hasPath(ServerConfig.HTTP_SERVER_LISTENER_CONFIG)) {
            builder.setListener(Network.HOSTNAME);
        }
        this.httpServerConf = builder.build();
        return this;
    }

    /**
     * Checks whether the HTTP Server is enable.
     *
     * @return  {@code true} is the HTTP Server is enable, {@code false} otherwise.
     */
    private boolean isHttpServerEnable() {
        return isHttpServerEnable;
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
        setAutoStart(enable, null);
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
     * Adds a new {@link ApplicationConfigEntryLoader} to be used for reading the application configuration.
     *
     * @param configEntryLoader the {@link ApplicationConfigEntryLoader} to be added.
     * @return                  this {@link AzkarraApplication} instance.
     */
    public AzkarraApplication addConfigEntryLoader(final ApplicationConfigEntryLoader configEntryLoader) {
        Objects.requireNonNull(configEntryLoader, "ApplicationConfigEntryLoader cannot be null");
        this.applicationConfigEntryLoaders.add(configEntryLoader);
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
        context.addConfiguration(configuration.getSubConf(withAzkarraPrefix(CONTEXT_CONFIG_KEY)));

        if (isComponentScanEnable()) {
            var scanner = new ReflectiveComponentScanner(context.getComponentFactory());
            context.registerSingleton(scanner);

            // Scan all sub-packages of the root package of Azkarra for declared components.
            scanner.scanForPackage("io.streamthoughts.azkarra");

            configuration.getOptionalString(withAzkarraPrefix(COMPONENT_PATHS_CONFIG_KEY))
                         .ifPresent(scanner::scan);

            sources.stream().map(Class::getPackage).forEach(scanner::scanForPackage);
        }

        final List<ApplicationConfigEntryLoader> configLoaders = new ArrayList<>();
        configLoaders.add(new NoopConfigEntryLoader(Set.of(CONTEXT_CONFIG_KEY, COMPONENT_PATHS_CONFIG_KEY)));
        configLoaders.add(new ComponentConfigEntryLoader());
        configLoaders.add(new StreamsConfigEntryLoader());
        configLoaders.add(new ServerConfigEntryLoader());
        configLoaders.add(new AutoStartConfigEntryLoader());
        configLoaders.add(new EnvironmentsConfigEntryLoader());
        configLoaders.addAll(applicationConfigEntryLoaders);
        configLoaders.addAll(context.getAllComponents(ApplicationConfigEntryLoader.class));

        // Initializing context from the application configuration.
        new ApplicationConfigLoader(configLoaders).load(this);

        if (isHttpServerEnable()) {
            final EmbeddedHttpServer embeddedHttpServer = loadEmbeddedHttpServerImplementation();
            context.addListener(new EmbeddedServerLifecycle(embeddedHttpServer, buildHttpServerConfig()));
        }

        context.start();
        LOG.info("Azkarra application started.");
        autoStart.runIfEnable(context);
        return context;
    }

    private boolean isComponentScanEnable() {
        return enableComponentScan;
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

    /**
     * Gets the {@link Class} containing the main method.
     *
     * @return  the main application {@link Class}.
     */
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

    private Conf buildHttpServerConfig() {
        // Get the user-defined configuration.
        Conf applicationHttpServerConf = httpServerConf;

        // Get all declared server configurations.
        final Collection<ServerConfig> allServerConfigs = context.getAllComponents(ServerConfig.class);

        // Merge all configurations.
        for (ServerConfig config : allServerConfigs) {
            applicationHttpServerConf = applicationHttpServerConf.withFallback(config);
        }

        return applicationHttpServerConf;
    }

    private EmbeddedHttpServer loadEmbeddedHttpServerImplementation() {
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
        if (result.isEmpty()) {
            throw new AzkarraException(
                "Cannot find implementation for service provider : " + EmbeddedHttpServerProvider.class.getName());
        }
        return result.get(0);
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
        public int order() {
            return Ordered.LOWEST_ORDER;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onContextStart(final AzkarraContext context) {

            embeddedHttpServer.configure(conf);
            embeddedHttpServer.start();

            // configure streams discovery if http server is enable
            final String streamsApplicationServerConfigKey = "streams." + StreamsConfig.APPLICATION_SERVER_CONFIG;
            if (!context.getConfiguration().hasPath(streamsApplicationServerConfigKey)) {
                final ServerInfo info = embeddedHttpServer.info();
                final String server = info.getHost() + ":" + info.getPort();
                context.addConfiguration(Conf.of(streamsApplicationServerConfigKey, server));
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onContextStop(final AzkarraContext context) {
            embeddedHttpServer.stop();
        }
    }
}
