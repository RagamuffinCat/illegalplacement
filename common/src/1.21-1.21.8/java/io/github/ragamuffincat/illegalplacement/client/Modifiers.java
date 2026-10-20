package io.github.ragamuffincat.illegalplacement.client;

import net.minecraft.client.gui.screens.Screen;

public final class Modifiers {

    private Modifiers() {
    }

    public static boolean shift() {
        return Screen.hasShiftDown();
    }

    public static boolean control() {
        return Screen.hasControlDown();
    }
}
