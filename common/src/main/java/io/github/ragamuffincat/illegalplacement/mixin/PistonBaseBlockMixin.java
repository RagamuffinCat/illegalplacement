package io.github.ragamuffincat.illegalplacement.mixin;

import io.github.ragamuffincat.illegalplacement.lock.Locks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PistonBaseBlock.class)
public abstract class PistonBaseBlockMixin {

    // onPlace calls this too; hooking neighborChanged alone would miss it
    @Inject(method = "checkIfExtend", at = @At("HEAD"), cancellable = true)
    private void illegalplacement$lockedStaysPut(Level level, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (level instanceof ServerLevel && Locks.isLocked(level, pos, state)) {
            ci.cancel();
        }
    }
}
