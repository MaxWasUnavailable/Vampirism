package de.teamlapen.vampirism.recipes;

import de.teamlapen.vampirism.core.ModRecipes;
import de.teamlapen.vampirism.util.OilUtils;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class CleanOilRecipe extends CustomRecipe {

    public CleanOilRecipe(@NotNull CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingContainer inventory, @NotNull Level level) {
        ItemStack tool = null;
        ItemStack paper = null;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() == Items.PAPER) {
                    if (paper == null) {
                        paper = stack;
                    }
                } else if(OilUtils.hasAppliedOil(stack)) {
                    if (tool != null) return false;
                    tool = stack;
                } else {
                    return false;
                }
            }
        }
        return tool != null && paper != null;
    }

    @Override
    public @NotNull ItemStack assemble(CraftingContainer inventory, @NotNull RegistryAccess registryAccess) {
        ItemStack tool = ItemStack.EMPTY;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && OilUtils.hasAppliedOil(stack)) {
                tool = stack;
                break;
            }
        }
        ItemStack result = tool.copy();
        OilUtils.removeAppliedOil(result);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int x, int y) {
        return x * y >= 2;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.CLEAN_OIL.get();
    }
}
