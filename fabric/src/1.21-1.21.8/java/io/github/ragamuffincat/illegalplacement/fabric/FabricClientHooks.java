package io.github.ragamuffincat.illegalplacement.fabric;

import io.github.ragamuffincat.illegalplacement.client.BuildRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

final class FabricClientHooks {

    private FabricClientHooks() {
    }

    static void register() {
        // Same stage as the vanilla block outline
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context ->
                BuildRenderer.renderWorld(context.camera(), context.tickCounter().getGameTimeDeltaPartialTick(false)));
    }
}
