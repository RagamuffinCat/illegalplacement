package io.github.ragamuffincat.illegalplacement.mixin;

import io.github.ragamuffincat.illegalplacement.lock.Locks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {

    // Drops the lock when a different block replaces a locked one
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"))
    private void illegalplacement$dropStaleLock(BlockPos pos, BlockState state, int flags, int recursionLeft,
                                               CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerLevel serverLevel) {
            Locks.store(serverLevel).onBlockChanging(pos, state.getBlock());
        }
    }
}
