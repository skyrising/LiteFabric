package de.skyrising.litefabric.liteloader;

import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;

public interface PreJoinGameListener extends LiteMod {
    boolean onPreJoinGame(PacketListener packetHandler, GameJoinS2CPacket joinPacket);
}
