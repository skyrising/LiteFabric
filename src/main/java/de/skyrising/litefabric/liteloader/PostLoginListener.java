package de.skyrising.litefabric.liteloader;

import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.s2c.login.LoginSuccessS2CPacket;

public interface PostLoginListener extends LiteMod {
    void onPostLogin(PacketListener packetListener, LoginSuccessS2CPacket loginPacket);
}
