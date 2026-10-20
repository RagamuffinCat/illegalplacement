package io.github.ragamuffincat.illegalplacement.fabric;

import io.github.ragamuffincat.illegalplacement.IllegalPlacement;
import io.github.ragamuffincat.illegalplacement.network.Payloads;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class IllegalPlacementFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        IllegalPlacement.init();
        for (Payloads.Serverbound<?> entry : Payloads.SERVERBOUND) {
            registerServerbound(entry);
        }
        for (Payloads.Clientbound<?> entry : Payloads.CLIENTBOUND) {
            registerClientboundType(entry);
        }
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
                IllegalPlacement.registerCommands(dispatcher, buildContext));
    }

    private static <T extends CustomPacketPayload> void registerServerbound(Payloads.Serverbound<T> entry) {
        PayloadTypeRegistry.playC2S().register(entry.type(), entry.codec());
        // Runs on the server thread
        ServerPlayNetworking.registerGlobalReceiver(entry.type(),
                (payload, context) -> entry.handler().accept(payload, context.player()));
    }

    private static <T extends CustomPacketPayload> void registerClientboundType(Payloads.Clientbound<T> entry) {
        // The type is needed on both sides, the receiver is registered in the client initializer
        PayloadTypeRegistry.playS2C().register(entry.type(), entry.codec());
    }
}
