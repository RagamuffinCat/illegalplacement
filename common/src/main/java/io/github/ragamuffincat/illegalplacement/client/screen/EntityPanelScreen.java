package io.github.ragamuffincat.illegalplacement.client.screen;

import io.github.ragamuffincat.illegalplacement.network.Packets;
import io.github.ragamuffincat.illegalplacement.platform.Services;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.List;

public class EntityPanelScreen extends Screen {

    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 4;

    private int entityId;
    private List<Packets.EntityEntry> entries;
    private int missingTicks;

    public EntityPanelScreen(int entityId, List<Packets.EntityEntry> entries) {
        super(Component.translatable("illegalplacement.panel.entity.title"));
        this.entityId = entityId;
        this.entries = entries;
    }

    public void update(int entityId, List<Packets.EntityEntry> entries) {
        this.entityId = entityId;
        this.entries = entries;
        rebuildWidgets();
    }

    @Override
    protected void init() {
        int columns = entries.size() > 6 ? 2 : 1;
        int totalWidth = columns * BUTTON_WIDTH + (columns - 1) * GAP;
        int left = (width - totalWidth) / 2;
        int top = 48;
        for (int i = 0; i < entries.size(); i++) {
            Packets.EntityEntry entry = entries.get(i);
            int x = left + (i % columns) * (BUTTON_WIDTH + GAP);
            int y = top + (i / columns) * (BUTTON_HEIGHT + GAP);
            addRenderableWidget(new TwoWayButton(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, label(entry), (button, backwards) -> {
                int next = Math.floorMod(entry.value() + (backwards ? -entry.step() : entry.step()), entry.modulus());
                Services.PLATFORM.sendToServer(new Packets.SetEntityValue(entityId, entry.key(), next));
            }));
        }
        int rows = (entries.size() + columns - 1) / columns;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds((width - BUTTON_WIDTH) / 2, top + rows * (BUTTON_HEIGHT + GAP) + 12, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private static Component label(Packets.EntityEntry entry) {
        Component name = Labels.property(entry.key());
        Component value;
        if (entry.modulus() == 2) {
            value = Labels.onOff(entry.value() == 1);
        } else if ("Yaw".equals(entry.key())) {
            value = Component.literal(entry.value() + "°");
        } else if ("Facing".equals(entry.key())) {
            value = Labels.value(Direction.from3DDataValue(entry.value()).getName());
        } else {
            value = Component.literal(Integer.toString(entry.value()));
        }
        return Labels.pair(name, value);
    }

    @Override
    public void tick() {
        super.tick();
        Entity entity = minecraft.level.getEntity(entityId);
        if (entity == null) {
            // A respawned hanging entity can take a moment to show up under its new ID, so wait up to a second
            if (++missingTicks > 20) {
                onClose();
            }
        } else {
            missingTicks = 0;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Entity entity = minecraft.level.getEntity(entityId);
        Component name = entity != null ? entity.getDisplayName() : Component.literal("?");
        graphics.drawCenteredString(font, name, width / 2, 14, 0xFFFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("illegalplacement.panel.entity.subtitle"), width / 2, 26, 0xFFA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
