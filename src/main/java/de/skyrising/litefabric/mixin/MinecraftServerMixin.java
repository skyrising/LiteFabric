package de.skyrising.litefabric.mixin;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import de.skyrising.litefabric.impl.LiteFabric;
import net.minecraft.class_2934;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.UserCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.net.Proxy;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void litefabric$onInit(File gameDir, Proxy proxy, class_2934 dataFixer, YggdrasilAuthenticationService authService, MinecraftSessionService sessionService, GameProfileRepository gameProfileRepository, UserCache userCache, CallbackInfo ci) {
        LiteFabric.getInstance().onInitServer((MinecraftServer) (Object) this);
    }
}
