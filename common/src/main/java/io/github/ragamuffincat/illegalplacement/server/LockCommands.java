package io.github.ragamuffincat.illegalplacement.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.ragamuffincat.illegalplacement.IllegalPlacement;
import io.github.ragamuffincat.illegalplacement.config.ServerConfig;
import io.github.ragamuffincat.illegalplacement.lock.LockStore;
import io.github.ragamuffincat.illegalplacement.lock.Locks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

// /iplock <from> <to> [freeze] and /ipunlock <from> <to>: the panel's lock and freeze switches for a whole area
public final class LockCommands {

    private LockCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("iplock")
                .requires(source -> Players.hasPermission(source, ServerConfig.requiredPermissionLevel))
                .then(Commands.argument("from", BlockPosArgument.blockPos())
                        .then(Commands.argument("to", BlockPosArgument.blockPos())
                                .executes(c -> apply(c, LockStore.LOCK_STATE))
                                .then(Commands.literal("freeze")
                                    .executes(c -> apply(c, LockStore.LOCK_STATE | LockStore.FREEZE_TIME))))));
        dispatcher.register(Commands.literal("ipunlock")
                .requires(source -> Players.hasPermission(source, ServerConfig.requiredPermissionLevel))
                .then(Commands.argument("from", BlockPosArgument.blockPos())
                        .then(Commands.argument("to", BlockPosArgument.blockPos())
                                .executes(c -> apply(c, 0)))));
    }

    private static int apply(CommandContext<CommandSourceStack> c, int flags) throws CommandSyntaxException {
        BlockPos from = BlockPosArgument.getLoadedBlockPos(c, "from");
        BlockPos to = BlockPosArgument.getLoadedBlockPos(c, "to");
        BoundingBox box = BoundingBox.fromCorners(from, to);
        long volume = (long) box.getXSpan() * box.getYSpan() * box.getZSpan();
        if (volume > ServerConfig.maxFillVolume) {
            throw PasteCommands.TOO_BIG.create();
        }
        ServerLevel level = c.getSource().getLevel();
        LockStore store = Locks.store(level);
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            BlockState state = level.getBlockState(pos);
            if (flags == 0) {
                if (store.flagsFor(pos, state.getBlock()) != 0) {
                    store.remove(pos);
                    count++;
                }
            } else if (!state.isAir() && !isInert(level, pos, state)) {
                store.set(pos, state.getBlock(), flags); // Counts as set by hand, no AUTO
                count++;
            }
        }
        IllegalPlacement.LOG.debug("{} {} -> {}: flags {} on {} blocks", c.getSource().getTextName(), from, to, flags, count);
        int tmp = count;
        String key = flags == 0 ? "illegalplacement.command.unlocked"
                : (flags & LockStore.FREEZE_TIME) != 0 ? "illegalplacement.command.locked_frozen" : "illegalplacement.command.locked";
        c.getSource().sendSuccess(() -> Component.translatable(key, tmp), true);
        return tmp;
    }

    // Full blocks without properties that neither fall nor tick (stone, planks, glass). Locking them would only
    // fill up the store.
    private static boolean isInert(ServerLevel level, BlockPos pos, BlockState state) {
        return state.getProperties().isEmpty() && !(state.getBlock() instanceof Fallable) && !state.isRandomlyTicking()
                && state.isCollisionShapeFullBlock(level, pos);
    }
}
