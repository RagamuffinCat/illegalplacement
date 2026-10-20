package io.github.ragamuffincat.illegalplacement.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;

// Creative pick block done on the client
final class Picking {

    private Picking() {
    }

    static ItemStack cloneStack(LevelReader level, BlockPos pos, BlockState state) {
        return state.getCloneItemStack(level, pos, false);
    }

    // Same as vanilla's Ctrl + middle click
    static void copyBlockEntity(ItemStack stack, BlockEntity blockEntity, RegistryAccess registries) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        blockEntity.saveCustomOnly(output);
        blockEntity.removeComponentsFromTag(output);
        BlockItem.setBlockEntityData(stack, blockEntity.getType(), output);
        stack.applyComponents(blockEntity.collectComponents());
    }

    // Puts the stack in the hotbar the way vanilla pick block does (reusing a slot that already holds it) and syncs
    // the slot to the server
    static void give(Minecraft mc, ItemStack stack) {
        Inventory inventory = mc.player.getInventory();
        int slot = inventory.findSlotMatchingItem(stack);
        if (Inventory.isHotbarSlot(slot)) {
            inventory.setSelectedSlot(slot);
        } else if (slot != -1) {
            inventory.pickSlot(slot);
        } else {
            inventory.addAndPickItem(stack);
        }
        mc.gameMode.handleCreativeModeItemAdd(inventory.getSelectedItem(), 36 + inventory.getSelectedSlot());
    }
}
