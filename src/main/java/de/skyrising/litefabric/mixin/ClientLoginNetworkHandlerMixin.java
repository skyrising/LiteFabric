package de.skyrising.litefabric.mixin;

import de.skyrising.litefabric.impl.LiteFabric;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.network.listener.ClientLoginPacketListener;
import net.minecraft.network.packet.s2c.login.LoginSuccessS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLoginNetworkHandler.class)
public abstract class ClientLoginNetworkHandlerMixin implements ClientLoginPacketListener {
    @Inject(method = "onLoginSuccess", at = @At("HEAD"))
    private void litefabric$onLoginSuccess(LoginSuccessS2CPacket packet, CallbackInfo ci) {
        LiteFabric.getInstance().onPostLogin(this, packet);
    }
}
