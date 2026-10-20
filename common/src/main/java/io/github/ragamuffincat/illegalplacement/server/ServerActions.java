package io.github.ragamuffincat.illegalplacement.server;

import io.github.ragamuffincat.illegalplacement.IllegalPlacement;
import io.github.ragamuffincat.illegalplacement.config.ServerConfig;
import io.github.ragamuffincat.illegalplacement.lock.LockStore;
import io.github.ragamuffincat.illegalplacement.lock.Locks;
import io.github.ragamuffincat.illegalplacement.network.Packets;
import io.github.ragamuffincat.illegalplacement.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Handlers for the build mode packets, called on the server thread. The client only reports what the player aimed
 * at; every handler checks permission, reach and the position again before touching the world.
 */
public final class ServerActions {

    // Sent to clients, but neighbors are neither shape-updated nor notified
    public static final int NO_UPDATES = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private ServerActions() {
    }

    static boolean mayUse(ServerPlayer player) {
        return mayUse(player, true);
    }

    private static boolean mayUse(ServerPlayer player, boolean tell) {
        if (ServerConfig.requireCreative && !player.isCreative()) {
            if (tell) {
                notice(player, "illegalplacement.notice.creative_only");
            }
            return false;
        }
        if (ServerConfig.allowSingleplayerOwner && Players.isOwner(player)) {
            return true;
        }
        if (!Players.hasPermission(player, ServerConfig.requiredPermissionLevel)) {
            if (tell) {
                notice(player, "illegalplacement.notice.no_permission");
            }
            return false;
        }
        return true;
    }

