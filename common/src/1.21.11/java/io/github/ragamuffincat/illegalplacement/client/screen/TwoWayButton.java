package io.github.ragamuffincat.illegalplacement.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;

// Button is abstract since 1.21.11, Button.Plain is the regular one
public class TwoWayButton extends Button.Plain {

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
    protected boolean isValidClickButton(MouseButtonInfo info) {
        return info.button() == 0 || info.button() == 1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        lastMouseButton = event.button();
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onPress(InputWithModifiers input) {
        handler.onPress(this, lastMouseButton == 1);
    }
}
