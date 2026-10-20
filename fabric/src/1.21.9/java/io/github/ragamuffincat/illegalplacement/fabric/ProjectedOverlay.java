package io.github.ragamuffincat.illegalplacement.fabric;

import io.github.ragamuffincat.illegalplacement.client.BuildMode;
import io.github.ragamuffincat.illegalplacement.client.BuildRenderer;
import io.github.ragamuffincat.illegalplacement.client.BuildTarget;
import io.github.ragamuffincat.illegalplacement.client.Draw;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;

// Build mode boxes projected onto the HUD. They show through blocks and don't follow view bobbing.
final class ProjectedOverlay {

    // Near plane distance, anything closer to the camera is clipped
    private static final double NEAR = 0.05;

    private ProjectedOverlay() {
    }

    static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (!BuildMode.isActive() || mc.screen != null) {
            return;
        }
        BuildTarget target = BuildMode.computeTarget(mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cam = Draw.cameraPos(camera);
        Vec3 forward = new Vec3(camera.getLookVector());
        float guiScale = (float) mc.getWindow().getGuiScale();
        BuildRenderer.Lines out = (from, to, color) -> {
            double da = from.subtract(cam).dot(forward);
            double db = to.subtract(cam).dot(forward);
            if (da < NEAR && db < NEAR) {
                return;
            }
            // Clip the part behind the camera
            if (da < NEAR) {
                from = to.lerp(from, (db - NEAR) / (db - da));
            } else if (db < NEAR) {
                to = from.lerp(to, (da - NEAR) / (da - db));
            }
            Vec3 a = mc.gameRenderer.projectPointToScreen(from);
            Vec3 b = mc.gameRenderer.projectPointToScreen(to);
            line(graphics, guiX(a, graphics), guiY(a, graphics), guiX(b, graphics), guiY(b, graphics), color, guiScale);
        };
        BuildRenderer.collect(target, out);
        BuildRenderer.blockEntityBoxes(mc.level, cam, box -> {
            for (Vec3[] edge : BuildRenderer.edges(box)) {
                out.line(edge[0], edge[1], BuildRenderer.BLOCK_ENTITY);
            }
        });
    }

    private static float guiX(Vec3 ndc, GuiGraphics graphics) {
        return (float) (ndc.x * 0.5 + 0.5) * graphics.guiWidth();
    }

    private static float guiY(Vec3 ndc, GuiGraphics graphics) {
        return (float) (0.5 - ndc.y * 0.5) * graphics.guiHeight();
    }

    // A 2 px wide rectangle rotated along the line, measured in screen pixels
    private static void line(GuiGraphics graphics, float x1, float y1, float x2, float y2, int color, float guiScale) {
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
