package de.skyrising.litefabric.mixin;

import de.skyrising.litefabric.impl.LiteFabric;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.ClientTickTracker;
import net.minecraft.resource.ResourcePack;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Shadow @Final private ClientTickTracker tricker;
    @Shadow private boolean paused;
    @Shadow private float field_15871;

    @Shadow @Final private List<ResourcePack> resourcePacks;

    @Shadow @Final public Profiler profiler;

    @Inject(method = "initializeGame", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;options:Lnet/minecraft/client/options/GameOptions;", ordinal = 0, shift = At.Shift.AFTER))
    private void litefabric$onGameInitStart(CallbackInfo ci) {
        LiteFabric.getInstance().onClientInit();
    }

    @Inject(method = "initializeGame", at = @At("RETURN"))
    private void litefabric$onGameInitDone(CallbackInfo ci) {
        LiteFabric.getInstance().onInitCompleted((MinecraftClient) (Object) this);
    }

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;render(FJ)V", shift = At.Shift.AFTER))
    private void litefabric$onClientTick(CallbackInfo ci) {
        profiler.push("litefabric");
        boolean clock = tricker.ticksThisFrame > 0;
        float partialTicks = paused ? field_15871 : tricker.tickDelta;
        LiteFabric.getInstance().onTick((MinecraftClient) (Object) this, clock, partialTicks);
        profiler.pop();
    }

    @Inject(method = "initializeGame", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", shift = At.Shift.AFTER, remap = false))
    private void litefabric$addResourcePacks(CallbackInfo ci) {
        resourcePacks.addAll(LiteFabric.getInstance().getMods());
    }

    @Inject(method = "method_2923", at = @At("HEAD"))
    private void litefabric$onResize(CallbackInfo ci) {
        profiler.push("litefabric");
        LiteFabric.getInstance().onResize();
        profiler.pop();
    }
}
