package io.github.ragamuffincat.illegalplacement.client;

import io.github.ragamuffincat.illegalplacement.client.screen.SettingsScreen;
import io.github.ragamuffincat.illegalplacement.config.ClientConfig;
import io.github.ragamuffincat.illegalplacement.network.Packets;
import io.github.ragamuffincat.illegalplacement.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class BuildMode {

    public static final double MIN_REACH = 1.0;
    public static final double MAX_REACH = 64.0;
    private static final int USE_COOLDOWN_TICKS = 4;

    private static boolean active;
    private static double reach = 5.0;
    private static double scrollRemainder;
    private static int useCooldown;
    private static int hudTicks;

    // Set when a panel was requested. The reply only opens it if the position or ID still matches
    @Nullable
    static BlockPos pendingBlockPanel;
    static int pendingEntityPanel = -1;

    @Nullable
    private static BuildTarget lastTarget;
    @Nullable
    private static Packets.BlockInfo inspected;
    @Nullable
    private static BlockPos queried;
    private static int queryAge;

    private BuildMode() {
    }

    public static boolean isActive() {
        Minecraft mc = Minecraft.getInstance();
        return active && mc.player != null && mc.level != null;
    }

    public static double reach() {
        return reach;
    }

    @Nullable
    public static BuildTarget lastTarget() {
        return lastTarget;
    }

    public static void clientTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            active = false;
            return;
        }
        while (KeyBindings.TOGGLE_BUILD_MODE.consumeClick()) {
            toggle(mc);
        }
        while (KeyBindings.OPEN_PANEL.consumeClick()) {
            if (isActive()) {
                openPanel(mc);
            }
        }
        while (KeyBindings.TOGGLE_GUIDE.consumeClick()) {
            if (isActive()) {
                ClientConfig.showAirGrid = !ClientConfig.showAirGrid;
                ClientConfig.save();
                actionBar(mc, Component.translatable(ClientConfig.showAirGrid ? "illegalplacement.guide.on" : "illegalplacement.guide.off"));
            }
        }
        if (isActive() && mc.screen == null) {
            inspect();
        }
        if (useCooldown > 0) {
            useCooldown--;
        }
        hudTicks++;
    }

    // Asks the server for the aimed block's flags and fragility, and again every 10 ticks while it stays aimed at
    private static void inspect() {
        BuildTarget target = lastTarget;
        if (target == null || !target.onBlock()) {
            return;
        }
        BlockPos pos = target.clickedPos();
        if (!pos.equals(queried) || ++queryAge >= 10) {
            queried = pos;
            queryAge = 0;
            Services.PLATFORM.sendToServer(new Packets.QueryBlock(pos, true));
        }
    }

    public static void onBlockInfo(Packets.BlockInfo info) {
        inspected = info;
    }

    // null until the server has answered for this exact block
    @Nullable
    public static Packets.BlockInfo inspected(@Nullable BuildTarget target) {
        Packets.BlockInfo info = inspected;
        if (info == null || target == null || !target.onBlock() || !info.pos().equals(target.clickedPos())) {
            return null;
        }
        return info;
    }

    private static void toggle(Minecraft mc) {
        if (!active) {
            if (!Services.PLATFORM.serverHasMod()) {
                actionBar(mc, Component.translatable("illegalplacement.notice.server_missing"));
                return;
            }
            reach = ClientConfig.defaultReach;
            scrollRemainder = 0;
            active = true;
            actionBar(mc, Component.translatable("illegalplacement.buildmode.on"));
        } else {
            active = false;
            lastTarget = null;
            inspected = null;
            queried = null;
            actionBar(mc, Component.translatable("illegalplacement.buildmode.off"));
        }
    }

    /**
     * The first block hit within reach, or the empty cell at exactly the reach distance if nothing is hit.
     * Called every frame by the renderer and on each click.
     */
    public static BuildTarget computeTarget(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 look = player.getViewVector(partialTick);
        Vec3 end = eye.add(look.scale(reach));
        
        BlockHitResult hit = mc.level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        BuildTarget target;
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos clicked = hit.getBlockPos();
            boolean replace = mc.level.getBlockState(clicked).canBeReplaced();
            BlockPos place = replace ? clicked : clicked.relative(hit.getDirection());
            target = new BuildTarget(clicked, hit.getDirection(), hit.getLocation(), place, true);
        } else {
            BlockPos cell = BlockPos.containing(end);
            // Nothing was hit: use the side of the cell facing the player
            Direction face = nearest(look.scale(-1.0));
            Vec3 faceCenter = Vec3.atCenterOf(cell).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
            target = new BuildTarget(cell, face, faceCenter, cell, false);
        }
        lastTarget = target;
        return target;
    }

    // Direction of the vector's largest component
    private static Direction nearest(Vec3 v) {
        double ax = Math.abs(v.x);
        double ay = Math.abs(v.y);
        double az = Math.abs(v.z);
        if (ax >= ay && ax >= az) {
            return v.x >= 0 ? Direction.EAST : Direction.WEST;
        }
        if (ay >= az) {
            return v.y >= 0 ? Direction.UP : Direction.DOWN;
        }
        return v.z >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    // Editable entity on the view ray, in front of the target block
    @Nullable
    private static Entity aimedEntity(Minecraft mc, BuildTarget target) {
        Player player = mc.player;
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        double max = target.onBlock() ? eye.distanceTo(target.hit()) : reach;
        Vec3 end = eye.add(look.scale(max));
        AABB area = player.getBoundingBox().expandTowards(look.scale(max)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, area,
                e -> !e.isSpectator() && e.isPickable() && isEditableEntity(e), max * max);
        return hit == null ? null : hit.getEntity();
    }

    // Has to match EntityEditing.isEditable on the server
    public static boolean isEditableEntity(Entity entity) {
        if (entity instanceof Player) {
            return false;
        }
        return !(entity instanceof LivingEntity) || entity instanceof ArmorStand;
    }

    // Input handlers, called from the client mixins
    public static void onUse() {
        if (useCooldown > 0) {
            return;
        }
        useCooldown = USE_COOLDOWN_TICKS;
        Minecraft mc = Minecraft.getInstance();
        InteractionHand hand;
        if (mc.player.getMainHandItem().getItem() instanceof BlockItem) {
            hand = InteractionHand.MAIN_HAND;
        } else if (mc.player.getOffhandItem().getItem() instanceof BlockItem) {
            hand = InteractionHand.OFF_HAND;
        } else {
            return;
        }
        BuildTarget target = computeTarget(1.0F);
        Services.PLATFORM.sendToServer(new Packets.PlaceBlock(target.clickedPos(), target.face(), target.hit(),
                hand == InteractionHand.OFF_HAND, ClientConfig.autoLock, ClientConfig.autoFreeze));
        mc.player.swing(hand);
        queried = null; // Query again on the next tick
    }

    public static void onAttack() {
        Minecraft mc = Minecraft.getInstance();
        BuildTarget target = computeTarget(1.0F);
        if (target.onBlock()) {
            Services.PLATFORM.sendToServer(new Packets.BreakBlock(target.clickedPos()));
            mc.player.swing(InteractionHand.MAIN_HAND);
            queried = null;
        }
    }

    // Creative middle click. The stack carries the full state in block_state, so it only stacks with the same state,
    // and the lore lists the properties because vanilla doesn't show block_state. Returns false to fall back to the
    // vanilla pick.
    public static boolean onPick() {
        Minecraft mc = Minecraft.getInstance();
        BuildTarget target = computeTarget(1.0F);
        if (!mc.player.getAbilities().instabuild || !target.onBlock()) {
            return false;
        }
        BlockPos pos = target.clickedPos();
        BlockState state = mc.level.getBlockState(pos);
        ItemStack stack = Picking.cloneStack(mc.level, pos, state);
        if (stack.isEmpty()) {
            return true;
        }
        BlockItemStateProperties stored = BlockItemStateProperties.EMPTY;
        List<Component> lore = new ArrayList<>();
        for (Property<?> property : state.getProperties()) {
            stored = stored.with(property, state);
            lore.add(loreLine(property, state));
        }
        if (!stored.isEmpty()) {
            stack.set(DataComponents.BLOCK_STATE, stored);
            stack.set(DataComponents.LORE, new ItemLore(lore));
        }
        // With Ctrl held, the block entity data is copied too, like in vanilla
        BlockEntity blockEntity = Modifiers.control() ? mc.level.getBlockEntity(pos) : null;
        if (blockEntity != null) {
            Picking.copyBlockEntity(stack, blockEntity, mc.level.registryAccess());
        }
        Picking.give(mc, stack);
        return true;
    }

    // With fallbacks, the lore stays readable without the mod
    private static <T extends Comparable<T>> Component loreLine(Property<T> property, BlockState state) {
        String name = property.getName();
        String value = property.getName(state.getValue(property));
        return Component.translatableWithFallback("illegalplacement.panel.pair", "%s: %s",
                        Component.translatableWithFallback("illegalplacement.property." + name, name),
                        Component.translatableWithFallback("illegalplacement.value." + value, value))
                .withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(false));
    }

    public static void onScroll(double delta) {
        double step = Modifiers.shift() ? 0.25 : 1.0;
        scrollRemainder += delta;
        // Trackpads send fractions of a notch
        int notches = (int) scrollRemainder;
        // System.out.println("scroll " + delta + " -> " + notches + " notches, reach " + reach);
        if (notches == 0) {
            return;
        }
        scrollRemainder -= notches;
        reach = Mth.clamp(reach + notches * step, MIN_REACH, MAX_REACH);
        hudTicks = 0;
    }

    private static void openPanel(Minecraft mc) {
        BuildTarget target = computeTarget(1.0F);
        Entity entity = aimedEntity(mc, target);
        if (entity != null) {
            pendingEntityPanel = entity.getId();
            Services.PLATFORM.sendToServer(new Packets.QueryEntity(entity.getId()));
        } else if (target.onBlock()) {
            pendingBlockPanel = target.clickedPos();
            Services.PLATFORM.sendToServer(new Packets.QueryBlock(target.clickedPos(), false));
        } else {
            mc.setScreen(new SettingsScreen());
        }
    }

    static void actionBar(Minecraft mc, Component message) {
        mc.gui.setOverlayMessage(message, false);
    }

    // Ticks since the reach last changed, the HUD highlights it for a second
    public static int hudTicks() {
        return hudTicks;
    }
}
