package io.github.ragamuffincat.illegalplacement.forge;

import io.github.ragamuffincat.illegalplacement.IllegalPlacement;
import io.github.ragamuffincat.illegalplacement.client.BuildHud;
import io.github.ragamuffincat.illegalplacement.client.BuildMode;
import io.github.ragamuffincat.illegalplacement.client.IllegalPlacementClient;
import io.github.ragamuffincat.illegalplacement.client.KeyBindings;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;

final class ForgeClientSetup {

    private ForgeClientSetup() {
    }

    static void init(IEventBus modBus) {
        IllegalPlacementClient.init();
        modBus.addListener(ForgeClientSetup::registerKeys);
        modBus.addListener(ForgeClientSetup::registerHudLayer);
        MinecraftForge.EVENT_BUS.addListener(ForgeClientSetup::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(ForgeClientSetup::onComputeFov);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        KeyBindings.ALL.forEach(event::register);
    }

    private static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        BuildMode.clientTick(Minecraft.getInstance());
    }

    private static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (event.usedConfiguredFov()) {
            ProjectedOverlay.fov = event.getFOV();
        }
    }

    // Root layer, drawn after the vanilla HUD. Forge 53-55 have no level render event, the projected boxes are
    // drawn here too.
    private static void registerHudLayer(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(ResourceLocation.fromNamespaceAndPath(IllegalPlacement.MOD_ID, "build_hud"), (graphics, deltaTracker) -> {
            ProjectedOverlay.render(graphics);
            BuildHud.render(graphics);
        });
    }
}
