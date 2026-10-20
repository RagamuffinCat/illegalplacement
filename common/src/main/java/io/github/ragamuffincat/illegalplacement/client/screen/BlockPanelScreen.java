package io.github.ragamuffincat.illegalplacement.client.screen;

import io.github.ragamuffincat.illegalplacement.config.ClientConfig;
import io.github.ragamuffincat.illegalplacement.lock.LockStore;
import io.github.ragamuffincat.illegalplacement.network.Packets;
import io.github.ragamuffincat.illegalplacement.platform.Services;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.List;

public class BlockPanelScreen extends Screen {

    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 4;

    private final BlockPos pos;
    private int flags;
    private int fragility;
    private BlockState shownState;

    public BlockPanelScreen(BlockPos pos, int flags, int fragility) {
        super(Component.translatable("illegalplacement.panel.block.title"));
        this.pos = pos;
        this.flags = flags;
        this.fragility = fragility;
    }

    public BlockPos pos() {
        return pos;
    }

    public void setInfo(int flags, int fragility) {
        this.fragility = fragility;
        if (flags != this.flags) {
            this.flags = flags;
            rebuildWidgets();
        }
    }

    private BlockState currentState() {
        return minecraft.level.getBlockState(pos);
    }

    @Override
    protected void init() {
        shownState = currentState();
        List<Property<?>> properties = new ArrayList<>(shownState.getProperties());

        int columns = properties.size() > 6 ? 2 : 1; // Walls, redstone wire etc.
        int totalWidth = columns * BUTTON_WIDTH + (columns - 1) * GAP;
        int left = (width - totalWidth) / 2;
        int top = 48;

        for (int i = 0; i < properties.size(); i++) {
            int col = i % columns;
            int row = i / columns;
            addRenderableWidget(propertyButton(properties.get(i), left + col * (BUTTON_WIDTH + GAP), top + row * (BUTTON_HEIGHT + GAP)));
        }
        int rows = (properties.size() + columns - 1) / columns;
        int y = top + Math.max(rows, 1) * (BUTTON_HEIGHT + GAP) + 8;

        int half = BUTTON_WIDTH;
        int bottomLeft = (width - (BUTTON_WIDTH * 2 + GAP)) / 2;
        addRenderableWidget(Button.builder(
                        Labels.pair(Component.translatable("illegalplacement.panel.lock_state"), Labels.onOff((flags & LockStore.LOCK_STATE) != 0)),
                        b -> sendFlags(flags ^ LockStore.LOCK_STATE))
                .bounds(bottomLeft, y, half, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("illegalplacement.panel.lock_state.tooltip")))
                .build());
        addRenderableWidget(Button.builder(
                        Labels.pair(Component.translatable("illegalplacement.panel.freeze_time"), Labels.onOff((flags & LockStore.FREEZE_TIME) != 0)),
                        b -> sendFlags(flags ^ LockStore.FREEZE_TIME))
                .bounds(bottomLeft + half + GAP, y, half, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("illegalplacement.panel.freeze_time.tooltip")))
                .build());
        y += BUTTON_HEIGHT + GAP;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds((width - BUTTON_WIDTH) / 2, y + 4, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private <T extends Comparable<T>> TwoWayButton propertyButton(Property<T> property, int x, int y) {
        T value = shownState.getValue(property);
        Component label = Labels.pair(Labels.property(property.getName()), Labels.value(property.getName(value)));
        return new TwoWayButton(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, label, (button, backwards) -> {
            List<T> values = new ArrayList<>(property.getPossibleValues());
            int index = values.indexOf(shownState.getValue(property));
            int next = Math.floorMod(index + (backwards ? -1 : 1), values.size());
            String serialized = property.getName(values.get(next));
            Services.PLATFORM.sendToServer(new Packets.SetBlockProperty(pos, property.getName(), serialized, ClientConfig.autoLock));
        });
    }

    private void sendFlags(int newFlags) {
        Services.PLATFORM.sendToServer(new Packets.SetBlockFlags(pos, newFlags));
    }

    @Override
    public void tick() {
        super.tick();
        // Rebuild when the server changed the state, close when the block is gone
        BlockState state = currentState();
        if (state.isAir()) {
            onClose();
        } else if (state != shownState) {
            rebuildWidgets();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        BlockState state = shownState;
        graphics.drawCenteredString(font, state.getBlock().getName(), width / 2, 14, 0xFFFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("illegalplacement.panel.block.subtitle",
                pos.getX(), pos.getY(), pos.getZ()), width / 2, 26, 0xFFA0A0A0);
        Component fragile = Labels.fragility(fragility);
        if (fragile != null) {
            graphics.drawCenteredString(font, fragile, width / 2, 37, Labels.FRAGILE_COLOR);
        }
        if (state.getProperties().isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("illegalplacement.panel.no_properties"), width / 2, 54, 0xFFA0A0A0);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
