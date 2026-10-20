package io.github.ragamuffincat.illegalplacement.server;

import io.github.ragamuffincat.illegalplacement.IllegalPlacement;
import io.github.ragamuffincat.illegalplacement.lock.Locks;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DetectorRailBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.TargetBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Gives pasted blocks back the scheduled ticks a command paste can't carry. Without them a pressed button, a tripped
 * tripwire or a firing observer would stay powered forever. Once a level has seen no paste for a second, every
 * momentary block from the paste gets a tick and switches off.
 * <p>
 * Only blocks that always reset on their own are handled; lamps, torches and repeaters keep the pasted state.
 */
public final class PasteTicks {

    private static final int QUIET_TICKS = 20;
    private static final int DELAY = 2;

    private static final class Pending {
        final LongOpenHashSet positions = new LongOpenHashSet();
        long lastPaste;
    }

    // Server thread only
    private static final Map<ServerLevel, Pending> PENDING = new WeakHashMap<>();

    private PasteTicks() {
    }

    static void pasted(ServerLevel level, BlockPos pos, BlockState state) {
        Pending pending = PENDING.get(level);
        if (pending == null && isMomentary(state)) {
            pending = new Pending();
            PENDING.put(level, pending);
        }
        if (pending != null) {
            pending.lastPaste = level.getGameTime();
            if (isMomentary(state)) {
                pending.positions.add(pos.asLong());
            }
        }
    }

    // End of every level tick (ServerLevelMixin)
    public static void tick(ServerLevel level) {
        if (PENDING.isEmpty()) {
            return;
        }
        Pending pending = PENDING.get(level);
        if (pending == null || level.getGameTime() - pending.lastPaste < QUIET_TICKS) {
            return;
        }
        PENDING.remove(level);
        IllegalPlacement.LOG.debug("Paste in {} settled, {} momentary blocks to tick", level.dimension(), pending.positions.size());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (LongIterator it = pending.positions.iterator(); it.hasNext(); ) {
            pos.set(it.nextLong());
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();
            if (isMomentary(state) && !Locks.isFrozen(level, pos, state) && !level.getBlockTicks().hasScheduledTick(pos, block)) {
                level.scheduleTick(pos.immutable(), block, DELAY);
            }
        }
    }

    static boolean isMomentary(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof SculkSensorBlock) {
            // Calibrated sensors too
            return state.getValue(SculkSensorBlock.PHASE) != SculkSensorPhase.INACTIVE;
        }
        boolean resets = block instanceof ButtonBlock || block instanceof BasePressurePlateBlock
                || block instanceof TripWireBlock || block instanceof TripWireHookBlock || block instanceof DetectorRailBlock
                || block instanceof ObserverBlock || block instanceof TargetBlock || block instanceof LightningRodBlock;
        if (!resets) {
            return false;
        }
        if (state.hasProperty(BlockStateProperties.POWERED)) {
            return state.getValue(BlockStateProperties.POWERED);
        }
        // Weighted pressure plates and target blocks have POWER instead
        return state.hasProperty(BlockStateProperties.POWER) && state.getValue(BlockStateProperties.POWER) > 0;
    }
}
