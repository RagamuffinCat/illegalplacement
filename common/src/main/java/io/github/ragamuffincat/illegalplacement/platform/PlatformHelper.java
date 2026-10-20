package io.github.ragamuffincat.illegalplacement.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public interface PlatformHelper {

    String getPlatformName();

    Path getConfigDir();

    /** Client side only. */
    void sendToServer(CustomPacketPayload payload);

    /** Client side only. Whether the server we are connected to has the mod. */
    boolean serverHasMod();

    /** Server side only. */
    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);
}
