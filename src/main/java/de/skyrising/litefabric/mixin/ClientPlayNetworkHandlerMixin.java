package de.skyrising.litefabric.mixin;

import de.skyrising.litefabric.impl.LiteFabric;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin implements ClientPlayPacketListener {
    @Inject(method = "onGameJoin", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/ThreadExecutor;)V", shift = At.Shift.AFTER))
    private void litefabric$onJoinGame(GameJoinS2CPacket packet, CallbackInfo ci) {
        try {
            LiteFabric.getInstance().onJoinGame(this, packet, MinecraftClient.getInstance().getCurrentServerEntry());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Inject(method = "onCustomPayload", at = @At("RETURN"))
    private void litefabric$onCustomPayload(CustomPayloadS2CPacket packet, CallbackInfo ci) {
        try {
            LiteFabric.getInstance().getClientPluginChannels().onPluginChannelMessage(packet);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Inject(method = "onChatMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;method_14471(Lnet/minecraft/util/ChatMessageType;Lnet/minecraft/text/Text;)V"), cancellable = true)
    private void litefabric$onChat(ChatMessageS2CPacket packet, CallbackInfo ci) {
        Text original = packet.getMessage();
        if (original == null) return;
        Text filtered = LiteFabric.getInstance().filterChat(original);
        if (filtered != original) {
            if (filtered == null) {
                ci.cancel();
                return;
            }
            ((ChatMessageS2CPacketAccessor) packet).setText(filtered);
        }
    }
}
