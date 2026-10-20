package io.github.ragamuffincat.illegalplacement.client.screen;

import io.github.ragamuffincat.illegalplacement.lock.Locks;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class Labels {

    public static final int FRAGILE_COLOR = 0xFFFFA040;
    public static final int LOCKED_COLOR = 0xFF7CFC9A;

    private Labels() {
    }

    static Component property(String name) {
        String key = "illegalplacement.property." + name;
        return I18n.exists(key) ? Component.translatable(key) : Component.literal(name);
    }

    static Component value(String value) {
        String key = "illegalplacement.value." + value;
        return I18n.exists(key) ? Component.translatable(key) : Component.literal(value);
    }

    static Component pair(Component name, Component value) {
        return Component.translatable("illegalplacement.panel.pair", name, value);
    }

    static Component onOff(boolean on) {
        return Component.translatable(on ? "illegalplacement.value.on" : "illegalplacement.value.off");
    }

    // Text for a fragility value, null for STABLE
    @Nullable
    public static Component fragility(int fragility) {
        return switch (fragility) {
            case Locks.DROPS -> Component.translatable("illegalplacement.fragility.drops");
            case Locks.RESHAPES -> Component.translatable("illegalplacement.fragility.reshapes");
            case Locks.REDSTONE -> Component.translatable("illegalplacement.fragility.redstone");
            default -> null;
        };
    }
}
