package io.github.ragamuffincat.illegalplacement.forge;

import io.github.ragamuffincat.illegalplacement.client.BuildHud;
import io.github.ragamuffincat.illegalplacement.client.BuildMode;
import io.github.ragamuffincat.illegalplacement.client.BuildRenderer;
import io.github.ragamuffincat.illegalplacement.client.IllegalPlacementClient;
import io.github.ragamuffincat.illegalplacement.client.KeyBindings;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;

final class ForgeClientSetup {

    private ForgeClientSetup() {
    }

    static void init(IEventBus modBus) {
        IllegalPlacementClient.init();
        modBus.addListener(ForgeClientSetup::registerKeys);
        MinecraftForge.EVENT_BUS.addListener(ForgeClientSetup::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(ForgeClientSetup::onRenderLevel);
        MinecraftForge.EVENT_BUS.addListener(ForgeClientSetup::onChatOverlay);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        KeyBindings.ALL.forEach(event::register);
    }

    private static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        BuildMode.clientTick(Minecraft.getInstance());
    }

    private static void onRenderLevel(RenderLevelStageEvent event) {
        // Close to where vanilla draws the block outline
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            BuildRenderer.renderWorld(event.getCamera(), event.getPartialTick());
        }
    }

    // Forge 51 and 52.0 have no HUD layer event; the chat overlay event fires every frame and does the job
    private static void onChatOverlay(CustomizeGuiOverlayEvent.Chat event) {
        BuildHud.render(event.getGuiGraphics());
    }
}
