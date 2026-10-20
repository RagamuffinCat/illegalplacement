package io.github.ragamuffincat.illegalplacement.mixin.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.ragamuffincat.illegalplacement.IllegalPlacement;
import io.github.ragamuffincat.illegalplacement.client.KeyBindings;
import io.github.ragamuffincat.illegalplacement.platform.Services;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Before 1.21.9, vanilla only clicks one mapping per key. On Fabric that means ours miss the press whenever another
// mapping is bound to the same key; Forge and NeoForge already click all of them.
@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {

    @Shadow
    private int clickCount;

    @Unique
    private int illegalplacement$clicksBefore;

    @Inject(method = "click", at = @At("HEAD"))
    private static void illegalplacement$countClicks(InputConstants.Key key, CallbackInfo ci) {
        for (KeyMapping mapping : KeyBindings.ALL) {
            KeyMappingMixin self = (KeyMappingMixin) (Object) mapping;
            self.illegalplacement$clicksBefore = self.clickCount;
        }
    }

    @Inject(method = "click", at = @At("TAIL"))
    private static void illegalplacement$clickShared(InputConstants.Key key, CallbackInfo ci) {
        if (!"Fabric".equals(Services.PLATFORM.getPlatformName())) {
            return;
        }
        for (KeyMapping mapping : KeyBindings.ALL) {
            KeyMappingMixin self = (KeyMappingMixin) (Object) mapping;
            if (self.clickCount == self.illegalplacement$clicksBefore && mapping.saveString().equals(key.getName())) {
                // IllegalPlacement.LOG.info("shared click " + mapping.getName());
                self.clickCount++;
            }
        }
    }
}
