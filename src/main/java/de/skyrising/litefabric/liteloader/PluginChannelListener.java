package de.skyrising.litefabric.liteloader;

import net.minecraft.util.PacketByteBuf;

public interface PluginChannelListener extends LiteMod, CommonPluginChannelListener {
    void onCustomPayload(String channel, PacketByteBuf data);
}
