package io.github.ragamuffincat.illegalplacement.server;

import io.github.ragamuffincat.illegalplacement.mixin.StandingAndWallBlockItemAccessor;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

// Picks the state for a build mode click. getStateForPlacement is called directly, which skips the canSurvive
// check in BlockItem#getPlacementState. If the block returns null, fallback() builds a state from the click.
// setPlacedBy is never called, so beds and doors only get the clicked half.
public final class Placement {

    private Placement() {
    }

    // Torches, signs, heads and banners use their wall block on side faces
    public static Block blockFor(BlockItem item, Direction clickedFace) {
        if (item instanceof StandingAndWallBlockItem && clickedFace.getAxis().isHorizontal()) {
            return ((StandingAndWallBlockItemAccessor) item).illegalplacement$getWallBlock();
        }
        return item.getBlock();
    }

    public static BlockState stateFor(Block block, BlockPlaceContext context, ItemStack stack) {
        BlockState state;
        try {
            state = block.getStateForPlacement(context);
        } catch (RuntimeException e) {
            // Some modded blocks throw here
            // e.printStackTrace();
            state = null;
        }
        if (state == null || !state.is(block)) {
            state = fallback(block, context);
        }
        // Stacks picked in build mode (or given with /give) can carry a block_state component
        BlockItemStateProperties stored = stack.get(DataComponents.BLOCK_STATE);
        if (stored != null && !stored.isEmpty()) {
            state = stored.apply(state);
        }
        return state;
    }

    // face is the clicked face (for an empty cell, the side facing the player), look the view direction
    private static BlockState fallback(Block block, BlockPlaceContext context) {
        BlockState state = block.defaultBlockState();
        Direction face = context.getClickedFace();
        Direction look = context.getNearestLookingDirection();
        Direction horizontal = face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite();

        state = with(state, BlockStateProperties.FACING, face);
        state = with(state, BlockStateProperties.HORIZONTAL_FACING, horizontal);
        state = with(state, BlockStateProperties.FACING_HOPPER, look == Direction.UP ? Direction.DOWN : look);
        state = with(state, BlockStateProperties.VERTICAL_DIRECTION, face == Direction.DOWN ? Direction.DOWN : Direction.UP);
        state = with(state, BlockStateProperties.ATTACH_FACE,
                face == Direction.UP ? AttachFace.FLOOR : face == Direction.DOWN ? AttachFace.CEILING : AttachFace.WALL);

        // Multiface blocks (vines, glow lichen, sculk veins) attach to the side the player looks at
        for (Property<?> property : state.getProperties()) {
            if (property instanceof BooleanProperty bool && bool.getName().equals(look.getName())) {
                state = state.setValue(bool, true);
            }
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState with(BlockState state, Property<T> property, T value) {
        if (state.hasProperty(property) && property.getPossibleValues().contains(value)) {
            return state.setValue(property, value);
        }
        return state;
    }

    public static BlockState withValue(BlockState state, String propertyName, String value) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty(propertyName);
        return property == null ? state : parseAndSet(state, property, value);
    }

    private static <T extends Comparable<T>> BlockState parseAndSet(BlockState state, Property<T> property, String value) {
        return property.getValue(value).map(v -> state.setValue(property, v)).orElse(state);
    }
}
