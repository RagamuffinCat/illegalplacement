package io.github.ragamuffincat.illegalplacement.forge;

import io.github.ragamuffincat.illegalplacement.client.BuildHud;
import io.github.ragamuffincat.illegalplacement.client.BuildMode;
import io.github.ragamuffincat.illegalplacement.client.IllegalPlacementClient;
import io.github.ragamuffincat.illegalplacement.client.KeyBindings;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;

final class ForgeClientSetup {

    private ForgeClientSetup() {
    }

    static void init(BusGroup modBus) {
        IllegalPlacementClient.init();
        RegisterKeyMappingsEvent.getBus(modBus).addListener(ForgeClientSetup::registerKeys);
        TickEvent.ClientTickEvent.Post.BUS.addListener(ForgeClientSetup::onClientTick);
        CustomizeGuiOverlayEvent.Chat.BUS.addListener(ForgeClientSetup::onChatOverlay);
        ViewportEvent.ComputeFov.BUS.addListener(ForgeClientSetup::onComputeFov);
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

    // Forge 56 and 57 have neither a HUD layer nor a level render event. The HUD and the projected boxes are both
    // drawn from the chat overlay event, which fires every frame.
    private static void onChatOverlay(CustomizeGuiOverlayEvent.Chat event) {
        ProjectedOverlay.render(event.getGuiGraphics());
        BuildHud.render(event.getGuiGraphics());
    }
}
