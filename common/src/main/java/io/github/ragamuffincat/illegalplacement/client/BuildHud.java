package io.github.ragamuffincat.illegalplacement.client;

import io.github.ragamuffincat.illegalplacement.client.screen.Labels;
import io.github.ragamuffincat.illegalplacement.config.ClientConfig;
import io.github.ragamuffincat.illegalplacement.lock.LockStore;
import io.github.ragamuffincat.illegalplacement.network.Packets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Locale;

public final class BuildHud {

    private BuildHud() {
    }

    public static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (!BuildMode.isActive() || !ClientConfig.showHud || mc.options.hideGui || mc.screen != null) {
            return;
        }
        Font font = mc.font;
        int x = 4;
        int y = 4;
        int reachColor = BuildMode.hudTicks() < 20 ? 0xFFFFD34D : 0xFFFFFFFF;

        graphics.drawString(font, Component.translatable("illegalplacement.hud.title"), x, y, 0xFF5BD8FF, true);
        y += 10;
        graphics.drawString(font, Component.translatable("illegalplacement.hud.reach",
                String.format(Locale.ROOT, "%.2f", BuildMode.reach())), x, y, reachColor, true);
        y += 10;
        graphics.drawString(font, Component.translatable("illegalplacement.hud.auto",
                onOff(ClientConfig.autoLock), onOff(ClientConfig.autoFreeze)), x, y, 0xFFCCCCCC, true);
        y += 10;

        BuildTarget target = BuildMode.lastTarget();
        if (target != null && mc.level != null) {
            Component what;
            if (target.onBlock()) {
                BlockState state = mc.level.getBlockState(target.clickedPos());
                what = state.getBlock().getName();
            } else {
                what = Component.translatable("illegalplacement.hud.empty");
            }
            graphics.drawString(font, Component.translatable("illegalplacement.hud.target", what,
                    target.placePos().getX(), target.placePos().getY(), target.placePos().getZ()), x, y, 0xFFCCCCCC, true);
            y += 10;
            Packets.BlockInfo info = BuildMode.inspected(target);
            if (info != null) {
                status(graphics, font, info, x, y);
            }
        }
    }

    // Orange warning with a hint if the next update would change the block, otherwise the lock and freeze state
    private static void status(GuiGraphics graphics, Font font, Packets.BlockInfo info, int x, int y) {
        Component fragile = Labels.fragility(info.fragility());
        if (fragile != null) {
            graphics.drawString(font, Component.translatable("illegalplacement.hud.fragile", fragile,
                    KeyBindings.OPEN_PANEL.getTranslatedKeyMessage()), x, y, Labels.FRAGILE_COLOR, true);
            return;
        }
        boolean locked = (info.flags() & LockStore.LOCK_STATE) != 0;
        boolean frozen = (info.flags() & LockStore.FREEZE_TIME) != 0;
        if (locked || frozen) {
            String key = locked && frozen ? "illegalplacement.hud.locked_frozen" : locked ? "illegalplacement.hud.locked" : "illegalplacement.hud.frozen";
            graphics.drawString(font, Component.translatable(key), x, y, Labels.LOCKED_COLOR, true);
        }
    }

    private static Component onOff(boolean value) {
        return Component.translatable(value ? "illegalplacement.value.on" : "illegalplacement.value.off");
    }
}
