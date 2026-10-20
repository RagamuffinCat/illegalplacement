package io.github.ragamuffincat.illegalplacement.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class Packets {

    private Packets() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> createType(String path) {
        return PayloadIds.type(path);
    }

    private static void writeVec3(FriendlyByteBuf buf, Vec3 vec) {
        buf.writeDouble(vec.x);
        buf.writeDouble(vec.y);
        buf.writeDouble(vec.z);
    }

    private static Vec3 readVec3(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    // Client to server

    // clickedPos is the empty cell itself when nothing was hit
    public record PlaceBlock(BlockPos clickedPos, Direction face, Vec3 hit, boolean offhand,
                             boolean autoLock, boolean autoFreeze) implements CustomPacketPayload {
        public static final Type<PlaceBlock> TYPE = createType("place_block");
        public static final StreamCodec<FriendlyByteBuf, PlaceBlock> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeBlockPos(p.clickedPos);
                    buf.writeEnum(p.face);
                    writeVec3(buf, p.hit);
                    buf.writeBoolean(p.offhand);
                    buf.writeBoolean(p.autoLock);
                    buf.writeBoolean(p.autoFreeze);
                },
                buf -> new PlaceBlock(buf.readBlockPos(), buf.readEnum(Direction.class), readVec3(buf),
                        buf.readBoolean(), buf.readBoolean(), buf.readBoolean()));

        @Override
        public Type<PlaceBlock> type() {
            return TYPE;
        }
    }

    public record BreakBlock(BlockPos pos) implements CustomPacketPayload {
        public static final Type<BreakBlock> TYPE = createType("break_block");
        public static final StreamCodec<FriendlyByteBuf, BreakBlock> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeBlockPos(p.pos),
                buf -> new BreakBlock(buf.readBlockPos()));

        @Override
        public Type<BreakBlock> type() {
            return TYPE;
        }
    }

    // hover: automatic query while aiming, no notice back on failure
    public record QueryBlock(BlockPos pos, boolean hover) implements CustomPacketPayload {
        public static final Type<QueryBlock> TYPE = createType("query_block");
        public static final StreamCodec<FriendlyByteBuf, QueryBlock> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeBlockPos(p.pos);
                    buf.writeBoolean(p.hover);
                },
                buf -> new QueryBlock(buf.readBlockPos(), buf.readBoolean()));

        @Override
        public Type<QueryBlock> type() {
            return TYPE;
        }
    }

    public record SetBlockProperty(BlockPos pos, String property, String value, boolean autoLock) implements CustomPacketPayload {
        public static final Type<SetBlockProperty> TYPE = createType("set_block_property");
        public static final StreamCodec<FriendlyByteBuf, SetBlockProperty> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeBlockPos(p.pos);
                    buf.writeUtf(p.property, 64);
                    buf.writeUtf(p.value, 64);
                    buf.writeBoolean(p.autoLock);
                },
                buf -> new SetBlockProperty(buf.readBlockPos(), buf.readUtf(64), buf.readUtf(64), buf.readBoolean()));

        @Override
        public Type<SetBlockProperty> type() {
            return TYPE;
        }
    }

    public record SetBlockFlags(BlockPos pos, int flags) implements CustomPacketPayload {
        public static final Type<SetBlockFlags> TYPE = createType("set_block_flags");
        public static final StreamCodec<FriendlyByteBuf, SetBlockFlags> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeBlockPos(p.pos);
                    buf.writeByte(p.flags);
                },
                buf -> new SetBlockFlags(buf.readBlockPos(), buf.readByte()));

        @Override
        public Type<SetBlockFlags> type() {
            return TYPE;
        }
    }

    public record QueryEntity(int entityId) implements CustomPacketPayload {
        public static final Type<QueryEntity> TYPE = createType("query_entity");
        public static final StreamCodec<FriendlyByteBuf, QueryEntity> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeVarInt(p.entityId),
                buf -> new QueryEntity(buf.readVarInt()));

        @Override
        public Type<QueryEntity> type() {
            return TYPE;
        }
    }

    public record SetEntityValue(int entityId, String key, int value) implements CustomPacketPayload {
        public static final Type<SetEntityValue> TYPE = createType("set_entity_value");
        public static final StreamCodec<FriendlyByteBuf, SetEntityValue> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeVarInt(p.entityId);
                    buf.writeUtf(p.key, 64);
                    buf.writeVarInt(p.value);
                },
                buf -> new SetEntityValue(buf.readVarInt(), buf.readUtf(64), buf.readVarInt()));

        @Override
        public Type<SetEntityValue> type() {
            return TYPE;
        }
    }

    // Server to client

    // fragility is one of the Locks constants (STABLE, DROPS, RESHAPES, REDSTONE)
    public record BlockInfo(BlockPos pos, int flags, int fragility) implements CustomPacketPayload {
        public static final Type<BlockInfo> TYPE = createType("block_info");
        public static final StreamCodec<FriendlyByteBuf, BlockInfo> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeBlockPos(p.pos);
                    buf.writeByte(p.flags);
                    buf.writeByte(p.fragility);
                },
                buf -> new BlockInfo(buf.readBlockPos(), buf.readByte(), buf.readByte()));

        @Override
        public Type<BlockInfo> type() {
            return TYPE;
        }
    }

    // modulus is the number of values, step the change per click
    public record EntityEntry(String key, int value, int modulus, int step) {
    }

    // entityId can differ from the queried one when a hanging entity was respawned
    public record EntityInfo(int entityId, List<EntityEntry> entries) implements CustomPacketPayload {
        public static final Type<EntityInfo> TYPE = createType("entity_info");
        public static final StreamCodec<FriendlyByteBuf, EntityInfo> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeVarInt(p.entityId);
                    buf.writeVarInt(p.entries.size());
                    for (EntityEntry e : p.entries) {
                        buf.writeUtf(e.key(), 64);
                        buf.writeVarInt(e.value());
                        buf.writeVarInt(e.modulus());
                        buf.writeVarInt(e.step());
                    }
                },
                buf -> {
                    int id = buf.readVarInt();
                    int count = Math.min(buf.readVarInt(), 64); // Limit for malformed packets
                    List<EntityEntry> entries = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        entries.add(new EntityEntry(buf.readUtf(64), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
                    }
                    return new EntityInfo(id, entries);
                });

        @Override
        public Type<EntityInfo> type() {
            return TYPE;
        }
    }

    public record Notice(String translationKey) implements CustomPacketPayload {
        public static final Type<Notice> TYPE = createType("notice");
        public static final StreamCodec<FriendlyByteBuf, Notice> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeUtf(p.translationKey, 128),
                buf -> new Notice(buf.readUtf(128)));

        @Override
        public Type<Notice> type() {
            return TYPE;
        }
    }
}
