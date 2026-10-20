package io.github.ragamuffincat.illegalplacement.forge;

import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix3x2fStack;

final class GuiLines {

    private GuiLines() {
    }

    // A 2 px wide rectangle rotated along the line, measured in screen pixels
    static void line(GuiGraphics graphics, float x1, float y1, float x2, float y2, int color, float guiScale) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        int length = Math.round((float) Math.sqrt(dx * dx + dy * dy) * guiScale);
        if (length == 0) {
            return;
        }
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x1, y1);
        pose.rotate((float) Math.atan2(dy, dx));
        pose.scale(1.0F / guiScale, 1.0F / guiScale);
        graphics.fill(0, -1, length, 1, color);
        pose.popMatrix();
    }
}
