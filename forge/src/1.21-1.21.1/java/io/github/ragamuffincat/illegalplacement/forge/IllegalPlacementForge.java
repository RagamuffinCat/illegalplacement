package io.github.ragamuffincat.illegalplacement.forge;

import io.github.ragamuffincat.illegalplacement.IllegalPlacement;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

// Forge 51 and 52.0 only call no-arg mod constructors
@Mod(IllegalPlacement.MOD_ID)
public class IllegalPlacementForge {

    public IllegalPlacementForge() {
        IllegalPlacement.init();
        ForgeNetwork.register();
        MinecraftForge.EVENT_BUS.addListener(IllegalPlacementForge::registerCommands);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ForgeClientSetup.init(FMLJavaModLoadingContext.get().getModEventBus());
        }
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        IllegalPlacement.registerCommands(event.getDispatcher(), event.getBuildContext());
    }
}
