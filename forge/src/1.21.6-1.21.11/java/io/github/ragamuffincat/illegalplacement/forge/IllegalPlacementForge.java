package io.github.ragamuffincat.illegalplacement.forge;

import io.github.ragamuffincat.illegalplacement.IllegalPlacement;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(IllegalPlacement.MOD_ID)
public class IllegalPlacementForge {

    public IllegalPlacementForge(FMLJavaModLoadingContext context) {
        IllegalPlacement.init();
        ForgeNetwork.register();
        RegisterCommandsEvent.BUS.addListener(IllegalPlacementForge::registerCommands);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ForgeClientSetup.init(context.getModBusGroup());
        }
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        IllegalPlacement.registerCommands(event.getDispatcher(), event.getBuildContext());
    }
}
