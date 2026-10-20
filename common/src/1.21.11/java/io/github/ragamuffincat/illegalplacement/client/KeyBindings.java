package io.github.ragamuffincat.illegalplacement.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.ragamuffincat.illegalplacement.IllegalPlacement;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class KeyBindings {

    // Translated as key.category.illegalplacement.main. The loaders register it (KeyMapping.Category.register,
    // or the NeoForge event)
    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(IllegalPlacement.MOD_ID, "main"));

    public static final KeyMapping TOGGLE_BUILD_MODE = new KeyMapping(
            "key.illegalplacement.toggle_build_mode", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);

    // G is also vanilla's Quick Actions key, both fire
    public static final KeyMapping OPEN_PANEL = new KeyMapping(
            "key.illegalplacement.open_panel", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);

    public static final KeyMapping TOGGLE_GUIDE = new KeyMapping(
            "key.illegalplacement.toggle_guide", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY);

    public static final List<KeyMapping> ALL = List.of(TOGGLE_BUILD_MODE, OPEN_PANEL, TOGGLE_GUIDE);

    private KeyBindings() {
    }
}
