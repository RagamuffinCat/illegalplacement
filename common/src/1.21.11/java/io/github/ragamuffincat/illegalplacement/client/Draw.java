package io.github.ragamuffincat.illegalplacement.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;

// The render types and vertex layout BuildRenderer draws with
public final class Draw {

    private Draw() {
    }

    public static Vec3 cameraPos(Camera camera) {
        return camera.position();
    }

    static RenderType lines() {
        return RenderTypes.lines();
    }

    // Colored quads without depth test
    static RenderType seeThrough() {
        return RenderTypes.textBackgroundSeeThrough();
    }

    static void lineVertex(VertexConsumer lines, PoseStack.Pose pose, Vec3 at, int color, Vec3 normal) {
        lines.addVertex(pose, (float) at.x, (float) at.y, (float) at.z)
                .setColor(color)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z)
                .setLineWidth(Minecraft.getInstance().getWindow().getAppropriateLineWidth());
    }
}
