package io.github.ragamuffincat.illegalplacement.client.screen;

import io.github.ragamuffincat.illegalplacement.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Locale;

public class SettingsScreen extends Screen {

    private static final int WIDTH = 220;
    private static final int HEIGHT = 20;
    private static final int GAP = 4;

    public SettingsScreen() {
        super(Component.translatable("illegalplacement.settings.title"));
    }

    @Override
    protected void init() {
        int x = (width - WIDTH) / 2;
        int y = 44;

        toggle(x, y, "illegalplacement.settings.auto_lock", ClientConfig.autoLock, () -> ClientConfig.autoLock = !ClientConfig.autoLock);
        y += HEIGHT + GAP;
        toggle(x, y, "illegalplacement.settings.auto_freeze", ClientConfig.autoFreeze, () -> ClientConfig.autoFreeze = !ClientConfig.autoFreeze);
        y += HEIGHT + GAP;
        toggle(x, y, "illegalplacement.settings.air_grid", ClientConfig.showAirGrid, () -> ClientConfig.showAirGrid = !ClientConfig.showAirGrid);
        y += HEIGHT + GAP;
        toggle(x, y, "illegalplacement.settings.hud", ClientConfig.showHud, () -> ClientConfig.showHud = !ClientConfig.showHud);
        y += HEIGHT + GAP;

        // TODO use BuildMode.MIN_REACH/MAX_REACH here (and in ClientConfig.load)
        addRenderableWidget(new TwoWayButton(x, y, WIDTH, HEIGHT, reachLabel(), (button, backwards) -> {
            ClientConfig.defaultReach = Mth.clamp(ClientConfig.defaultReach + (backwards ? -1.0 : 1.0), 1.0, 64.0);
            button.setMessage(reachLabel());
        }));
        y += HEIGHT + GAP;
        addRenderableWidget(new TwoWayButton(x, y, WIDTH, HEIGHT, radiusLabel(), (button, backwards) -> {
            ClientConfig.blockEntityRadius = Mth.clamp(ClientConfig.blockEntityRadius + (backwards ? -8 : 8), 0, 64);
            button.setMessage(radiusLabel());
        }));
        y += HEIGHT + GAP + 8;

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds((width - 150) / 2, y, 150, HEIGHT)
                .build());
    }

    private void toggle(int x, int y, String key, boolean value, Runnable flip) {
        addRenderableWidget(Button.builder(Labels.pair(Component.translatable(key), Labels.onOff(value)), b -> {
                    flip.run();
                    rebuildWidgets();
                })
                .bounds(x, y, WIDTH, HEIGHT)
                .tooltip(Tooltip.create(Component.translatable(key + ".tooltip")))
                .build());
    }

    private static Component reachLabel() {
        return Labels.pair(Component.translatable("illegalplacement.settings.default_reach"),
                Component.literal(String.format(Locale.ROOT, "%.0f", ClientConfig.defaultReach)));
    }

    private static Component radiusLabel() {
        return Labels.pair(Component.translatable("illegalplacement.settings.block_entity_radius"),
                ClientConfig.blockEntityRadius == 0
                        ? Labels.onOff(false)
                        : Component.literal(Integer.toString(ClientConfig.blockEntityRadius)));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("illegalplacement.settings.hint"), width / 2, 28, 0xFFA0A0A0);
    }

    @Override
    public void removed() {
        ClientConfig.save();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
