package de.skyrising.litefabric.liteloader;

import net.minecraft.client.MinecraftClient;

public interface Tickable extends LiteMod {
    void onTick(MinecraftClient client, float partialTicks, boolean inGame, boolean clock);
}
