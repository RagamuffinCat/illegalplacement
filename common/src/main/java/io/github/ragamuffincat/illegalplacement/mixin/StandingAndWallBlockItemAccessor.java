package io.github.ragamuffincat.illegalplacement.mixin;

import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StandingAndWallBlockItem.class)
public interface StandingAndWallBlockItemAccessor {

    @Accessor("wallBlock")
    Block illegalplacement$getWallBlock();
}
