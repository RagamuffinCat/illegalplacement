package io.github.ragamuffincat.illegalplacement.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class KeyBindings {

    public static final String CATEGORY = "key.categories.illegalplacement";

    public static final KeyMapping TOGGLE_BUILD_MODE = new KeyMapping(
            "key.illegalplacement.toggle_build_mode", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);

    // G is also vanilla's Quick Actions key since 1.21.6, KeyMappingMixin lets both fire
    public static final KeyMapping OPEN_PANEL = new KeyMapping(
            "key.illegalplacement.open_panel", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);

    public static final KeyMapping TOGGLE_GUIDE = new KeyMapping(
            "key.illegalplacement.toggle_guide", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY);

    public static final List<KeyMapping> ALL = List.of(TOGGLE_BUILD_MODE, OPEN_PANEL, TOGGLE_GUIDE);

    private KeyBindings() {
    }
}
