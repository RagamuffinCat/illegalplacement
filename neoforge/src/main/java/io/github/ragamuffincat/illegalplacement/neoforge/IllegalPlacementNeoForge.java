package io.github.ragamuffincat.illegalplacement.neoforge;

import io.github.ragamuffincat.illegalplacement.IllegalPlacement;
import io.github.ragamuffincat.illegalplacement.network.Payloads;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(IllegalPlacement.MOD_ID)
public class IllegalPlacementNeoForge {

    public IllegalPlacementNeoForge(IEventBus modBus, Dist dist) {
        IllegalPlacement.init();
        modBus.addListener(IllegalPlacementNeoForge::registerPayloads);
        NeoForge.EVENT_BUS.addListener(IllegalPlacementNeoForge::registerCommands);
        if (dist.isClient()) {
            NeoForgeClientSetup.init(modBus);
        }
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        IllegalPlacement.registerCommands(event.getDispatcher(), event.getBuildContext());
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        // optional() lets vanilla clients connect. Handlers run on the main thread by default
        PayloadRegistrar registrar = event.registrar("1").optional();
        for (Payloads.Serverbound<?> entry : Payloads.SERVERBOUND) {
            serverbound(registrar, entry);
        }
        for (Payloads.Clientbound<?> entry : Payloads.CLIENTBOUND) {
            clientbound(registrar, entry);
        }
    }

    private static <T extends CustomPacketPayload> void serverbound(PayloadRegistrar registrar, Payloads.Serverbound<T> entry) {
        registrar.playToServer(entry.type(), entry.codec(), (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                entry.handler().accept(payload, player);
            }
        });
    }

    private static <T extends CustomPacketPayload> void clientbound(PayloadRegistrar registrar, Payloads.Clientbound<T> entry) {
        // Registered on both sides, but only ever received on the client
        registrar.playToClient(entry.type(), entry.codec(), (payload, context) -> Payloads.handleOnClient(payload));
    }
}
