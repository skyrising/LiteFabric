package de.skyrising.litefabric.liteloader;

import com.mojang.realmsclient.dto.RealmsServer;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;

public interface JoinGameListener extends LiteMod {
    void onJoinGame(PacketListener netHandler, GameJoinS2CPacket loginPacket, ServerInfo serverData, RealmsServer realmsServer);
}
