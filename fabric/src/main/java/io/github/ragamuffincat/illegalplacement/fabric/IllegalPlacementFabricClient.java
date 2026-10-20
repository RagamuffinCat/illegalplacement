package io.github.ragamuffincat.illegalplacement.fabric;

import io.github.ragamuffincat.illegalplacement.client.BuildHud;
import io.github.ragamuffincat.illegalplacement.client.BuildMode;
import io.github.ragamuffincat.illegalplacement.client.IllegalPlacementClient;
import io.github.ragamuffincat.illegalplacement.client.KeyBindings;
import io.github.ragamuffincat.illegalplacement.network.Payloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class IllegalPlacementFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        IllegalPlacementClient.init();
        KeyBindings.ALL.forEach(KeyBindingHelper::registerKeyBinding);
        for (Payloads.Clientbound<?> entry : Payloads.CLIENTBOUND) {
            registerClientbound(entry);
        }
        ClientTickEvents.END_CLIENT_TICK.register(BuildMode::clientTick);
        FabricClientHooks.register();
        HudRenderCallback.EVENT.register((graphics, deltaTracker) -> BuildHud.render(graphics));
    }

    private static <T extends CustomPacketPayload> void registerClientbound(Payloads.Clientbound<T> entry) {
        ClientPlayNetworking.registerGlobalReceiver(entry.type(), (payload, context) -> Payloads.handleOnClient(payload));
    }
}
