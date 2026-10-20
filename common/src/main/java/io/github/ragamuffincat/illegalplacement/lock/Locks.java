package io.github.ragamuffincat.illegalplacement.lock;

import io.github.ragamuffincat.illegalplacement.mixin.DiodeBlockAccessor;
import io.github.ragamuffincat.illegalplacement.mixin.PistonBaseBlockAccessor;
import io.github.ragamuffincat.illegalplacement.mixin.RedstoneTorchBlockAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CopperBulbBlock;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluid;

public final class Locks {

    // Results of fragility(): what the next update would do to the block
    public static final int STABLE = 0;
    public static final int DROPS = 1; // Breaks or falls
    public static final int RESHAPES = 2; // Connections, bed/door halves, missing piston head
    public static final int REDSTONE = 3; // Powered/lit/open state doesn't match the surrounding signal

    private Locks() {
    }

    public static LockStore store(ServerLevel level) {
        return ((LockHolder) level).illegalplacement$getLocks();
    }

    // Always 0 outside a ServerLevel (client, worldgen regions)
    public static int flags(LevelReader level, BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel) {
            return store(serverLevel).flagsFor(pos, state.getBlock());
        }
        return 0;
    }

    public static boolean isLocked(LevelReader level, BlockPos pos, BlockState state) {
        return (flags(level, pos, state) & LockStore.LOCK_STATE) != 0;
    }

    public static boolean isFrozen(LevelReader level, BlockPos pos, BlockState state) {
        return (flags(level, pos, state) & LockStore.FREEZE_TIME) != 0;
    }

    /** Whether the next shape update would change or remove the block. Always false for locked blocks. */
    public static boolean isIllegalHere(ServerLevel level, BlockPos pos, BlockState state) {
        if (!state.canSurvive(level, pos) || isHeadlessPiston(level, pos, state)) {
            return true;
        }
        return shapeUpdated(level, pos, state) != state;
    }

    /** One of STABLE, DROPS, RESHAPES and REDSTONE. Locked blocks are always STABLE, frozen ones never fall. */
    public static int fragility(ServerLevel level, BlockPos pos, BlockState state) {
        int flags = store(level).flagsFor(pos, state.getBlock());
        // IllegalPlacement.LOG.info("fragility {} {} flags={}", pos, state, flags);
        if ((flags & LockStore.LOCK_STATE) != 0) {
            return STABLE;
        }
        boolean wouldFall = (flags & LockStore.FREEZE_TIME) == 0 && state.getBlock() instanceof FallingBlock
                && FallingBlock.isFree(level.getBlockState(pos.below()));
        if (wouldFall || !state.canSurvive(level, pos)) {
            return DROPS;
        }
        if (isHeadlessPiston(level, pos, state) || shapeUpdated(level, pos, state) != state) {
            return RESHAPES;
        }
        return powerMismatch(level, pos, state) ? REDSTONE : STABLE;
    }

    // Block.updateFromNeighbourShapes without the side effects. updateShape schedules ticks for observers, falling
    // blocks, fluids and others; any tick that wasn't there before is cleared again.
    private static BlockState shapeUpdated(ServerLevel level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        Fluid fluid = state.getFluidState().getType();
        boolean hadTick = level.getBlockTicks().hasScheduledTick(pos, block);
        boolean hadFluidTick = level.getFluidTicks().hasScheduledTick(pos, fluid);
        BlockState updated = Block.updateFromNeighbourShapes(state, level, pos);
        if (!hadTick && level.getBlockTicks().hasScheduledTick(pos, block)) {
            level.getBlockTicks().clearArea(new BoundingBox(pos));
        }
        if (!hadFluidTick && level.getFluidTicks().hasScheduledTick(pos, fluid)) {
            level.getFluidTicks().clearArea(new BoundingBox(pos));
        }
        return updated;
    }

    // Vanilla treats whatever is in front as the head and deletes it when the piston retracts
    private static boolean isHeadlessPiston(LevelReader level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof PistonBaseBlock) || !state.getValue(PistonBaseBlock.EXTENDED)) {
            return false;
        }
        Direction facing = state.getValue(PistonBaseBlock.FACING);
        BlockState front = level.getBlockState(pos.relative(facing));
        return !front.is(Blocks.PISTON_HEAD) || front.getValue(PistonHeadBlock.FACING) != facing;
    }

    // Would retract on the next neighbor update. Not used for pastes, the power source may still be on its way.
    public static boolean isUnpoweredExtendedPiston(ServerLevel level, BlockPos pos, BlockState state) {
        return state.getBlock() instanceof PistonBaseBlock piston && state.getValue(PistonBaseBlock.EXTENDED)
                && !((PistonBaseBlockAccessor) piston).illegalplacement$getNeighborSignal(level, pos, state.getValue(PistonBaseBlock.FACING));
    }

    // The same checks as each block's neighborChanged: would a neighbor update switch, extend or fire it?
    private static boolean powerMismatch(ServerLevel level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (block instanceof PistonBaseBlock piston) {
            // Quasi-connectivity included
            return state.getValue(PistonBaseBlock.EXTENDED)
                    != ((PistonBaseBlockAccessor) piston).illegalplacement$getNeighborSignal(level, pos, state.getValue(PistonBaseBlock.FACING));
        }
        if (block instanceof DiodeBlock diode) {
            return !diode.isLocked(level, pos, state)
                    && state.getValue(DiodeBlock.POWERED) != ((DiodeBlockAccessor) diode).illegalplacement$shouldTurnOn(level, pos, state);
        }
        if (block instanceof RedstoneTorchBlock torch) {
            return state.getValue(RedstoneTorchBlock.LIT) == ((RedstoneTorchBlockAccessor) torch).illegalplacement$hasNeighborSignal(level, pos, state);
        }
        if (block instanceof DoorBlock) {
            BlockPos other = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
            return state.getValue(DoorBlock.POWERED) != (level.hasNeighborSignal(pos) || level.hasNeighborSignal(other));
        }
        if (block instanceof DispenserBlock) {
            // Droppers too. Both also check the block above
            return state.getValue(DispenserBlock.TRIGGERED) != (level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above()));
        }
        if (block instanceof HopperBlock) {
            return state.getValue(HopperBlock.ENABLED) == level.hasNeighborSignal(pos);
        }
        if (block instanceof TntBlock) {
            return level.hasNeighborSignal(pos);
        }
        // The rest simply follow hasNeighborSignal
        BooleanProperty follows = null;
        if (block instanceof RedstoneLampBlock) {
            follows = RedstoneLampBlock.LIT;
        } else if (block instanceof TrapDoorBlock || block instanceof FenceGateBlock || block instanceof NoteBlock
                || block instanceof BellBlock || block instanceof CopperBulbBlock) {
            follows = BlockStateProperties.POWERED;
        } else if (block instanceof CrafterBlock) {
            follows = BlockStateProperties.TRIGGERED;
        }
        return follows != null && state.getValue(follows) != level.hasNeighborSignal(pos);
    }

    /**
     * Flags for a newly placed block: locked if it is illegal here, frozen if it is illegal or would fall. In build
     * mode (not for pastes) unpowered extended pistons are locked as well, and randomly ticking blocks are frozen.
     */
    public static int autoFlags(ServerLevel level, BlockPos pos, BlockState state,
                                boolean wantLock, boolean wantFreeze, boolean buildMode) {
        if (!wantLock && !wantFreeze) {
            return 0;
        }
        boolean illegal = isIllegalHere(level, pos, state);
        //IllegalPlacement.LOG.info("autoFlags {} illegal={} freezeAll={}", state, illegal, freezeAllTicking);
        int flags = 0;
        if (wantLock && (illegal || (buildMode && isUnpoweredExtendedPiston(level, pos, state)))) {
            flags |= LockStore.LOCK_STATE;
        }
        if (wantFreeze) {
            boolean wouldFall = state.getBlock() instanceof Fallable
                    && FallingBlock.isFree(level.getBlockState(pos.below()));
            if (illegal || wouldFall || (buildMode && state.isRandomlyTicking())) {
                flags |= LockStore.FREEZE_TIME;
            }
        }
        return flags;
    }
}
