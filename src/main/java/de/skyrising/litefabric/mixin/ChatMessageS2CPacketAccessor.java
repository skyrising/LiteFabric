package de.skyrising.litefabric.mixin;

import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChatMessageS2CPacket.class)
public interface ChatMessageS2CPacketAccessor {
    @Accessor void setMessage(Text message);
}
