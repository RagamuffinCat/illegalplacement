package io.github.ragamuffincat.illegalplacement.forge;

import io.github.ragamuffincat.illegalplacement.client.BuildMode;
import io.github.ragamuffincat.illegalplacement.client.BuildRenderer;
import io.github.ragamuffincat.illegalplacement.client.BuildTarget;
import io.github.ragamuffincat.illegalplacement.client.Draw;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

// Forge 53-57 have no level render event, so the build mode boxes are projected onto the HUD instead.
// That way they show through blocks and don't follow view bobbing.
final class ProjectedOverlay {

    // View space looks down -z; only what is in front of this plane is drawn
    private static final float NEAR = -0.05F;

    // FOV of the last world frame, from ViewportEvent.ComputeFov
    static float fov = 70.0F;

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
        Matrix4f projection = mc.gameRenderer.getProjectionMatrix(fov);
        Quaternionf view = camera.rotation().conjugate(new Quaternionf());
        float guiScale = (float) mc.getWindow().getGuiScale();
        BuildRenderer.Lines out = (from, to, color) -> {
            Vector3f a = view.transform(new Vector3f((float) (from.x - cam.x), (float) (from.y - cam.y), (float) (from.z - cam.z)));
            Vector3f b = view.transform(new Vector3f((float) (to.x - cam.x), (float) (to.y - cam.y), (float) (to.z - cam.z)));
            if (a.z > NEAR && b.z > NEAR) {
                return;
            }
            // Clip the part behind the camera
            if (a.z > NEAR) {
                a = clip(b, a);
            } else if (b.z > NEAR) {
                b = clip(a, b);
            }
            Vector3f sa = screen(projection, a, graphics);
            Vector3f sb = screen(projection, b, graphics);
            GuiLines.line(graphics, sa.x, sa.y, sb.x, sb.y, color, guiScale);
        };
        BuildRenderer.collect(target, out);
        BuildRenderer.blockEntityBoxes(mc.level, cam, box -> {
            for (Vec3[] edge : BuildRenderer.edges(box)) {
                out.line(edge[0], edge[1], BuildRenderer.BLOCK_ENTITY);
            }
        });
    }

    // Point between front and back where z = NEAR
    private static Vector3f clip(Vector3f front, Vector3f back) {
        float t = (NEAR - front.z) / (back.z - front.z);
        return new Vector3f(front).lerp(back, t);
    }

    // View space to GUI coordinates
    private static Vector3f screen(Matrix4f projection, Vector3f v, GuiGraphics graphics) {
        Vector4f clip = projection.transform(new Vector4f(v, 1.0F));
        float x = clip.x / clip.w;
        float y = clip.y / clip.w;
        return new Vector3f((x * 0.5F + 0.5F) * graphics.guiWidth(), (0.5F - y * 0.5F) * graphics.guiHeight(), 0.0F);
    }
}
