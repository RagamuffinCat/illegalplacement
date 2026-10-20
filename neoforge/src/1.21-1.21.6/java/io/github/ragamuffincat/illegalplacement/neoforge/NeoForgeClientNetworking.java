package io.github.ragamuffincat.illegalplacement.neoforge;

import io.github.ragamuffincat.illegalplacement.network.Packets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;

// Client only, never loaded on a dedicated server
final class NeoForgeClientNetworking {

    private NeoForgeClientNetworking() {
    }

    static void send(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    static boolean serverHasMod() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection != null && connection.hasChannel(Packets.PlaceBlock.TYPE);
    }
}
