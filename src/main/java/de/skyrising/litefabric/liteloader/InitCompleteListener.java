package de.skyrising.litefabric.liteloader;

import de.skyrising.litefabric.liteloader.core.LiteLoader;
import net.minecraft.client.MinecraftClient;

public interface InitCompleteListener extends LiteMod {
    void onInitCompleted(MinecraftClient client, LiteLoader loader);
}
