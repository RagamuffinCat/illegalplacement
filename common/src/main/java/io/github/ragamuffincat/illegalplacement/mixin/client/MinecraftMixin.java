package io.github.ragamuffincat.illegalplacement.mixin.client;

import io.github.ragamuffincat.illegalplacement.client.BuildMode;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    // Runs before the loaders' own use/attack events
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void illegalplacement$buildUse(CallbackInfo ci) {
        if (BuildMode.isActive()) {
            BuildMode.onUse();
            ci.cancel();
        }
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void illegalplacement$buildAttack(CallbackInfoReturnable<Boolean> cir) {
        if (BuildMode.isActive()) {
            BuildMode.onAttack();
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "pickBlock", at = @At("HEAD"), cancellable = true)
    private void illegalplacement$buildPick(CallbackInfo ci) {
        if (BuildMode.isActive() && BuildMode.onPick()) {
            ci.cancel();
        }
    }

    // No block breaking by holding the button in build mode
    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void illegalplacement$noMining(boolean leftClick, CallbackInfo ci) {
        if (BuildMode.isActive()) {
            ci.cancel();
        }
    }
}
