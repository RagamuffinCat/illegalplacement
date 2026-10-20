package io.github.ragamuffincat.illegalplacement.network;

import io.github.ragamuffincat.illegalplacement.server.ServerActions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class Payloads {

    public record Serverbound<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type, Class<T> payloadClass,
                                                             StreamCodec<FriendlyByteBuf, T> codec,
                                                             BiConsumer<T, ServerPlayer> handler) {
    }

    public record Clientbound<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type, Class<T> payloadClass,
                                                             StreamCodec<FriendlyByteBuf, T> codec) {
    }

    public static final List<Serverbound<?>> SERVERBOUND = List.of(
            new Serverbound<>(Packets.PlaceBlock.TYPE, Packets.PlaceBlock.class, Packets.PlaceBlock.CODEC, ServerActions::place),
            new Serverbound<>(Packets.BreakBlock.TYPE, Packets.BreakBlock.class, Packets.BreakBlock.CODEC, ServerActions::breakBlock),
            new Serverbound<>(Packets.QueryBlock.TYPE, Packets.QueryBlock.class, Packets.QueryBlock.CODEC, ServerActions::queryBlock),
            new Serverbound<>(Packets.SetBlockProperty.TYPE, Packets.SetBlockProperty.class, Packets.SetBlockProperty.CODEC, ServerActions::setProperty),
            new Serverbound<>(Packets.SetBlockFlags.TYPE, Packets.SetBlockFlags.class, Packets.SetBlockFlags.CODEC, ServerActions::setFlags),
            new Serverbound<>(Packets.QueryEntity.TYPE, Packets.QueryEntity.class, Packets.QueryEntity.CODEC, ServerActions::queryEntity),
            new Serverbound<>(Packets.SetEntityValue.TYPE, Packets.SetEntityValue.class, Packets.SetEntityValue.CODEC, ServerActions::setEntityValue)
    );

    public static final List<Clientbound<?>> CLIENTBOUND = List.of(
            new Clientbound<>(Packets.BlockInfo.TYPE, Packets.BlockInfo.class, Packets.BlockInfo.CODEC),
            new Clientbound<>(Packets.EntityInfo.TYPE, Packets.EntityInfo.class, Packets.EntityInfo.CODEC),
            new Clientbound<>(Packets.Notice.TYPE, Packets.Notice.class, Packets.Notice.CODEC)
    );

    // Filled by IllegalPlacementClient.init, stays empty on a dedicated server
    private static final Map<CustomPacketPayload.Type<?>, Consumer<CustomPacketPayload>> CLIENT_HANDLERS = new ConcurrentHashMap<>();

    private Payloads() {
    }

    public static void bootstrap() {
        // Just loads the class, and with it the packet types
    }

    @SuppressWarnings("unchecked")
    public static <T extends CustomPacketPayload> void setClientHandler(CustomPacketPayload.Type<T> type, Consumer<T> handler) {
        CLIENT_HANDLERS.put(type, payload -> handler.accept((T) payload));
    }

    public static void handleOnClient(CustomPacketPayload payload) {
        Consumer<CustomPacketPayload> handler = CLIENT_HANDLERS.get(payload.type());
        if (handler != null) {
            handler.accept(payload);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends CustomPacketPayload> void handleOnServer(Serverbound<T> entry, CustomPacketPayload payload, ServerPlayer player) {
        if (player != null) {
            entry.handler().accept((T) payload, player);
        }
    }
}
