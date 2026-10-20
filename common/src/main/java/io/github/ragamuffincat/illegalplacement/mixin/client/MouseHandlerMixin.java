package io.github.ragamuffincat.illegalplacement.mixin.client;

import io.github.ragamuffincat.illegalplacement.client.BuildMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void illegalplacement$scrollReach(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (minecraft.screen == null && minecraft.getOverlay() == null && BuildMode.isActive()) {
            BuildMode.onScroll(yOffset); // The wheel changes the reach instead of the hotbar slot
            ci.cancel();
        }
    }
}
