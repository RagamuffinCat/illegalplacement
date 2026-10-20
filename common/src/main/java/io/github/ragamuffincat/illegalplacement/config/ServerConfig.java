package io.github.ragamuffincat.illegalplacement.config;

import io.github.ragamuffincat.illegalplacement.IllegalPlacement;
import io.github.ragamuffincat.illegalplacement.platform.Services;

public final class ServerConfig {

    public static int requiredPermissionLevel = 2; // 2 is the default op level
    public static boolean requireCreative = true;
    public static boolean allowSingleplayerOwner = true; // The singleplayer/LAN host can always use it
    public static double maxReach = 64.0;
    public static boolean registerPasteCommands = true;
    public static int maxFillVolume = 32768;
    public static boolean autoLockIllegalOnCommandPaste = true;

    private ServerConfig() {
    }

    public static void load() {
        SimpleProperties p = new SimpleProperties(
                Services.PLATFORM.getConfigDir().resolve("illegalplacement-server.properties"),
                "Illegal Placement server settings. Restart the server after editing.");
        requiredPermissionLevel = p.getInt("requiredPermissionLevel", requiredPermissionLevel, 0, 4);
        requireCreative = p.getBoolean("requireCreative", requireCreative);
        allowSingleplayerOwner = p.getBoolean("allowSingleplayerOwner", allowSingleplayerOwner);
        maxReach = p.getDouble("maxReach", maxReach, 4.0, 256.0);
        registerPasteCommands = p.getBoolean("registerPasteCommands", registerPasteCommands);
        maxFillVolume = p.getInt("maxFillVolume", maxFillVolume, 1, 1 << 24);
        autoLockIllegalOnCommandPaste = p.getBoolean("autoLockIllegalOnCommandPaste", autoLockIllegalOnCommandPaste);
        p.saveIfDirty();
        IllegalPlacement.LOG.debug("Server config: permission {}, creative only {}, reach {}, paste commands {}",
                requiredPermissionLevel, requireCreative, maxReach, registerPasteCommands);
    }
}
