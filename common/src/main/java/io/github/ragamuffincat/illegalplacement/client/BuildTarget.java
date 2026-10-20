package io.github.ragamuffincat.illegalplacement.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * @param clickedPos the block that was hit, or the empty cell if nothing was hit
 * @param face       the clicked face; for an empty cell, the side facing the player
 * @param hit        exact hit position, needed for slabs, stairs and other half placements
 * @param placePos   only a preview, the server decides where the block goes
 */
public record BuildTarget(BlockPos clickedPos, Direction face, Vec3 hit, BlockPos placePos, boolean onBlock) {
}
