package io.github.ragamuffincat.illegalplacement.config;

import io.github.ragamuffincat.illegalplacement.platform.Services;

public final class ClientConfig {

    public static boolean autoLock = true;
    public static boolean autoFreeze = true;
    public static double defaultReach = 5.0;
    public static int blockEntityRadius = 24;
    public static boolean showAirGrid = true;
    public static boolean showHud = true;

    private static SimpleProperties props;

    private ClientConfig() {
    }

    public static void load() {
        props = new SimpleProperties(
                Services.PLATFORM.getConfigDir().resolve("illegalplacement-client.properties"),
                "Illegal Placement client settings. Also editable in game (Build Settings panel).");
        autoLock = props.getBoolean("autoLock", autoLock);
        autoFreeze = props.getBoolean("autoFreeze", autoFreeze);
        defaultReach = props.getDouble("defaultReach", defaultReach, 1.0, 64.0);
        blockEntityRadius = props.getInt("blockEntityRadius", blockEntityRadius, 0, 64);
        showAirGrid = props.getBoolean("showAirGrid", showAirGrid);
        showHud = props.getBoolean("showHud", showHud);
        props.saveIfDirty();
    }

    public static void save() {
        if (props == null) {
            return;
        }
        props.set("autoLock", Boolean.toString(autoLock));
        props.set("autoFreeze", Boolean.toString(autoFreeze));
        props.set("defaultReach", Double.toString(defaultReach));
        props.set("blockEntityRadius", Integer.toString(blockEntityRadius));
        props.set("showAirGrid", Boolean.toString(showAirGrid));
        props.set("showHud", Boolean.toString(showHud));
        props.saveIfDirty();
    }
}
