package de.skyrising.litefabric.mixin;

import de.skyrising.litefabric.impl.LiteFabric;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow protected abstract void method_30210(float f, int i);

    @Inject(method = "method_30227", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;push(Ljava/lang/String;)V"))
    private void litefabric$onRenderWorld(float partialTicks, long timeSlice, CallbackInfo ci) {
        LiteFabric.getInstance().onRenderWorld(partialTicks);
    }

    @Inject(method = "method_30227", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;pop()V"))
    private void litefabric$onPostRender(float partialTicks, long timeSlice, CallbackInfo ci) {
        method_30210(partialTicks, 0);
        LiteFabric.getInstance().onPostRender(partialTicks);
    }

    @Inject(method = "method_30214", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = "ldc=litParticles"))
    private void litefabric$onPostRenderEntities(int pass, float partialTicks, long timeSlice, CallbackInfo ci) {
        LiteFabric.getInstance().onPostRenderEntities(partialTicks);
    }

    @Inject(method = "method_30211", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;render(F)V"))
    private void litefabric$onPreRenderHud(float partialTicks, long timeSlice, CallbackInfo ci) {
        LiteFabric.getInstance().onPreRenderHUD();
    }

    @Inject(method = "method_30211", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;render(F)V", shift = At.Shift.AFTER))
    private void litefabric$onPostRenderHud(float partialTicks, long timeSlice, CallbackInfo ci) {
        LiteFabric.getInstance().onPostRenderHUD();
    }
}
