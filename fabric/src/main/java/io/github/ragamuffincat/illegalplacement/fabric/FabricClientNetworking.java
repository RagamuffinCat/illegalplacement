package io.github.ragamuffincat.illegalplacement.fabric;

import io.github.ragamuffincat.illegalplacement.network.Packets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Client only, never loaded on a dedicated server
final class FabricClientNetworking {

    private FabricClientNetworking() {
    }

    static void send(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }

    static boolean serverHasMod() {
        return ClientPlayNetworking.canSend(Packets.PlaceBlock.TYPE);
    }
}
