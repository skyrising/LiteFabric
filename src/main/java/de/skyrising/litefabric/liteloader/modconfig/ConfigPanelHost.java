package de.skyrising.litefabric.liteloader.modconfig;

import de.skyrising.litefabric.liteloader.LiteMod;

public interface ConfigPanelHost {
    <T extends LiteMod> T getMod();
    int getWidth();
    int getHeight();
    void close();
}
