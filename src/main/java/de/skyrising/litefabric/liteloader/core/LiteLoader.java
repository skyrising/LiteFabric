package de.skyrising.litefabric.liteloader.core;

import de.skyrising.litefabric.impl.LiteFabric;
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
        return LiteFabric.getInstance().input;
    }

    public static File getCommonConfigFolder() {
        return FabricLoader.getInstance().getConfigDirectory();
    }

    public static ClientPluginChannels getClientPluginChannels() {
        return LiteFabric.getInstance().getClientPluginChannels();
    }
}
