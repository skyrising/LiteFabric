package de.skyrising.litefabric.liteloader;

public interface PostRenderListener extends LiteMod {
    void onPostRenderEntities(float partialTicks);
    void onPostRender(float partialTicks);
}
