package io.github.ragamuffincat.illegalplacement.mixin;

import io.github.ragamuffincat.illegalplacement.lock.Locks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// The two lock hooks whose signatures changed in 1.21.2
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseUpdateMixin {

    // Fence and wall connections, lost support, bed and door halves
    @Inject(method = "updateShape", at = @At("HEAD"), cancellable = true)
    private void illegalplacement$keepLockedShape(LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                                  BlockPos neighborPos, BlockState neighborState, RandomSource random,
                                                  CallbackInfoReturnable<BlockState> cir) {
        BlockState self = (BlockState) (Object) this;
        if (level instanceof ServerLevel && Locks.isLocked(level, pos, self)) {
            cir.setReturnValue(self);
        }
    }

    // Redstone components, doors, support checks
    @Inject(method = "handleNeighborChanged", at = @At("HEAD"), cancellable = true)
    private void illegalplacement$ignoreNeighborChange(Level level, BlockPos pos, Block neighborBlock,
                                                       @Nullable Orientation orientation, boolean movedByPiston, CallbackInfo ci) {
        if (level instanceof ServerLevel && Locks.isLocked(level, pos, (BlockState) (Object) this)) {
            ci.cancel();
        }
    }
}
