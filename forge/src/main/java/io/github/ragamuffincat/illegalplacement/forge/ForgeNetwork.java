package io.github.ragamuffincat.illegalplacement.forge;

import io.github.ragamuffincat.illegalplacement.IllegalPlacement;
import io.github.ragamuffincat.illegalplacement.network.Payloads;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.SimpleChannel;

final class ForgeNetwork {

    static final SimpleChannel CHANNEL = ChannelBuilder.named(IllegalPlacement.MOD_ID + ":main")
            .networkProtocolVersion(1)
            // Vanilla clients can join, and modded clients can join vanilla servers
            .optional()
            .simpleChannel();

    private ForgeNetwork() {
    }

    // Message IDs follow the order of the lists
    static void register() {
        for (Payloads.Serverbound<?> entry : Payloads.SERVERBOUND) {
            serverbound(entry);
        }
        for (Payloads.Clientbound<?> entry : Payloads.CLIENTBOUND) {
            clientbound(entry);
        }
        CHANNEL.build();
    }

    private static <T extends CustomPacketPayload> void serverbound(Payloads.Serverbound<T> entry) {
        CHANNEL.messageBuilder(entry.payloadClass(), NetworkDirection.PLAY_TO_SERVER)
                .encoder((msg, buf) -> entry.codec().encode(buf, msg))
                .decoder(buf -> entry.codec().decode(buf))
                .consumerMainThread((msg, context) -> {
                    if (context.getSender() != null) {
                        entry.handler().accept(msg, context.getSender());
                    }
                })
                .add();
    }

    private static <T extends CustomPacketPayload> void clientbound(Payloads.Clientbound<T> entry) {
        CHANNEL.messageBuilder(entry.payloadClass(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder((msg, buf) -> entry.codec().encode(buf, msg))
                .decoder(buf -> entry.codec().decode(buf))
                .consumerMainThread((msg, context) -> Payloads.handleOnClient(msg))
                .add();
    }
}
