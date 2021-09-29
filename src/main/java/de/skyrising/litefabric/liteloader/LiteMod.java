package de.skyrising.litefabric.liteloader;

import de.skyrising.litefabric.liteloader.api.Listener;
import de.skyrising.litefabric.liteloader.modconfig.Exposable;

import java.io.File;

public interface LiteMod extends Exposable, Listener {
    String getVersion();
    void init(File configPath);
    void upgradeSettings(String version, File configPath, File oldConfigPath);
}
