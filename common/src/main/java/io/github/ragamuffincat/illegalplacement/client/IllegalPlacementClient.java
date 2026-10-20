package io.github.ragamuffincat.illegalplacement.client;

import io.github.ragamuffincat.illegalplacement.client.screen.BlockPanelScreen;
import io.github.ragamuffincat.illegalplacement.client.screen.EntityPanelScreen;
import io.github.ragamuffincat.illegalplacement.config.ClientConfig;
import io.github.ragamuffincat.illegalplacement.network.Packets;
import io.github.ragamuffincat.illegalplacement.network.Payloads;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class IllegalPlacementClient {

    private IllegalPlacementClient() {
    }

    public static void init() {
        ClientConfig.load();
        Payloads.setClientHandler(Packets.BlockInfo.TYPE, IllegalPlacementClient::onBlockInfo);
        Payloads.setClientHandler(Packets.EntityInfo.TYPE, IllegalPlacementClient::onEntityInfo);
        Payloads.setClientHandler(Packets.Notice.TYPE, IllegalPlacementClient::onNotice);
    }

    private static void onBlockInfo(Packets.BlockInfo info) {
        Minecraft mc = Minecraft.getInstance();
        BuildMode.onBlockInfo(info);
        if (mc.screen instanceof BlockPanelScreen panel && panel.pos().equals(info.pos())) {
            panel.setInfo(info.flags(), info.fragility());
        } else if (info.pos().equals(BuildMode.pendingBlockPanel)) {
            BuildMode.pendingBlockPanel = null;
            mc.setScreen(new BlockPanelScreen(info.pos(), info.flags(), info.fragility()));
        }
    }

    private static void onEntityInfo(Packets.EntityInfo info) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof EntityPanelScreen panel) {
            // The ID changes when the server respawned a hanging entity
            panel.update(info.entityId(), info.entries());
        } else if (info.entityId() == BuildMode.pendingEntityPanel) {
            BuildMode.pendingEntityPanel = -1;
            mc.setScreen(new EntityPanelScreen(info.entityId(), info.entries()));
        }
    }

    private static void onNotice(Packets.Notice notice) {
        BuildMode.actionBar(Minecraft.getInstance(), Component.translatable(notice.translationKey()));
    }
}
