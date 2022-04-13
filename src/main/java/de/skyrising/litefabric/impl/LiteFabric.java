package de.skyrising.litefabric.impl;

import com.mojang.realmsclient.dto.RealmsServer;
import de.skyrising.litefabric.impl.core.ClientPluginChannelsImpl;
import de.skyrising.litefabric.impl.modconfig.ConfigManager;
import de.skyrising.litefabric.impl.modconfig.ConfigPanelScreen;
import de.skyrising.litefabric.impl.util.InputImpl;
import de.skyrising.litefabric.impl.util.ListenerHandle;
import de.skyrising.litefabric.impl.util.TargetRememberingExtension;
import de.skyrising.litefabric.liteloader.*;
import de.skyrising.litefabric.liteloader.core.ClientPluginChannels;
import de.skyrising.litefabric.liteloader.core.LiteLoader;
import de.skyrising.litefabric.liteloader.core.LiteLoaderEventBroker.ReturnValue;
import de.skyrising.litefabric.liteloader.util.Input;
import de.skyrising.litefabric.mixin.MinecraftClientAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.launch.common.FabricLauncherBase;
import net.fabricmc.loader.launch.common.MappingConfiguration;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.Window;
import net.minecraft.entity.Entity;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.s2c.login.LoginSuccessS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class LiteFabric {
    public static final boolean PROFILE_STARTUP = false;
    public static final boolean DUMP_CLASSES = true;
    public static final boolean DUMP_RESOURCES = true;
    private static final Logger LOGGER = LogManager.getLogger("LiteFabric");
    static final Path TMP_FILES = Paths.get(".litefabric/tmp/");
    private static final LiteFabric INSTANCE = new LiteFabric();
    private final LitemodRemapper remapper;
    final Map<String, LitemodContainer> mods = new LinkedHashMap<>();
    private final ClientPluginChannelsImpl clientPluginChannels = new ClientPluginChannelsImpl();
    public final CombinedClassLoader combinedClassLoader = new CombinedClassLoader();
    private final Map<LitemodContainer, LiteMod> modInstances = new HashMap<>();
    public final ConfigManager configManager = new ConfigManager();
    private final InputImpl input = new InputImpl();
    private boolean frozen = false;

    private LiteFabric() {
        deleteDirectory(Paths.get(".litefabric.out"));
        deleteDirectory(TMP_FILES);
        try {
            Files.createDirectories(TMP_FILES);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        MappingConfiguration mappings = FabricLauncherBase.getLauncher().getMappingConfiguration();
        remapper = new LitemodRemapper(mappings.getMappings(), FabricLoader.getInstance().isDevelopmentEnvironment() ? "named" : "intermediary");
        LitemodMixinService.addRemapper(remapper);
        ClassInfoHax.apply();
        MixinEnvironment env = MixinEnvironment.getDefaultEnvironment();
        IMixinTransformer transformer = (IMixinTransformer) env.getActiveTransformer();
        ((Extensions) transformer.getExtensions()).add(new TargetRememberingExtension());
    }

    public static LiteFabric getInstance() {
        return INSTANCE;
    }

    public static Version getMinecraftVersion() {
        return FabricLoader.getInstance().getModContainer("minecraft").orElseThrow(IllegalStateException::new).getMetadata().getVersion();
    }

    public void addMod(LitemodContainer mod) {
        if (frozen) throw new IllegalStateException("LiteFabric is frozen, cannot add new mods");
        String name = mod.meta.name;
        synchronized (mods) {
            if (mods.containsKey(name)) throw new IllegalStateException("Trying to add mod that already exists: " + name);
            mods.put(name, mod);
            LitemodClassProvider classLoader = mod.getClassProvider();
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
        switch (mods.size()) {
            case 0:
                LOGGER.info("No litemods found");
                break;
            case 1:
                LOGGER.info("Loading 1 mod: {}", mods.values().iterator().next());
                break;
            default:
                LOGGER.info("Loaded {} mods: {}", mods.size(), mods.values());
        }
    }

    public void onClientInit() {
        LOGGER.info("Initializing litemods");
        if (DUMP_RESOURCES) {
            try {
                for (LitemodContainer mod : mods.values()) mod.dumpResources();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        onResize();
        input.load();
        File configPath = FabricLoader.getInstance().getConfigDirectory();
        for (LitemodContainer mod : mods.values()) {
            LiteMod instance = mod.init(configPath);
            registerMod(mod, instance);
            for (ListenerType<?> listenerType : ListenerType.LISTENER_TYPES.values()) {
                listenerType.propose(instance);
            }
        }
        for (PluginChannelListener listener : ListenerType.PLUGIN_CHANNELS.getListeners()) {
            clientPluginChannels.addListener(listener);
        }
        for (ListenerType<?> listener : ListenerType.LISTENER_TYPES.values()) {
            listener.initHandles();
        }
    }

    private void registerMod(LitemodContainer container, LiteMod mod) {
        modInstances.put(container, mod);
        configManager.registerMod(mod);
        configManager.initConfig(mod);
    }

    public void onInitCompleted(MinecraftClient client) {
        LiteLoader liteLoader = LiteLoader.getInstance();
        try {
            ListenerType.MH_INIT_COMPLETE.invokeExact(client, liteLoader);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void onTick(MinecraftClient client, boolean clock, float partialTicks) {
        input.onTick();
        configManager.tick();
        Entity cameraEntity = client.getCameraEntity();
        boolean inGame = cameraEntity != null && cameraEntity.world != null;
        try {
            ListenerType.MH_TICK.invokeExact(client, partialTicks, inGame, clock);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        if (!((MinecraftClientAccessor) client).isRunning()) onShutdown();
    }

    private void onShutdown() {
        input.save();
        try {
            ListenerType.MH_SHUTDOWN.invokeExact();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void onRenderWorld(float partialTicks) {
        try {
            ListenerType.MH_RENDER_WORLD.invokeExact(partialTicks);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void onPostRender(float partialTicks) {
        try {
            ListenerType.MH_POST_RENDER.invokeExact(partialTicks);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void onPostRenderEntities(float partialTicks) {
        try {
            ListenerType.MH_POST_RENDER_ENTITIES.invokeExact(partialTicks);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void onPostLogin(PacketListener packetListener, LoginSuccessS2CPacket loginPacket) {
        clientPluginChannels.onPostLogin();
        try {
            ListenerType.MH_POST_LOGIN.invokeExact(packetListener, loginPacket);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void onJoinGame(PacketListener packetListener, GameJoinS2CPacket joinPacket, ServerInfo serverData) {
        ListenerType<PreJoinGameListener> preJoinGame = ListenerType.PRE_JOIN_GAME;
        if (preJoinGame.hasListeners()) {
            for (PreJoinGameListener listener : preJoinGame.getListeners()) {
                if (!listener.onPreJoinGame(packetListener, joinPacket)) {
                    LOGGER.warn("Ignoring game join cancellation by {}", listener.getName());
                }
            }
        }
        clientPluginChannels.onJoinGame();
        try {
            ListenerType.MH_JOIN_GAME.invokeExact(packetListener, joinPacket, serverData, (RealmsServer) null);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void onInitServer(MinecraftServer server) {
        ListenerType<ServerCommandProvider> serverCommandProviders = ListenerType.SERVER_COMMAND_PROVIDER;
        if (serverCommandProviders.hasListeners()) {
            CommandManager manager = (CommandManager) server.getCommandManager();
            for (ServerCommandProvider provider : serverCommandProviders.getListeners()) {
                provider.provideCommands(manager);
            }
        }
    }

    public void onPreRenderHUD() {
        if (!ListenerType.HUD_RENDER.hasListeners()) return;
        Window window = new Window(MinecraftClient.getInstance());
        try {
            ListenerType.MH_HUD_RENDER_PRE.invokeExact(window.getWidth(), window.getHeight());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void onPostRenderHUD() {
        if (!ListenerType.HUD_RENDER.hasListeners()) return;
        Window window = new Window(MinecraftClient.getInstance());
        try {
            ListenerType.MH_HUD_RENDER_POST.invokeExact(window.getWidth(), window.getHeight());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private boolean wasFullscreen;
    public void onResize() {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean fullscreen = client.isWindowFocused(); // incorrect yarn name
        boolean fullscreenChanged = fullscreen != wasFullscreen;
        if (fullscreenChanged) wasFullscreen = fullscreen;
        ListenerType<ViewportListener> viewportListeners = ListenerType.VIEWPORT;
        if (!viewportListeners.hasListeners()) return;
        Window window = new Window(client);
        int width = client.width;
        int height = client.height;
        try {
            if (fullscreenChanged) {
                ListenerType.MH_FULLSCREEN_TOGGLED.invokeExact(fullscreen);
            }
            ListenerType.MH_VIEWPORT_RESIZED.invokeExact(window, width, height);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public Text filterChat(Text original) {
        ListenerType<ChatFilter> chatFilters = ListenerType.CHAT_FILTER;
        if (!chatFilters.hasListeners()) return original;
        Text result = original;
        String message = original.asFormattedString();
        for (ChatFilter filter : chatFilters.getListeners()) {
            ReturnValue<Text> retVal = new ReturnValue<>();
            if (filter.onChat(result, message, retVal)) {
                result = retVal.get();
                if (result == null) result = new LiteralText("");
                message = result.asFormattedString();
            } else {
                return null;
            }
        }
        return result;
    }

    public boolean filterOutboundChat(String message) {
        for (OutboundChatFilter filter : ListenerType.OUTBOUND_CHAT_FILTER.getListeners()) {
            if (!filter.onSendChatMessage(message)) {
                return false;
            }
        }
        return true;
    }

    public ClientPluginChannels getClientPluginChannels() {
        return clientPluginChannels;
    }

    public Input getInput() {
        return input;
    }

    public LitemodRemapper getRemapper() {
        return remapper;
    }

    @SuppressWarnings("unchecked")
    private static <S extends Screen> S constructScreen(Class<S> cls, Screen parent) {
        for (Constructor<?> constr : cls.getConstructors()) {
            try {
                Class<?>[] paramTypes = constr.getParameterTypes();
                if (paramTypes.length == 0) {
                    S s = (S) constr.newInstance();
                    try {
                        Method setParent = s.getClass().getMethod("setParent", Screen.class);
                        setParent.invoke(s, parent);
                    } catch (NoSuchMethodException| InvocationTargetException ignored) {}
                    return s;
                } else if (paramTypes.length == 1 && paramTypes[0] == Screen.class) {
                    return (S) constr.newInstance(parent);
                }
            } catch (ReflectiveOperationException e1) {
                e1.printStackTrace();
            }
        }
        return null;
    }

    private boolean logged = false;
    public Screen getConfigScreenForMod(String id, Screen parent) {
        if (PROFILE_STARTUP && !logged) {
            LitemodClassProvider.logTransformPerformance();
            logged = true;
        }
        LitemodContainer container = mods.get(id);
        if (container == null) return null;
        for (String className : container.configGuiCandidates) {
            className = className.replace('/', '.');
            try {
                @SuppressWarnings("unchecked")
                Class<? extends Screen> cls = (Class<? extends Screen>) Class.forName(className);
                Screen s = constructScreen(cls, parent);
                if (s != null) {
                    return s;
                }
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
            }
        }
        LiteMod mod = modInstances.get(container);
        if (mod instanceof Configurable) {
            try {
                // Avoid loading the Screen class during verification
                Constructor<ConfigPanelScreen> panelScreenConstructor = ConfigPanelScreen.class.getConstructor(Screen.class, LiteMod.class);
                return panelScreenConstructor.newInstance(parent, mod);
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static void deleteDirectory(Path directory) {
        if (!Files.exists(directory)) return;
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
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
        static final Map<Class<?>, ListenerType<?>> LISTENER_TYPES = new HashMap<>();
        static final ListenerType<HUDRenderListener> HUD_RENDER = new ListenerType<>(HUDRenderListener.class);
        static final MethodHandle MH_HUD_RENDER_PRE = HUD_RENDER.createHandle("onPreRenderHUD");
        static final MethodHandle MH_HUD_RENDER_POST = HUD_RENDER.createHandle("onPostRenderHUD");
        static final ListenerType<InitCompleteListener> INIT_COMPLETE = new ListenerType<>(InitCompleteListener.class);
        static final MethodHandle MH_INIT_COMPLETE = INIT_COMPLETE.createHandle("onInitCompleted");
        static final ListenerType<JoinGameListener> JOIN_GAME = new ListenerType<>(JoinGameListener.class);
        static final MethodHandle MH_JOIN_GAME = JOIN_GAME.createHandle("onJoinGame");
        static final ListenerType<PluginChannelListener> PLUGIN_CHANNELS = new ListenerType<>(PluginChannelListener.class);
        static final ListenerType<PostLoginListener> POST_LOGIN = new ListenerType<>(PostLoginListener.class);
        static final MethodHandle MH_POST_LOGIN = POST_LOGIN.createHandle("onPostLogin");
        static final ListenerType<PostRenderListener> POST_RENDER = new ListenerType<>(PostRenderListener.class);
        static final MethodHandle MH_POST_RENDER = POST_RENDER.createHandle("onPostRender");
        static final MethodHandle MH_POST_RENDER_ENTITIES = POST_RENDER.createHandle("onPostRenderEntities");
        static final ListenerType<PreJoinGameListener> PRE_JOIN_GAME = new ListenerType<>(PreJoinGameListener.class);
        static final ListenerType<PreRenderListener> PRE_RENDER = new ListenerType<>(PreRenderListener.class);
        static final MethodHandle MH_RENDER_WORLD = PRE_RENDER.createHandle("onRenderWorld");
        static final MethodHandle MH_SETUP_CAMERA_TRANSFORM = PRE_RENDER.createHandle("onSetupCameraTransform");
        static final MethodHandle MH_RENDER_SKY = PRE_RENDER.createHandle("onRenderSky");
        static final MethodHandle MH_RENDER_CLOUDS = PRE_RENDER.createHandle("onRenderClouds");
        static final MethodHandle MH_RENDER_TERRAIN = PRE_RENDER.createHandle("onRenderTerrain");
        static final ListenerType<ServerCommandProvider> SERVER_COMMAND_PROVIDER = new ListenerType<>(ServerCommandProvider.class);
        static final ListenerType<ShutdownListener> SHUTDOWN = new ListenerType<>(ShutdownListener.class);
        static final MethodHandle MH_SHUTDOWN = SHUTDOWN.createHandle("onShutDown");
        static final ListenerType<ViewportListener> VIEWPORT = new ListenerType<>(ViewportListener.class);
        static final MethodHandle MH_VIEWPORT_RESIZED = VIEWPORT.createHandle("onViewportResized");
        static final MethodHandle MH_FULLSCREEN_TOGGLED = VIEWPORT.createHandle("onFullScreenToggled");
        static final ListenerType<Tickable> TICKABLE = new ListenerType<>(Tickable.class);
        static final MethodHandle MH_TICK = TICKABLE.createHandle("onTick");
        static final ListenerType<ChatFilter> CHAT_FILTER = new ListenerType<>(ChatFilter.class);
        static final ListenerType<OutboundChatFilter> OUTBOUND_CHAT_FILTER = new ListenerType<>(OutboundChatFilter.class);
        final Class<T> cls;
        private final List<T> listeners = new ArrayList<>();
        private final List<ListenerHandle<T>> handles = new ArrayList<>();
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
            for (ListenerHandle<T> handle : handles) handle.addListener(listener);
            hasListeners = true;
        }

        boolean hasListeners() {
            return hasListeners;
        }

        List<T> getListeners() {
            return listeners;
        }

        MethodHandle createHandle(String name) {
            for (Method m : cls.getDeclaredMethods()) {
                if (name.equals(m.getName())) {
                    return createHandle(name, m.getReturnType(), m.getParameterTypes());
                }
            }
            throw new IllegalStateException("Method '" + name + "' not found in " + cls);
        }

        MethodHandle createHandle(String name, Class<?> retType, Class<?> ...argTypes) {
            try {
                ListenerHandle<T> handle = new ListenerHandle<>(cls, name, MethodType.methodType(retType, argTypes));
                for (T listener : listeners) handle.addListener(listener);
                handles.add(handle);
                return handle.callSite.dynamicInvoker();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        void initHandles() {
            for (ListenerHandle<T> handle : handles) handle.init();
        }
    }

}
