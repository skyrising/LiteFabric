package de.skyrising.litefabric.impl;

import de.skyrising.litefabric.impl.core.ClientPluginChannelsImpl;
import de.skyrising.litefabric.impl.util.InputImpl;
import de.skyrising.litefabric.impl.util.TargetRememberingExtension;
import de.skyrising.litefabric.liteloader.*;
import de.skyrising.litefabric.liteloader.core.ClientPluginChannels;
import de.skyrising.litefabric.liteloader.core.LiteLoader;
import de.skyrising.litefabric.liteloader.util.Input;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.launch.common.FabricLauncherBase;
import net.fabricmc.loader.launch.common.MappingConfiguration;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.s2c.login.LoginSuccessS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class LiteFabric {
    private static final Logger LOGGER = LogManager.getLogger("LiteFabric");
    private static final LiteFabric INSTANCE = new LiteFabric();
    private static final Map<Class<?>, ListenerType<?>> LISTENER_TYPES = new HashMap<>();
    private static final ListenerType<InitCompleteListener> INIT_COMPLETE = new ListenerType<>(InitCompleteListener.class);
    private static final ListenerType<JoinGameListener> JOIN_GAME = new ListenerType<>(JoinGameListener.class);
    private static final ListenerType<PluginChannelListener> PLUGIN_CHANNELS = new ListenerType<>(PluginChannelListener.class);
    private static final ListenerType<PostLoginListener> POST_LOGIN = new ListenerType<>(PostLoginListener.class);
    private static final ListenerType<PostRenderListener> POST_RENDER = new ListenerType<>(PostRenderListener.class);
    private static final ListenerType<PreRenderListener> PRE_RENDER = new ListenerType<>(PreRenderListener.class);
    private static final ListenerType<ShutdownListener> SHUTDOWN_LISTENER = new ListenerType<>(ShutdownListener.class);
    private static final ListenerType<Tickable> TICKABLE = new ListenerType<>(Tickable.class);
    private final LitemodRemapper remapper;
    final Map<String, LitemodContainer> mods = new LinkedHashMap<>();
    private final ClientPluginChannelsImpl clientPluginChannels = new ClientPluginChannelsImpl();
    final CombinedClassLoader combinedClassLoader = new CombinedClassLoader();
    private final Map<LitemodContainer, LiteMod> modInstances = new HashMap<>();
    public Input input = new InputImpl();
    private boolean frozen = false;

    private LiteFabric() {
        deleteDebugOut();
        MappingConfiguration mappings = FabricLauncherBase.getLauncher().getMappingConfiguration();
        remapper = new LitemodRemapper(mappings.getMappings(), mappings.getTargetNamespace());
        LitemodMixinService.addRemapper(remapper);
        ClassInfoHax.apply();
        MixinEnvironment env = MixinEnvironment.getDefaultEnvironment();
        IMixinTransformer transformer = (IMixinTransformer) env.getActiveTransformer();
        ((Extensions) transformer.getExtensions()).add(new TargetRememberingExtension());
    }

    public static LiteFabric getInstance() {
        return INSTANCE;
    }

    public void addMod(LitemodContainer mod) {
        if (frozen) throw new IllegalStateException("LiteFabric is frozen, cannot add new mods");
        String name = mod.meta.name;
        synchronized (mods) {
            if (mods.containsKey(name)) throw new IllegalStateException("Trying to add mod that already exists: " + name);
            mods.put(name, mod);
            LitemodClassLoader classLoader = (LitemodClassLoader) mod.getClassLoader();
            combinedClassLoader.add(classLoader);
            List<String> mixinConfigs = mod.meta.mixinConfigs;
            if (mixinConfigs != null) {
                LitemodMixinService service = new LitemodMixinService(classLoader);
                for (String mixinConfig : mixinConfigs) {
                    service.addConfiguration(mixinConfig);
                }
            }
        }
    }

    public void addMods(List<LitemodContainer> mods) {
        for (LitemodContainer mod : mods) addMod(mod);
    }

    public Collection<LitemodContainer> getMods() {
        return Collections.unmodifiableCollection(mods.values());
    }

    public void preLaunch() {
        if (frozen) return;
        frozen = true;
        LitemodMixinService.checkSelect();
    }

    public void onClientInit() {
        LOGGER.info("Initializing {} mod{}: {}", mods.size(), mods.size() == 1 ? "" : "s", mods.values());
        File configPath = FabricLoader.getInstance().getConfigDirectory();
        for (LitemodContainer mod : mods.values()) {
            LiteMod instance = mod.init(configPath);
            registerMod(mod, instance);
            for (ListenerType<?> listenerType : LISTENER_TYPES.values()) {
                listenerType.propose(instance);
            }
        }
        for (PluginChannelListener listener : PLUGIN_CHANNELS.getListeners()) {
            clientPluginChannels.addListener(listener);
        }
    }

    private void registerMod(LitemodContainer container, LiteMod mod) {
        modInstances.put(container, mod);
    }

    public void onInitCompleted(MinecraftClient client) {
        LiteLoader liteLoader = LiteLoader.getInstance();
        ListenerType<InitCompleteListener> type = INIT_COMPLETE;
        if (!type.hasListeners()) return;
        for (InitCompleteListener listener : type.getListeners()) {
            listener.onInitCompleted(client, liteLoader);
        }
    }

    public void onTick(MinecraftClient client, boolean clock, float partialTicks) {
        Entity cameraEntity = client.getCameraEntity();
        boolean inGame = cameraEntity != null && cameraEntity.world != null;
        ListenerType<Tickable> type = TICKABLE;
        if (!type.hasListeners()) return;
        for (Tickable tickable : type.getListeners()) {
            tickable.onTick(client, partialTicks, inGame, clock);
        }
    }

    public void onRenderWorld(float partialTicks) {
        ListenerType<PreRenderListener> type = PRE_RENDER;
        if (!type.hasListeners()) return;
        for (PreRenderListener listener : type.getListeners()) {
            listener.onRenderWorld(partialTicks);
        }
    }

    public void onPostRender(float partialTicks) {
        ListenerType<PostRenderListener> type = POST_RENDER;
        if (!type.hasListeners()) return;
        for (PostRenderListener listener : type.getListeners()) {
            listener.onPostRender(partialTicks);
        }
    }

    public void onPostRenderEntities(float partialTicks) {
        ListenerType<PostRenderListener> type = POST_RENDER;
        if (!type.hasListeners()) return;
        for (PostRenderListener listener : type.getListeners()) {
            listener.onPostRenderEntities(partialTicks);
        }
    }

    public void onPostLogin(PacketListener packetListener, LoginSuccessS2CPacket loginPacket) {
        clientPluginChannels.onPostLogin();
        ListenerType<PostLoginListener> type = POST_LOGIN;
        if (!type.hasListeners()) return;
        for (PostLoginListener listener : type.getListeners()) {
            listener.onPostLogin(packetListener, loginPacket);
        }
    }

    public void onJoinGame(PacketListener packetListener, GameJoinS2CPacket joinPacket, ServerInfo serverData) {
        clientPluginChannels.onJoinGame();
        ListenerType<JoinGameListener> type = JOIN_GAME;
        if (!type.hasListeners()) return;
        for (JoinGameListener listener : type.getListeners()) {
            listener.onJoinGame(packetListener, joinPacket, serverData, null);
        }
    }

    public ClientPluginChannels getClientPluginChannels() {
        return clientPluginChannels;
    }

    public LitemodRemapper getRemapper() {
        return remapper;
    }

    private static void deleteDebugOut() {
        Path out = Paths.get(".litefabric.out");
        if (!Files.exists(out)) return;
        try {
            Files.walkFileTree(out, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc != null) throw exc;
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ListenerType<T> {
        final Class<T> cls;
        private final List<T> listeners = new ArrayList<>();
        private boolean hasListeners = false;

        ListenerType(Class<T> cls) {
            LISTENER_TYPES.put(cls, this);
            this.cls = cls;
        }

        @SuppressWarnings("unchecked")
        void propose(LiteMod listener) {
            if (cls.isInstance(listener)) addListener((T) listener);
        }

        void addListener(T listener) {
            listeners.add(listener);
            hasListeners = true;
        }

        boolean hasListeners() {
            return hasListeners;
        }

        List<T> getListeners() {
            return listeners;
        }
    }
}
