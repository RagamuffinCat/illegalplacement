package io.github.ragamuffincat.illegalplacement.neoforge;

import io.github.ragamuffincat.illegalplacement.client.BuildHud;
import io.github.ragamuffincat.illegalplacement.client.BuildMode;
import io.github.ragamuffincat.illegalplacement.client.BuildRenderer;
import io.github.ragamuffincat.illegalplacement.client.IllegalPlacementClient;
import io.github.ragamuffincat.illegalplacement.client.KeyBindings;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

final class NeoForgeClientSetup {

    private NeoForgeClientSetup() {
    }

    static void init(IEventBus modBus) {
        IllegalPlacementClient.init();
        modBus.addListener(NeoForgeClientSetup::registerKeys);
        NeoForge.EVENT_BUS.addListener(NeoForgeClientSetup::onClientTick);
        NeoForge.EVENT_BUS.addListener(NeoForgeClientSetup::onRenderLevel);
        NeoForge.EVENT_BUS.addListener(NeoForgeClientSetup::onRenderGui);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(KeyBindings.CATEGORY);
        KeyBindings.ALL.forEach(event::register);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        BuildMode.clientTick(Minecraft.getInstance());
    }

    // Block entities are part of the entity pass since 1.21.9
    private static void onRenderLevel(RenderLevelStageEvent.AfterEntities event) {
        Minecraft mc = Minecraft.getInstance();
        BuildRenderer.renderWorld(mc.gameRenderer.getMainCamera(), mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
    }

    private static void onRenderGui(RenderGuiEvent.Post event) {
        BuildHud.render(event.getGuiGraphics());
    }
}
