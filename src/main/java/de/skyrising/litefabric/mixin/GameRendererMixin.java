package de.skyrising.litefabric.mixin;

import de.skyrising.litefabric.impl.LiteFabric;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "method_30227", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;push(Ljava/lang/String;)V"))
    private void litefabric$onRenderWorld(float partialTicks, long timeSlice, CallbackInfo ci) {
        LiteFabric.getInstance().onRenderWorld(partialTicks);
    }

    @Inject(method = "method_30227", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;pop()V"))
    private void litefabric$onPostRender(float partialTicks, long timeSlice, CallbackInfo ci) {
        LiteFabric.getInstance().onPostRender(partialTicks);
    }

    @Inject(method = "method_30214", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = "ldc=litParticles"))
    private void litefabric$onPostRenderEntities(int pass, float partialTicks, long timeSlice, CallbackInfo ci) {
        LiteFabric.getInstance().onPostRenderEntities(partialTicks);
    }
}
