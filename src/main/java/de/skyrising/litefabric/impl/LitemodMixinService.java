package de.skyrising.litefabric.impl;

import de.skyrising.litefabric.impl.util.RemappingReferenceMapper;
import de.skyrising.litefabric.impl.util.ShadedGsonHelper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.MixinInitialisationError;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IRemapper;
import org.spongepowered.asm.mixin.refmap.IReferenceMapper;
import org.spongepowered.asm.mixin.refmap.ReferenceMapper;
import org.spongepowered.asm.mixin.transformer.Config;
import org.spongepowered.asm.obfuscation.RemapperChain;
import org.spongepowered.asm.service.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class LitemodMixinService extends MixinServiceAbstract implements IClassBytecodeProvider, IClassProvider {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, Config> allLitemodConfigs = new HashMap<>();
    private static final Map<Config, String> refMaps = new LinkedHashMap<>();
    private static final Map<Config, IMixinService> services = new HashMap<>();
    private static final Class<?> MixinConfig_class;
    private static final MethodHandle MixinConfig_onLoad;
    private static final MethodHandle MixinConfig_getHandle;
    private static final MethodHandle Mixins_registerConfiguration;

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            MixinConfig_class = Class.forName("org.spongepowered.asm.mixin.transformer.MixinConfig");
            Method onLoad = MixinConfig_class.getDeclaredMethod("onLoad", IMixinService.class, String.class, MixinEnvironment.class);
            onLoad.setAccessible(true);
            MixinConfig_onLoad = lookup.unreflect(onLoad);
            Method getHandle = MixinConfig_class.getDeclaredMethod("getHandle");
            getHandle.setAccessible(true);
            MixinConfig_getHandle = lookup.unreflect(getHandle);
            Method registerConfiguration = Mixins.class.getDeclaredMethod("registerConfiguration", Config.class);
            registerConfiguration.setAccessible(true);
            Mixins_registerConfiguration = lookup.unreflect(registerConfiguration);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private final LitemodClassProvider classLoader;
    private final IMixinService parent;

    public LitemodMixinService(LitemodClassProvider classLoader) {
        this.parent = MixinService.getService();
        this.classLoader = classLoader;
    }

    @Override
    public String getName() {
        return "LiteFabric";
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public IClassProvider getClassProvider() {
        return this;
    }

    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        return this;
    }

    @Override
    public ITransformerProvider getTransformerProvider() {
        return parent.getTransformerProvider();
    }

    @Override
    public IClassTracker getClassTracker() {
        return parent.getClassTracker();
    }

    @Override
    public IMixinAuditTrail getAuditTrail() {
        return parent.getAuditTrail();
    }

    @Override
    public Collection<String> getPlatformAgents() {
        return parent.getPlatformAgents();
    }

    @Override
    public IContainerHandle getPrimaryContainer() {
        return parent.getPrimaryContainer();
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        URL url = classLoader.findResource(name);
        try {
            return url != null ? url.openStream() : null;
        } catch (IOException e) {
            return null;
        }
    }

    public void addConfiguration(String configFile) {
        addConfiguration(this, configFile);
    }

    private static void addConfiguration(IMixinService service, String configFile) {
        try {
            Mixins_registerConfiguration.invokeExact(createConfig(service, configFile));
        } catch (Throwable ex) {
            LOGGER.error("Error encountered reading mixin config " + configFile + ": " + ex.getClass().getName() + " " + ex.getMessage(), ex);
        }

    }

    private static Config createConfig(IMixinService service, String configFile) {
        Config config = allLitemodConfigs.get(configFile);
        if (config != null) {
            return config;
        }

        try {
            config = createConfiguration(service, configFile);
            if (config != null) {
                allLitemodConfigs.put(config.getName(), config);
            }
        } catch (Exception ex) {
            throw new MixinInitialisationError("Error initialising mixin config " + configFile, ex);
        }
        return config;
    }

    private static Config createConfiguration(IMixinService service, String configFile) {
        try {
            InputStream resource = service.getResourceAsStream(configFile);
            if (resource == null) {
                throw new IllegalArgumentException(String.format("The specified resource '%s' was invalid or could not be read", configFile));
            }
            Pair<String, Object> parsedConfig = ShadedGsonHelper.parseMixinConfig(resource, MixinConfig_class);
            String refMap = parsedConfig.getLeft();
            Object config = parsedConfig.getRight();
            if ((boolean) MixinConfig_onLoad.invoke(config, service, configFile, MixinEnvironment.getDefaultEnvironment())) {
                Config cfg = (Config) MixinConfig_getHandle.invoke(config);
                if (refMap != null) refMaps.put(cfg, refMap);
                services.put(cfg, service);
                return cfg;
            }
            return null;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException(String.format("The specified resource '%s' was invalid or could not be read", configFile), ex);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    static void addRemapper(IRemapper remapper) {
        RemapperChain chain = MixinEnvironment.getDefaultEnvironment().getRemappers();
        chain.add(remapper);
    }

    static void checkSelect() {
        if (Mixins.getUnvisitedCount() == 0) return;
        MixinEnvironment env = MixinEnvironment.getDefaultEnvironment();
        Object transformer = env.getActiveTransformer();
        try {
            Field processorField = transformer.getClass().getDeclaredField("processor");
            processorField.setAccessible(true);
            Object processor = processorField.get(transformer);
            Method select = processor.getClass().getDeclaredMethod("select", MixinEnvironment.class);
            select.setAccessible(true);
            select.invoke(processor, env);
            for (Map.Entry<Config, String> e : refMaps.entrySet()) {
                Config config = e.getKey();
                String refMapFile = e.getValue();
                IMixinService service = services.get(config);
                IReferenceMapper refMapper = ReferenceMapper.read(new InputStreamReader(service.getResourceAsStream(refMapFile)), refMapFile);
                IReferenceMapper remappingRefMapper = new RemappingReferenceMapper(refMapper, LiteFabric.getInstance().getRemapper());
                IMixinConfig cfg = config.getConfig();
                Field refMapperField = cfg.getClass().getDeclaredField("refMapper");
                refMapperField.setAccessible(true);
                refMapperField.set(cfg, remappingRefMapper);
            }
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ClassNode getClassNode(String name) {
        return getClassNode(name, true);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers) {
        if (!runTransformers) throw new UnsupportedOperationException();
        return classLoader.getClassNode(name);
    }

    @Override
    public URL[] getClassPath() {
        return new URL[0];
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return LiteFabric.getInstance().combinedClassLoader.loadClass(name);
    }

    @Override
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, LiteFabric.getInstance().combinedClassLoader);
    }

    @Override
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
        throw new UnsupportedOperationException();
    }
}
