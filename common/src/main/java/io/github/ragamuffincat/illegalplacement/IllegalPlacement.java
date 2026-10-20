package io.github.ragamuffincat.illegalplacement;

import com.mojang.brigadier.CommandDispatcher;
import io.github.ragamuffincat.illegalplacement.config.ServerConfig;
import io.github.ragamuffincat.illegalplacement.network.Payloads;
import io.github.ragamuffincat.illegalplacement.server.LockCommands;
import io.github.ragamuffincat.illegalplacement.server.PasteCommands;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IllegalPlacement {

    public static final String MOD_ID = "illegalplacement";
    public static final String MOD_NAME = "Illegal Placement";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    private IllegalPlacement() {
    }

    public static void init() {
        ServerConfig.load();
        Payloads.bootstrap();
        LOG.info("{} initialized", MOD_NAME);
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        PasteCommands.register(dispatcher, buildContext);
        LockCommands.register(dispatcher);
    }
}
