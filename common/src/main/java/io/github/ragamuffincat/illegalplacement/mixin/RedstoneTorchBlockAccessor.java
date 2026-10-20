package io.github.ragamuffincat.illegalplacement.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RedstoneTorchBlock.class)
public interface RedstoneTorchBlockAccessor {

    // Wall torches override it to check the block they hang on
    @Invoker("hasNeighborSignal")
    boolean illegalplacement$hasNeighborSignal(Level level, BlockPos pos, BlockState state);
}