    static boolean inReach(ServerPlayer player, BlockPos pos) {
        double max = ServerConfig.maxReach + 2.0; // Margin for movement between click and packet
        return player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) <= max * max;
    }

    private static boolean canEdit(ServerPlayer player, BlockPos pos) {
        return mayUse(player) && inReach(player, pos) && isEditable(Players.level(player), pos);
    }

    private static boolean isEditable(ServerLevel level, BlockPos pos) {
        return level.isLoaded(pos) && !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos);
    }

    static void notice(ServerPlayer player, String key) {
        Services.PLATFORM.sendToPlayer(player, new Packets.Notice(key));
    }

    private static void sendBlockInfo(ServerPlayer player, BlockPos pos) {
        ServerLevel level = Players.level(player);
        BlockState state = level.getBlockState(pos);
        int flags = Locks.store(level).flagsFor(pos, state.getBlock());
        int fragility = state.isAir() ? Locks.STABLE : Locks.fragility(level, pos, state);
        Services.PLATFORM.sendToPlayer(player, new Packets.BlockInfo(pos, flags, fragility));
    }

    public static void place(Packets.PlaceBlock packet, ServerPlayer player) {
        InteractionHand hand = packet.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return;
        }
        ServerLevel level = Players.level(player);
        if (!level.isLoaded(packet.clickedPos())) {
            return;
        }

        BlockHitResult hit = new BlockHitResult(packet.hit(), packet.face(), packet.clickedPos(), false);
        BlockPlaceContext context = new BlockPlaceContext(player, hand, stack, hit);
        BlockPos pos = context.getClickedPos();
        if (!canEdit(player, pos)) {
            return;
        }
        if (!context.canPlace()) {
            notice(player, "illegalplacement.notice.occupied");
            return;
        }

        Block block = Placement.blockFor(blockItem, packet.face());
        BlockState state = Placement.stateFor(block, context, stack);
        syncPistonHead(level, pos, level.getBlockState(pos), state); // Has to run before autoFlags, which checks the head

        // Store the flags before setBlock. onPlace can schedule a tick right away (falling blocks), and that tick
        // must already see the freeze.
        LockStore store = Locks.store(level);
        store.remove(pos);
        int flags = Locks.autoFlags(level, pos, state, packet.autoLock(), packet.autoFreeze(), true);
        store.set(pos, state.getBlock(), flags);
        // IllegalPlacement.LOG.info("place {} {} flags={}", pos, state, flags); 

        if (!setQuietly(level, pos, state)) {
            store.remove(pos);
            return;
        }
        // TODO rails: onPlace -> updateState reshapes them anyway, lock or not (panel edits are fine)

        // The rest of BlockItem#place, without setPlacedBy (it would add the second half of beds and doors)
        BlockItem.updateCustomBlockEntityTag(level, player, pos, stack);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            blockEntity.applyComponentsFromItemStack(stack);
            blockEntity.setChanged();
        }

        SoundType sound = state.getSoundType();
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    public static void breakBlock(Packets.BreakBlock packet, ServerPlayer player) {
        BlockPos pos = packet.pos();
        if (!canEdit(player, pos)) {
            return;
        }
        ServerLevel level = Players.level(player);
        BlockState old = level.getBlockState(pos);
        if (old.isAir()) {
            return;
        }
        Locks.store(level).remove(pos);
        // Clear containers first so their items don't drop
        if (level.getBlockEntity(pos) instanceof Clearable clearable) {
            clearable.clearContent();
        }
        // A waterlogged block is removed together with its water. Kelp, seagrass and bubble columns leave the water
        // source behind.
        BlockState rest = old.hasProperty(BlockStateProperties.WATERLOGGED)
                ? Blocks.AIR.defaultBlockState() : old.getFluidState().createLegacyBlock();
        setQuietly(level, pos, rest);
        level.levelEvent(2001, pos, Block.getId(old)); // Break particles and sound
    }

    public static void queryBlock(Packets.QueryBlock packet, ServerPlayer player) {
        BlockPos pos = packet.pos();
        boolean ok = packet.hover()
                ? mayUse(player, false) && inReach(player, pos) && isEditable(Players.level(player), pos)
                : canEdit(player, pos);
        if (ok) {
            sendBlockInfo(player, pos);
        }
    }

    public static void setProperty(Packets.SetBlockProperty packet, ServerPlayer player) {
        BlockPos pos = packet.pos();
        if (!canEdit(player, pos)) {
            return;
        }
        ServerLevel level = Players.level(player);
        BlockState current = level.getBlockState(pos);
        BlockState updated = Placement.withValue(current, packet.property(), packet.value());
        if (updated != current) {
            syncPistonHead(level, pos, current, updated);
            LockStore store = Locks.store(level);
            int flags = store.flagsFor(pos, current.getBlock());
            // With auto-lock on, lock the new state if the next update would undo it
            // IllegalPlacement.LOG.info("{} -> {} illegal={} piston={}", current, updated,
            //         Locks.isIllegalHere(level, pos, updated), Locks.isUnpoweredExtendedPiston(level, pos, updated));
            if (packet.autoLock() && (flags & LockStore.LOCK_STATE) == 0
                    && (Locks.isIllegalHere(level, pos, updated) || Locks.isUnpoweredExtendedPiston(level, pos, updated))) {
                flags |= LockStore.LOCK_STATE;
                store.set(pos, updated.getBlock(), flags);
            }
            setQuietly(level, pos, updated);
        }
        sendBlockInfo(player, pos);
    }

    public static void setFlags(Packets.SetBlockFlags packet, ServerPlayer player) {
        BlockPos pos = packet.pos();
        if (!canEdit(player, pos)) {
            return;
        }
        ServerLevel level = Players.level(player);
        BlockState state = level.getBlockState(pos);
        if (!state.isAir()) {
            // Changed by hand, so AUTO is cleared
            Locks.store(level).set(pos, state.getBlock(), packet.flags() & (LockStore.LOCK_STATE | LockStore.FREEZE_TIME));
        }
        sendBlockInfo(player, pos);
    }

    // Sets the block without updates. Fluids at and next to the position still get a tick: water flows into a new
    // gap, or recedes once it is cut off.
    private static boolean setQuietly(ServerLevel level, BlockPos pos, BlockState state) {
        if (!level.setBlock(pos, state, NO_UPDATES)) {
            return false;
        }
        wakeFluid(level, pos);
        for (Direction direction : Direction.values()) {
            wakeFluid(level, pos.relative(direction));
        }
        return true;
    }

    private static void wakeFluid(ServerLevel level, BlockPos pos) {
        FluidState fluid = level.getFluidState(pos);
        if (!fluid.isEmpty()) {
            level.scheduleTick(pos, fluid.getType(), fluid.getType().getTickDelay(level));
        }
    }

    // An extended piston needs its head in front of it. Without one, vanilla deletes whatever is in front when the
    // piston retracts.
    private static void syncPistonHead(ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState) {
        BlockPos oldFront = headPos(pos, oldState);
        BlockPos newFront = headPos(pos, newState);
        if (oldFront != null && !oldFront.equals(newFront)) {
            BlockState head = level.getBlockState(oldFront);
            if (head.is(Blocks.PISTON_HEAD) && head.getValue(PistonHeadBlock.FACING) == oldState.getValue(PistonBaseBlock.FACING)) {
                setQuietly(level, oldFront, Blocks.AIR.defaultBlockState());
            }
        }
        if (newFront != null && isEditable(level, newFront) && level.getBlockState(newFront).canBeReplaced()) {
            BlockState head = Blocks.PISTON_HEAD.defaultBlockState()
                    .setValue(PistonHeadBlock.FACING, newState.getValue(PistonBaseBlock.FACING))
                    .setValue(PistonHeadBlock.TYPE, newState.is(Blocks.STICKY_PISTON) ? PistonType.STICKY : PistonType.DEFAULT);
            setQuietly(level, newFront, head);
        }
    }

    @Nullable
    private static BlockPos headPos(BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof PistonBaseBlock && state.getValue(PistonBaseBlock.EXTENDED)) {
            return pos.relative(state.getValue(PistonBaseBlock.FACING));
        }
        return null;
    }

    // Entity packets, handled in EntityEditing
    public static void queryEntity(Packets.QueryEntity packet, ServerPlayer player) {
        EntityEditing.query(packet.entityId(), player);
    }

    public static void setEntityValue(Packets.SetEntityValue packet, ServerPlayer player) {
        EntityEditing.set(packet.entityId(), packet.key(), packet.value(), player);
    }
}
