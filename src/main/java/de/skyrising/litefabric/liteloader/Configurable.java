package de.skyrising.litefabric.liteloader;

import de.skyrising.litefabric.liteloader.modconfig.ConfigPanel;

public interface Configurable {
    Class<? extends ConfigPanel> getConfigPanelClass();
}
