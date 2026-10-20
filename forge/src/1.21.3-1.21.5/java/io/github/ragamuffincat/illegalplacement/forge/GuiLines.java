package io.github.ragamuffincat.illegalplacement.forge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;

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
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x1, y1, 0.0F);
        pose.mulPose(Axis.ZP.rotation((float) Math.atan2(dy, dx)));
        pose.scale(1.0F / guiScale, 1.0F / guiScale, 1.0F);
        graphics.fill(0, -1, length, 1, color);
        pose.popPose();
    }
}
