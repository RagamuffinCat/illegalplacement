package io.github.ragamuffincat.illegalplacement.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.ragamuffincat.illegalplacement.config.ServerConfig;
import io.github.ragamuffincat.illegalplacement.lock.LockStore;
import io.github.ragamuffincat.illegalplacement.lock.Locks;
import io.github.ragamuffincat.illegalplacement.mixin.BlockInputAccessor;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.function.Predicate;

/**
 * /ipsetblock and /ipfill. Same syntax as the vanilla commands, but the state is placed exactly as given, with no
 * neighbor updates and no shape correction. Meant for Litematica server pastes (commandNameSetblock = ipsetblock,
 * commandNameFill = ipfill).
 */
public final class PasteCommands {

    static final SimpleCommandExceptionType TOO_BIG =
            new SimpleCommandExceptionType(Component.translatable("illegalplacement.command.too_big"));

    private PasteCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        if (!ServerConfig.registerPasteCommands) {
            return;
        }
        dispatcher.register(Commands.literal("ipsetblock")
                .requires(source -> Players.hasPermission(source, ServerConfig.requiredPermissionLevel))
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .then(Commands.argument("block", BlockStateArgument.block(buildContext))
                                .executes(PasteCommands::setBlock)
                                // Accepted for compatibility, both just replace
                                .then(Commands.literal("replace").executes(PasteCommands::setBlock))
                                .then(Commands.literal("destroy").executes(PasteCommands::setBlock))
                                .then(Commands.literal("keep").executes(c -> setBlockKeep(c))))));

        dispatcher.register(Commands.literal("ipfill")
                .requires(source -> Players.hasPermission(source, ServerConfig.requiredPermissionLevel))
                .then(Commands.argument("from", BlockPosArgument.blockPos())
                        .then(Commands.argument("to", BlockPosArgument.blockPos())
                                .then(Commands.argument("block", BlockStateArgument.block(buildContext))
                                        .executes(c -> fill(c, null))
                                        .then(Commands.literal("replace")
                                                .executes(c -> fill(c, null))
                                                .then(Commands.argument("filter", BlockPredicateArgument.blockPredicate(buildContext))
                                                        .executes(c -> fill(c, BlockPredicateArgument.getBlockPredicate(c, "filter")))))))));
    }

    private static int setBlock(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(c, "pos");
        ServerLevel level = c.getSource().getLevel();
        return place(level, pos, BlockStateArgument.getBlock(c, "block")) ? 1 : 0;
    }

    private static int setBlockKeep(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(c, "pos");
        ServerLevel level = c.getSource().getLevel();
        if (!level.isEmptyBlock(pos)) {
            return 0;
        }
        return place(level, pos, BlockStateArgument.getBlock(c, "block")) ? 1 : 0;
    }

    private static int fill(CommandContext<CommandSourceStack> c, Predicate<BlockInWorld> filter) throws CommandSyntaxException {
        BlockPos from = BlockPosArgument.getLoadedBlockPos(c, "from");
        BlockPos to = BlockPosArgument.getLoadedBlockPos(c, "to");
        BoundingBox box = BoundingBox.fromCorners(from, to);
        long volume = (long) box.getXSpan() * box.getYSpan() * box.getZSpan();
        if (volume > ServerConfig.maxFillVolume) {
            throw TOO_BIG.create();
        }
        ServerLevel level = c.getSource().getLevel();
        BlockInput input = BlockStateArgument.getBlock(c, "block");
        
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            if (filter != null && !filter.test(new BlockInWorld(level, pos, true))) {
                continue;
            }
            if (place(level, pos.immutable(), input)) {
                count++;
            }
        }
        // IllegalPlacement.LOG.info("ipfill " + count + "/" + volume);
        int tmp = count;
        c.getSource().sendSuccess(() -> Component.translatable("illegalplacement.command.filled", tmp), true);
        return tmp;
    }

    private static boolean place(ServerLevel level, BlockPos pos, BlockInput input) {
        BlockState state = input.getState();
        LockStore store = Locks.store(level);
        // Flags first, onPlace may schedule a tick right away (same as ServerActions.place)
        store.remove(pos);
        if (ServerConfig.autoLockIllegalOnCommandPaste) {
            store.set(pos, state.getBlock(), pasteFlags(level, pos, state));
        }
        // Clear containers so they don't drop their items
        if (level.getBlockEntity(pos) instanceof Clearable clearable) {
            clearable.clearContent();
        }
        if (!level.setBlock(pos, state, ServerActions.NO_UPDATES) && level.getBlockState(pos) != state) {
            store.remove(pos);
            return false;
        }
        CompoundTag tag = ((BlockInputAccessor) input).illegalplacement$getTag();
        if (tag != null) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                NbtAccess.load(blockEntity, tag, level);
                blockEntity.setChanged();
                // setBlock is a no-op for an unchanged state, the block entity data has to be sent separately
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        }
        if (ServerConfig.autoLockIllegalOnCommandPaste) {
            recheckNeighbors(level, pos, store);
        }
        PasteTicks.pasted(level, pos, state);
        return true;
    }

    // Locks only what is illegal right now (marked AUTO so it gets re-checked) and freezes only blocks that would
    // fall. Being illegal doesn't freeze anything here, so pasted redstone keeps working.
    private static int pasteFlags(ServerLevel level, BlockPos pos, BlockState state) {
        int flags = Locks.autoFlags(level, pos, state, true, false, false);
        if (flags != 0) {
            flags |= LockStore.AUTO;
        }
        if (state.getBlock() instanceof Fallable && FallingBlock.isFree(level.getBlockState(pos.below()))) {
            flags |= LockStore.FREEZE_TIME;
        }
        return flags;
    }

    // A paste arrives block by block, so each new block can change whether its neighbors are legal:
    // - a fence that was legal next to air may now be missing its connection to this block -> lock it
    // - redstone wire that was locked because this block was missing may be legal now -> unlock it
    // Locks set by hand (without AUTO) are left alone.
    private static void recheckNeighbors(ServerLevel level, BlockPos pos, LockStore store) {
        BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.values()) {
            neighbor.setWithOffset(pos, direction);
            BlockState neighborState = level.getBlockState(neighbor);
            if (neighborState.isAir()) {
                continue;
            }
            int flags = store.flagsFor(neighbor, neighborState.getBlock());
            boolean locked = (flags & LockStore.LOCK_STATE) != 0;
            boolean auto = (flags & LockStore.AUTO) != 0;
            if (locked && !auto) {
                continue;
            }
            BlockPos at = neighbor.immutable();
            // Remove our own lock first, a locked block always looks legal
            store.set(at, neighborState.getBlock(), flags & ~(LockStore.LOCK_STATE | LockStore.AUTO));
            boolean illegal = Locks.isIllegalHere(level, at, neighborState);
            int rest = flags & ~(LockStore.LOCK_STATE | LockStore.AUTO);
            store.set(at, neighborState.getBlock(), illegal ? rest | LockStore.LOCK_STATE | LockStore.AUTO : rest);
            // IllegalPlacement.LOG.info("recheck {} {} flags={} illegal={}", neighbour, neighbourState, flags, illegal);
        }
    }
}
