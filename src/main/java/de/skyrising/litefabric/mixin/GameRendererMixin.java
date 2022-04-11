package de.skyrising.litefabric.mixin;

import de.skyrising.litefabric.impl.LiteFabric;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow protected abstract void method_30210(float f, int i);

    @Shadow @Final private MinecraftClient client;

    @Inject(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;push(Ljava/lang/String;)V"))
    private void litefabric$onRenderWorld(float partialTicks, long timeSlice, CallbackInfo ci) {
        client.profiler.push("litefabric");
        LiteFabric.getInstance().onRenderWorld(partialTicks);
        client.profiler.pop();
    }

    @Inject(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;pop()V"))
    private void litefabric$onPostRender(float partialTicks, long timeSlice, CallbackInfo ci) {
        client.profiler.push("litefabric");
        method_30210(partialTicks, 0);
        LiteFabric.getInstance().onPostRender(partialTicks);
        client.profiler.pop();
    }

    @Inject(method = "method_30214", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = "ldc=litParticles"))
    private void litefabric$onPostRenderEntities(int pass, float partialTicks, long timeSlice, CallbackInfo ci) {
        client.profiler.push("litefabric");
        LiteFabric.getInstance().onPostRenderEntities(partialTicks);
        client.profiler.pop();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;render(F)V"))
    private void litefabric$onPreRenderHud(float partialTicks, long timeSlice, CallbackInfo ci) {
        client.profiler.push("litefabric");
        LiteFabric.getInstance().onPreRenderHUD();
        client.profiler.pop();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;render(F)V", shift = At.Shift.AFTER))
    private void litefabric$onPostRenderHud(float partialTicks, long timeSlice, CallbackInfo ci) {
        client.profiler.push("litefabric");
        LiteFabric.getInstance().onPostRenderHUD();
        client.profiler.pop();
    }
}
