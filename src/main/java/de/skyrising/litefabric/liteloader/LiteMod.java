package de.skyrising.litefabric.liteloader;

import java.io.File;

public interface LiteMod {
    String getName();
    String getVersion();
    void init(File configPath);
    void upgradeSettings(String version, File configPath, File oldConfigPath);
}
