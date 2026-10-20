package io.github.ragamuffincat.illegalplacement.fabric;

import io.github.ragamuffincat.illegalplacement.client.BuildRenderer;
import io.github.ragamuffincat.illegalplacement.client.KeyBindings;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

final class FabricClientHooks {

    private FabricClientHooks() {
    }

    static void register() {
        KeyMapping.Category.register(KeyBindings.CATEGORY.id());
        // Same stage as the vanilla block outline
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {
            Minecraft mc = Minecraft.getInstance();
            BuildRenderer.renderWorld(mc.gameRenderer.getMainCamera(), mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        });
    }
}
