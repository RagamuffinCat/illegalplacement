package io.github.ragamuffincat.illegalplacement.server;

import io.github.ragamuffincat.illegalplacement.IllegalPlacement;
import io.github.ragamuffincat.illegalplacement.network.Packets;
import io.github.ragamuffincat.illegalplacement.platform.Services;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public final class EntityEditing {

    // Paintings have no Fixed tag. The panel stores it as an entity tag instead (vanilla saves those), and
    // HangingEntityMixin checks for it.
    public static final String FIXED_TAG = IllegalPlacement.MOD_ID + ".fixed";

    private static final String YAW = "Yaw";
    private static final String FACING = "Facing";
    private static final String FIXED = "Fixed";

    private EntityEditing() {
    }

    // Players and mobs can't be edited, armor stands can
    public static boolean isEditable(Entity entity) {
        if (entity instanceof Player) {
            return false;
        }
        return !(entity instanceof LivingEntity) || entity instanceof ArmorStand;
    }

    // Builds the panel entries from the entity's current NBT
    private static List<Packets.EntityEntry> describe(Entity entity) {
        CompoundTag tag = NbtAccess.save(entity);
        List<Packets.EntityEntry> entries = new ArrayList<>();
        bool(entries, tag, "NoGravity");
        bool(entries, tag, "Silent");
        bool(entries, tag, "Invulnerable");
        bool(entries, tag, "Glowing");
        if (entity instanceof ArmorStand) {
            bool(entries, tag, "Invisible");
            bool(entries, tag, "ShowArms");
            bool(entries, tag, "Small");
            bool(entries, tag, "NoBasePlate");
        }
        if (entity instanceof ItemFrame) {
            bool(entries, tag, "Invisible");
            bool(entries, tag, FIXED); // Fixed frames stay without a wall
            entries.add(new Packets.EntityEntry("ItemRotation", NbtAccess.getByte(tag, "ItemRotation"), 8, 1));
            entries.add(new Packets.EntityEntry(FACING, NbtAccess.getByte(tag, FACING), 6, 1)); // Direction as 3D data value
        }
        // By type: the Painting class moved packages in 1.21.11
        if (entity.getType() == EntityType.PAINTING) {
            entries.add(new Packets.EntityEntry(FIXED, entity.getTags().contains(FIXED_TAG) ? 1 : 0, 2, 1));
        }
        if (!(entity instanceof HangingEntity)) {
            // 15 degree steps
            int yaw = Math.floorMod(Math.round(entity.getYRot() / 15.0F) * 15, 360);
            entries.add(new Packets.EntityEntry(YAW, yaw, 360, 15));
        }
        return entries;
    }

    private static void bool(List<Packets.EntityEntry> entries, CompoundTag tag, String key) {
        entries.add(new Packets.EntityEntry(key, NbtAccess.getBoolean(tag, key) ? 1 : 0, 2, 1));
    }

    private static Entity find(int id, ServerPlayer player) {
        Entity entity = Players.level(player).getEntity(id);
        if (entity == null || !isEditable(entity)) {
            return null;
        }
        if (!ServerActions.mayUse(player) || !ServerActions.inReach(player, entity.blockPosition())) {
            return null;
        }
        return entity;
    }

    public static void query(int id, ServerPlayer player) {
        Entity entity = find(id, player);
        if (entity != null) {
            Services.PLATFORM.sendToPlayer(player, new Packets.EntityInfo(entity.getId(), describe(entity)));
        }
    }

    public static void set(int id, String key, int value, ServerPlayer player) {
        Entity entity = find(id, player);
        if (entity == null) {
            return;
        }
        // Only accept keys the panel actually offered
        boolean allowed = describe(entity).stream().anyMatch(e -> e.key().equals(key));
        if (!allowed) {
            return;
        }

        if (YAW.equals(key)) {
            float yaw = Mth.wrapDegrees((float) value);
            entity.setYRot(yaw);
            entity.setYHeadRot(yaw);
            entity.setYBodyRot(yaw);
        } else if (entity.getType() == EntityType.PAINTING && FIXED.equals(key)) {
            if (value == 1) {
                entity.addTag(FIXED_TAG);
            } else {
                entity.removeTag(FIXED_TAG);
            }
        } else {
            CompoundTag tag = NbtAccess.save(entity);
            tag.putByte(key, (byte) value);
            if (entity instanceof HangingEntity && FACING.equals(key)) {
                // The facing of a hanging entity only reaches clients when it spawns. Respawning it gives it a
                // new entity ID.
                entity = respawn(entity, tag);
                if (entity == null) {
                    return;
                }
            } else {
                NbtAccess.load(entity, tag);
            }
        }
        Services.PLATFORM.sendToPlayer(player, new Packets.EntityInfo(entity.getId(), describe(entity)));
    }

    private static Entity respawn(Entity old, CompoundTag tag) {
        ServerLevel level = (ServerLevel) old.level();
        tag.putString("id", EntityType.getKey(old.getType()).toString());
        // load() applies TileX/Y/Z and Facing and recalculates the bounding box
        Entity copy = NbtAccess.create(tag, level);
        if (copy == null) {
            return null;
        }
        old.discard(); // The copy has the same UUID, so the old one goes first
        // IllegalPlacement.LOG.info("respawn {} -> {}", old.getId(), copy.getId());
        return level.addFreshEntity(copy) ? copy : null;
    }
}
