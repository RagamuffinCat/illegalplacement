package io.github.ragamuffincat.illegalplacement.forge;

import io.github.ragamuffincat.illegalplacement.IllegalPlacement;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(IllegalPlacement.MOD_ID)
public class IllegalPlacementForge {

    public IllegalPlacementForge(FMLJavaModLoadingContext context) {
        IllegalPlacement.init();
        ForgeNetwork.register();
        MinecraftForge.EVENT_BUS.addListener(IllegalPlacementForge::registerCommands);
        IEventBus modBus = context.getModEventBus();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ForgeClientSetup.init(modBus);
        }
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        IllegalPlacement.registerCommands(event.getDispatcher(), event.getBuildContext());
    }
}
