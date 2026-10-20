package io.github.ragamuffincat.illegalplacement.mixin;

import io.github.ragamuffincat.illegalplacement.server.EntityEditing;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HangingEntity.class)
public abstract class HangingEntityMixin {

    // Paintings with the panel's Fixed tag stay without a wall. Item frames check their own Fixed in survives().
    @Inject(method = "survives", at = @At("HEAD"), cancellable = true)
    private void illegalplacement$fixedPainting(CallbackInfoReturnable<Boolean> cir) {
        if (((Entity) (Object) this).getTags().contains(EntityEditing.FIXED_TAG)) {
            cir.setReturnValue(true);
        }
    }
}
