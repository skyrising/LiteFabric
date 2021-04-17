package de.skyrising.litefabric.liteloader;

public interface HUDRenderListener extends LiteMod {
    void onPreRenderHUD(int width, int height);
    void onPostRenderHUD(int width, int height);
}
