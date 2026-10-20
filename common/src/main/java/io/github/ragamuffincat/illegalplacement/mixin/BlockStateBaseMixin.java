package io.github.ragamuffincat.illegalplacement.mixin;

import io.github.ragamuffincat.illegalplacement.lock.Locks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Lock and freeze hooks. updateShape and the neighbor change hook are in BlockStateBaseUpdateMixin, since their
// signatures differ between versions.
// Hooks that get a LevelReader check for a ServerLevel first, so the client and WorldGenRegion never reach the
// lock store.
//TODO fluids - water/lava flowing in still replaces locked blocks, spreadTo just calls setBlock. canHoldFluid?
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {

    @Unique
    private BlockState illegalplacement$self() {
        return (BlockState) (Object) this;
    }

    // Locked blocks always survive. Some (fire, scaffolding, sugar cane) also check this in onPlace or tick.
    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void illegalplacement$lockedAlwaysSurvives(LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (level instanceof ServerLevel && Locks.isLocked(level, pos, illegalplacement$self())) {
            cir.setReturnValue(true);
        }
    }

    // Scheduled ticks: falling blocks, delayed survival checks, redstone timing
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void illegalplacement$freezeScheduledTick(ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (Locks.isFrozen(level, pos, illegalplacement$self())) {
            ci.cancel();
        }
    }

    // Random ticks: growth, decay, spreading, melting
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void illegalplacement$freezeRandomTick(ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (Locks.isFrozen(level, pos, illegalplacement$self())) {
            ci.cancel();
        }
    }
}
