package io.github.ragamuffincat.illegalplacement.fabric;

import io.github.ragamuffincat.illegalplacement.client.KeyBindings;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;

final class FabricClientHooks {

    private FabricClientHooks() {
    }

    static void register() {
        KeyMapping.Category.register(KeyBindings.CATEGORY.id());
        // Fabric API for 1.21.9 has no world render event, so the boxes are projected onto the HUD
        HudRenderCallback.EVENT.register((graphics, deltaTracker) -> ProjectedOverlay.render(graphics));
    }
}