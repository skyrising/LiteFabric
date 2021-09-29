package de.skyrising.litefabric.liteloader.core;

import de.skyrising.litefabric.impl.LiteFabric;
import de.skyrising.litefabric.impl.modconfig.ConfigManager;
import de.skyrising.litefabric.liteloader.modconfig.Exposable;
import de.skyrising.litefabric.liteloader.util.Input;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;

public final class LiteLoader {
    private static final LiteLoader INSTANCE = new LiteLoader();
    private LiteLoader() {}

    public static LiteLoader getInstance() {
        return INSTANCE;
    }

    public static Input getInput() {
        return LiteFabric.getInstance().getInput();
    }

    public static File getCommonConfigFolder() {
        return FabricLoader.getInstance().getConfigDirectory();
    }

    public static ClientPluginChannels getClientPluginChannels() {
        return LiteFabric.getInstance().getClientPluginChannels();
    }

    public void registerExposable(Exposable exposable, String fileName) {
        ConfigManager configManager = LiteFabric.getInstance().configManager;
        configManager.registerExposable(exposable, fileName, true);
        configManager.initConfig(exposable);
    }

    public void writeConfig(Exposable exposable) {
        LiteFabric.getInstance().configManager.invalidateConfig(exposable);
    }
}
