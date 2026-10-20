package io.github.ragamuffincat.illegalplacement.forge;

import com.mojang.blaze3d.framegraph.FramePass;
import io.github.ragamuffincat.illegalplacement.IllegalPlacement;
import io.github.ragamuffincat.illegalplacement.client.BuildHud;
import io.github.ragamuffincat.illegalplacement.client.BuildMode;
import io.github.ragamuffincat.illegalplacement.client.BuildRenderer;
import io.github.ragamuffincat.illegalplacement.client.IllegalPlacementClient;
import io.github.ragamuffincat.illegalplacement.client.KeyBindings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.FramePassManager;
import net.minecraftforge.client.event.AddFramePassEvent;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;

final class ForgeClientSetup {

    private ForgeClientSetup() {
    }

    static void init(BusGroup modBus) {
        IllegalPlacementClient.init();
        RegisterKeyMappingsEvent.getBus(modBus).addListener(ForgeClientSetup::registerKeys);
        AddGuiOverlayLayersEvent.getBus(modBus).addListener(ForgeClientSetup::registerHudLayer);
        TickEvent.ClientTickEvent.Post.BUS.addListener(ForgeClientSetup::onClientTick);
        AddFramePassEvent.BUS.addListener(ForgeClientSetup::addFramePass);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        KeyBindings.ALL.forEach(event::register);
    }

    private static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        BuildMode.clientTick(Minecraft.getInstance());
    }

    // Root layer, drawn after the vanilla HUD
    private static void registerHudLayer(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(ResourceLocation.fromNamespaceAndPath(IllegalPlacement.MOD_ID, "build_hud"),
                (graphics, deltaTracker) -> BuildHud.render(graphics));
    }

    // Separate frame pass that draws into the main target
    private static void addFramePass(AddFramePassEvent event) {
        event.addPass(ResourceLocation.fromNamespaceAndPath(IllegalPlacement.MOD_ID, "build_overlay"), new FramePassManager.PassDefinition() {
            @Override
            public void targets(LevelTargetBundle targets, FramePass pass) {
                targets.main = pass.readsAndWrites(targets.main);
            }
            
            @Override
            public void executes() {
                Minecraft mc = Minecraft.getInstance();
                BuildRenderer.renderWorld(mc.gameRenderer.getMainCamera(), mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
            }
        });
    }
}
