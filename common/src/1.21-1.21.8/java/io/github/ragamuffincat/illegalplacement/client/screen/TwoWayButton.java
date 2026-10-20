package io.github.ragamuffincat.illegalplacement.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class TwoWayButton extends Button {

    @FunctionalInterface
    public interface Handler {
        void onPress(TwoWayButton button, boolean backwards);
    }

    private final Handler handler;
    private int lastMouseButton;

    public TwoWayButton(int x, int y, int width, int height, Component message, Handler handler) {
        super(x, y, width, height, message, button -> { }, DEFAULT_NARRATION);
        this.handler = handler;
    }

    @Override
    // Left click goes to the next value, right click to the previous one
    protected boolean isValidClickButton(int button) {
        return button == 0 || button == 1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        lastMouseButton = button;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onPress() {
        handler.onPress(this, lastMouseButton == 1);
    }
}
