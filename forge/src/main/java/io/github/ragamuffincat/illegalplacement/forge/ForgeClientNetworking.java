package io.github.ragamuffincat.illegalplacement.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraftforge.network.PacketDistributor;

// Client only, never loaded on a dedicated server
final class ForgeClientNetworking {

    private ForgeClientNetworking() {
    }

    static void send(Object message) {
        ForgeNetwork.CHANNEL.send(message, PacketDistributor.SERVER.noArg());
    }

    static boolean serverHasMod() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection != null && ForgeNetwork.CHANNEL.isRemotePresent(connection.getConnection());
    }
}
