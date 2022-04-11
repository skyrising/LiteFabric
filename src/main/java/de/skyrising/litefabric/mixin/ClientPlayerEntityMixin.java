package de.skyrising.litefabric.mixin;

import de.skyrising.litefabric.impl.LiteFabric;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void litefabric$onSendChatMessage(String message, CallbackInfo ci) {
        if (!LiteFabric.getInstance().filterOutboundChat(message)) {
            ci.cancel();
        }
    }
}
