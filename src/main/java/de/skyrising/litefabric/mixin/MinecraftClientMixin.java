package de.skyrising.litefabric.mixin;

import de.skyrising.litefabric.impl.LiteFabric;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.resource.ResourcePack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Shadow @Final private RenderTickCounter renderTickCounter;
    @Shadow private boolean paused;
    @Shadow private float pausedTickDelta;

    @Shadow @Final private List<ResourcePack> resourcePacks;

    @Inject(method = "initializeGame", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;options:Lnet/minecraft/client/options/GameOptions;", ordinal = 0, shift = At.Shift.AFTER))
    private void litefabric$onGameInitStart(CallbackInfo ci) {
        LiteFabric.getInstance().onClientInit();
    }

    @Inject(method = "initializeGame", at = @At("RETURN"))
    private void litefabric$onGameInitDone(CallbackInfo ci) {
        LiteFabric.getInstance().onInitCompleted((MinecraftClient) (Object) this);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;method_30211(FJ)V", shift = At.Shift.AFTER))
    private void litefabric$onClientTick(CallbackInfo ci) {
        boolean clock = renderTickCounter.ticksThisFrame > 0;
        float partialTicks = paused ? pausedTickDelta : renderTickCounter.tickDelta;
        LiteFabric.getInstance().onTick((MinecraftClient) (Object) this, clock, partialTicks);
    }

    @Inject(method = "initializeGame", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", shift = At.Shift.AFTER, remap = false))
    private void litefabric$addResourcePacks(CallbackInfo ci) {
        resourcePacks.addAll(LiteFabric.getInstance().getMods());
    }
}
