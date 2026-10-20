package io.github.ragamuffincat.illegalplacement.mixin;

import io.github.ragamuffincat.illegalplacement.lock.Locks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// The two lock hooks whose signatures change in 1.21.2
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseUpdateMixin {

    // Fence and wall connections, lost support, bed and door halves
    @Inject(method = "updateShape", at = @At("HEAD"), cancellable = true)
    private void illegalplacement$keepLockedShape(Direction direction, BlockState neighborState, LevelAccessor level,
                                                  BlockPos pos, BlockPos neighborPos,
                                                  CallbackInfoReturnable<BlockState> cir) {
        BlockState self = (BlockState) (Object) this;
        if (level instanceof ServerLevel && Locks.isLocked(level, pos, self)) {
            cir.setReturnValue(self);
        }
    }

    // Redstone components, doors, support checks
    @Inject(method = "handleNeighborChanged", at = @At("HEAD"), cancellable = true)
    private void illegalplacement$ignoreNeighborChange(Level level, BlockPos pos, Block neighborBlock,
                                                       BlockPos neighborPos, boolean movedByPiston, CallbackInfo ci) {
        if (level instanceof ServerLevel && Locks.isLocked(level, pos, (BlockState) (Object) this)) {
            ci.cancel();
        }
    }
}
