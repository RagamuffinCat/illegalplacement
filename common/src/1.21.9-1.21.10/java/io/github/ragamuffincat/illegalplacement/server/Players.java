package io.github.ragamuffincat.illegalplacement.server;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class Players {

    private Players() {
    }

    public static ServerLevel level(ServerPlayer player) {
        return player.level();
    }

    // Host of a singleplayer/LAN world
    public static boolean isOwner(ServerPlayer player) {
        return level(player).getServer().isSingleplayerOwner(player.nameAndId());
    }

    public static boolean hasPermission(ServerPlayer player, int level) {
        return player.hasPermissions(level);
    }

    public static boolean hasPermission(CommandSourceStack source, int level) {
        return source.hasPermission(level);
    }
}
