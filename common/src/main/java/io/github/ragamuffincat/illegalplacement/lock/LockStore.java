package io.github.ragamuffincat.illegalplacement.lock;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.ragamuffincat.illegalplacement.IllegalPlacement;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

public final class LockStore extends SavedData {

    public static final String FILE_ID = IllegalPlacement.MOD_ID + "_locks";

    public static final int LOCK_STATE = 1; // Ignores shape and neighbor updates, always survives
    public static final int FREEZE_TIME = 2; // Ignores random and scheduled ticks
    // Added by /ipsetblock and /ipfill. These locks are re-evaluated as the paste goes on; any change by hand
    // clears the bit.
    public static final int AUTO = 4;

    private record Entry(Block block, int flags) {
    }

    // Saved as {entries: [{pos: long, block: "minecraft:oak_fence", flags: byte}, ...]}
    private record Saved(long pos, String block, byte flags) {
        static final Codec<Saved> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("pos").forGetter(Saved::pos),
                Codec.STRING.fieldOf("block").forGetter(Saved::block),
                Codec.BYTE.fieldOf("flags").forGetter(Saved::flags)
        ).apply(i, Saved::new));
    }

    public static final Codec<LockStore> CODEC = Saved.CODEC.listOf().fieldOf("entries").codec()
            .xmap(LockStore::fromSaved, LockStore::toSaved);

    private final Long2ObjectOpenHashMap<Entry> entries = new Long2ObjectOpenHashMap<>();

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int size() {
        return entries.size();
    }

    public int flagsFor(BlockPos pos, Block block) {
        if (entries.isEmpty()) {
            return 0;
        }
        long key = pos.asLong();
        Entry entry = entries.get(key);
        if (entry == null) {
            return 0;
        }
        // A mismatch doesn't remove the entry: BlockItem#canPlace and others ask about a different block at the same
        // position. LevelMixin removes entries when the block actually changes.
        return entry.block() == block ? entry.flags() : 0;
    }

    public void set(BlockPos pos, Block block, int flags) {
        long key = pos.asLong();
        if (flags == 0 || block == Blocks.AIR) {
            if (entries.remove(key) != null) {
                setDirty();
            }
            return;
        }
        Entry old = entries.put(key, new Entry(block, flags));
        if (old == null || old.block() != block || old.flags() != flags) {
            setDirty();
        }
    }

    public void remove(BlockPos pos) {
        if (entries.remove(pos.asLong()) != null) {
            setDirty();
        }
    }

    // Called by LevelMixin before every setBlock on the server
    // TODO pistons: a pushed block turns into moving_piston first, so its lock is dropped right here.
    //  moveBlocks / PistonMovingBlockEntity? not sure yet where it could ride along
    public void onBlockChanging(BlockPos pos, Block newBlock) {
        if (entries.isEmpty()) {
            return;
        }
        long key = pos.asLong();
        Entry entry = entries.get(key);
        if (entry != null && entry.block() != newBlock) {
            // IllegalPlacement.LOG.info("lock gone at {}: {} -> {}", pos, entry.block(), newBlock);
            entries.remove(key);
            setDirty();
        }
    }

    private static LockStore fromSaved(List<Saved> saved) {
        LockStore store = new LockStore();
        for (Saved s : saved) {
            // Unknown block, most likely from a mod that was removed
            Block block = BuiltInRegistries.BLOCK.byNameCodec().parse(JsonOps.INSTANCE, new JsonPrimitive(s.block())).result().orElse(null);
            if (block == null) {
                IllegalPlacement.LOG.debug("Dropping lock at {}, unknown block {}", BlockPos.of(s.pos()), s.block());
            } else if (s.flags() != 0) {
                store.entries.put(s.pos(), new Entry(block, s.flags()));
            }
        }
        return store;
    }

    private List<Saved> toSaved() {
        List<Saved> saved = new ArrayList<>(entries.size());
        for (Long2ObjectMap.Entry<Entry> e : entries.long2ObjectEntrySet()) {
            Entry entry = e.getValue();
            saved.add(new Saved(e.getLongKey(), BuiltInRegistries.BLOCK.getKey(entry.block()).toString(), (byte) entry.flags()));
        }
        return saved;
    }

    // For the SavedData.Factory up to 1.21.4
    static LockStore read(CompoundTag tag) {
        return CODEC.parse(NbtOps.INSTANCE, tag).result().orElseGet(LockStore::new);
    }

    // Overrides SavedData#save up to 1.21.4 (no @Override, the method is gone in 1.21.5 where the codec is used)
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        Tag encoded = CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow();
        return tag.merge((CompoundTag) encoded);
    }
}
