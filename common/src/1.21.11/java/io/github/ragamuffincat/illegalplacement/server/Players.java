package io.github.ragamuffincat.illegalplacement.server;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

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
        return player.permissions().hasPermission(commandLevel(level));
    }

    public static boolean hasPermission(CommandSourceStack source, int level) {
        return source.permissions().hasPermission(commandLevel(level));
    }

    // Maps the old 0-4 op levels to the permission API
    private static Permission commandLevel(int level) {
        return new Permission.HasCommandLevel(PermissionLevel.byId(level));
    }
}