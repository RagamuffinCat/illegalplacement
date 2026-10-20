package io.github.ragamuffincat.illegalplacement.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.ragamuffincat.illegalplacement.config.ClientConfig;
import io.github.ragamuffincat.illegalplacement.lock.LockStore;
import io.github.ragamuffincat.illegalplacement.lock.Locks;
import io.github.ragamuffincat.illegalplacement.network.Packets;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

public final class BuildRenderer {

    public static final int BLOCK_ENTITY = 0xE6FFBF1A;

    private static final int WHITE = 0x8CFFFFFF;
    private static final int ORANGE = 0xFFFF8C1A;
    private static final int GREEN = 0xE673FF99;
    private static final int CYAN = 0xFF40D9FF;
    private static final int GUIDE = 0x2EFFFFFF;
    private static final int CROSS = 0xFFFFD933;

    @FunctionalInterface
    public interface Lines {
        void line(Vec3 from, Vec3 to, int color);
    }

    private BuildRenderer() {
    }

    // World overlay, called from the loaders' render events. The camera rotation is already applied there, the
    // pose only translates by the camera position.
    public static void renderWorld(Camera camera, float partialTick) {
        if (!BuildMode.isActive()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        BuildTarget target = BuildMode.computeTarget(partialTick);
        Vec3 cam = Draw.cameraPos(camera);

        PoseStack poseStack = new PoseStack();
        poseStack.translate(-cam.x, -cam.y, -cam.z);
        PoseStack.Pose pose = poseStack.last();

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        var linesType = Draw.lines(); // var because RenderType moved packages in 1.21.11
        VertexConsumer lines = buffers.getBuffer(linesType);
        collect(target, (from, to, color) -> {
            Vec3 normal = to.subtract(from).normalize();
            Draw.lineVertex(lines, pose, from, color, normal);
            Draw.lineVertex(lines, pose, to, color, normal);
        });
        buffers.endBatch(linesType);

        // Block entities within the radius, visible through walls. Each edge is a thin strip facing the camera, drawn
        // with the see-through render type that name tags use.
        var quadsType = Draw.seeThrough();
        VertexConsumer[] quads = new VertexConsumer[1];
        blockEntityBoxes(mc.level, cam, box -> {
            if (quads[0] == null) {
                quads[0] = buffers.getBuffer(quadsType);
            }
            for (Vec3[] edge : edges(box)) {
                strip(pose, quads[0], edge[0], edge[1], cam);
            }
        });
        if (quads[0] != null) {
            buffers.endBatch(quadsType);
        }
    }

    // Collects the lines for this frame: the placement guide, the hit block (white, orange if the next update would
    // change it, green if locked), the placement position in cyan and a cross on the clicked face
    public static void collect(BuildTarget target, Lines out) {
        Minecraft mc = Minecraft.getInstance();
        if (ClientConfig.showAirGrid) {
            airGrid(mc.level, target.placePos(), out);
        }
        if (target.onBlock()) {
            Packets.BlockInfo info = BuildMode.inspected(target);
            int color = WHITE;
            if (info != null && info.fragility() != Locks.STABLE) {
                color = ORANGE;
            } else if (info != null && (info.flags() & LockStore.LOCK_STATE) != 0) {
                color = GREEN;
            }
            box(new AABB(target.clickedPos()).inflate(0.004), color, out);
        }
        box(new AABB(target.placePos()).inflate(0.002), CYAN, out);
        faceCross(target, out);
    }

    // Empty cells in the 3x3x3 around the target. 5x5x5 was too busy
    private static void airGrid(ClientLevel level, BlockPos center, Lines out) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    pos.setWithOffset(center, dx, dy, dz);
                    if (level.getBlockState(pos).isAir()) {
                        box(new AABB(pos).deflate(0.01), GUIDE, out);
                    }
                }
            }
        }
    }

    private static void faceCross(BuildTarget target, Lines out) {
        Direction face = target.face();
        // Center of the clicked face, moved slightly outward against z-fighting
        Vec3 center = Vec3.atCenterOf(target.clickedPos())
                .add(face.getStepX() * 0.502, face.getStepY() * 0.502, face.getStepZ() * 0.502);
        Direction a;
        Direction b;
        // Two directions within the face plane
        switch (face.getAxis()) {
            case X -> { a = Direction.UP; b = Direction.SOUTH; }
            case Y -> { a = Direction.EAST; b = Direction.SOUTH; }
            default -> { a = Direction.UP; b = Direction.EAST; }
        }
        Vec3 u = new Vec3(a.getStepX(), a.getStepY(), a.getStepZ()).scale(0.5);
        Vec3 v = new Vec3(b.getStepX(), b.getStepY(), b.getStepZ()).scale(0.5);
        out.line(center.add(u).add(v), center.subtract(u).subtract(v), CROSS);
        out.line(center.add(u).subtract(v), center.subtract(u).add(v), CROSS);
    }

    private static void box(AABB b, int color, Lines out) {
        for (Vec3[] edge : edges(b)) {
            out.line(edge[0], edge[1], color);
        }
    }

    // 12 edges: corner pairs whose indices differ in one bit
    public static Vec3[][] edges(AABB b) {
        Vec3[] c = new Vec3[8];
        for (int i = 0; i < 8; i++) {
            c[i] = new Vec3((i & 1) == 0 ? b.minX : b.maxX, (i & 2) == 0 ? b.minY : b.maxY, (i & 4) == 0 ? b.minZ : b.maxZ);
        }
        return new Vec3[][]{
                {c[0], c[1]}, {c[2], c[3]}, {c[4], c[5]}, {c[6], c[7]},
                {c[0], c[2]}, {c[1], c[3]}, {c[4], c[6]}, {c[5], c[7]},
                {c[0], c[4]}, {c[1], c[5]}, {c[2], c[6]}, {c[3], c[7]}};
    }

    // Boxes for all block entities within ClientConfig.blockEntityRadius
    public static void blockEntityBoxes(ClientLevel level, Vec3 cam, Consumer<AABB> out) {
        int radius = ClientConfig.blockEntityRadius;
        if (radius <= 0) {
            return;
        }
        double radiusSq = (double) radius * radius;
        BlockPos origin = BlockPos.containing(cam);
        ChunkPos centerChunk = new ChunkPos(origin);
        int chunkRadius = (radius >> 4) + 1;
        for (int cx = centerChunk.x - chunkRadius; cx <= centerChunk.x + chunkRadius; cx++) {
            for (int cz = centerChunk.z - chunkRadius; cz <= centerChunk.z + chunkRadius; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, false);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    BlockPos pos = blockEntity.getBlockPos();
                    if (pos.distSqr(origin) <= radiusSq) {
                        out.accept(new AABB(pos).deflate(0.06));
                    }
                }
            }
        }
    }

    // About 1.5 px wide at any distance. Both windings, otherwise back-face culling can drop it
    private static void strip(PoseStack.Pose pose, VertexConsumer quads, Vec3 from, Vec3 to, Vec3 cam) {
        Vec3 mid = from.add(to).scale(0.5);
        Vec3 side = to.subtract(from).cross(mid.subtract(cam)).normalize().scale(Math.max(0.006, mid.distanceTo(cam) * 0.0014));
        Vec3[] corners = {from.add(side), to.add(side), to.subtract(side), from.subtract(side)};
        for (int i = 0; i < 4; i++) {
            quadVertex(pose, quads, corners[i]);
        }
        for (int i = 3; i >= 0; i--) {
            quadVertex(pose, quads, corners[i]);
        }
    }

    private static void quadVertex(PoseStack.Pose pose, VertexConsumer quads, Vec3 at) {
        quads.addVertex(pose, (float) at.x, (float) at.y, (float) at.z).setColor(BLOCK_ENTITY).setLight(LightTexture.FULL_BRIGHT);
    }
}
