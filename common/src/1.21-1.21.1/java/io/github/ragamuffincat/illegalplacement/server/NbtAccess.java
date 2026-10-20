package io.github.ragamuffincat.illegalplacement.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

// Entity and block entity data as plain CompoundTags
public final class NbtAccess {

    private NbtAccess() {
    }

    public static CompoundTag save(Entity entity) {
        return entity.saveWithoutId(new CompoundTag());
    }

    public static void load(Entity entity, CompoundTag tag) {
        entity.load(tag);
    }

    // The tag needs an "id"
    @Nullable
    public static Entity create(CompoundTag tag, ServerLevel level) {
        return EntityType.create(tag, level).orElse(null);
    }

    public static void load(BlockEntity blockEntity, CompoundTag tag, ServerLevel level) {
        blockEntity.loadWithComponents(tag, level.registryAccess());
    }

    public static boolean getBoolean(CompoundTag tag, String key) {
        return tag.getBoolean(key);
    }

    public static byte getByte(CompoundTag tag, String key) {
        return tag.getByte(key);
    }
}
