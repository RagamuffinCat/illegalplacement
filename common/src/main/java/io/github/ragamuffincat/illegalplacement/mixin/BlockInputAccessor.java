package io.github.ragamuffincat.illegalplacement.mixin;

import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockInput.class)
public interface BlockInputAccessor {

    @Accessor("tag")
    @Nullable
    CompoundTag illegalplacement$getTag();
}
