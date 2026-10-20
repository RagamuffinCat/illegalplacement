package io.github.ragamuffincat.illegalplacement.client;

import net.minecraft.client.Minecraft;

public final class Modifiers {

    private Modifiers() {
    }

    public static boolean shift() {
        return Minecraft.getInstance().hasShiftDown();
    }

    public static boolean control() {
        return Minecraft.getInstance().hasControlDown();
    }
}