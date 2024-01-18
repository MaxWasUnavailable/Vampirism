package de.teamlapen.vampirism.recipes;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import de.teamlapen.vampirism.api.items.IItemWithTier;
import de.teamlapen.vampirism.core.ModRecipes;
import de.teamlapen.vampirism.mixin.ShapedRecipeAccessor;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.jetbrains.annotations.NotNull;

/**
 * This recipe copies the {@link net.minecraft.nbt.CompoundTag} from the first found {@link IItemWithTier} and inserts it into the manufacturing result with damage = 0
 *
 * @author Cheaterpaul
 */
public class ShapedItemWithTierRepair extends ShapedRecipe {

    public ShapedItemWithTierRepair(@NotNull ShapedRecipe shaped) {
        super(shaped.getGroup(), CraftingBookCategory.EQUIPMENT, ((ShapedRecipeAccessor) shaped).getPattern(), ((ShapedRecipeAccessor) shaped).getResult(), shaped.showNotification());
    }

    @NotNull
    @Override
    public ItemStack assemble(@NotNull CraftingContainer inv, @NotNull RegistryAccess registryAccess) {
        ItemStack stack = null;
        search:
        for (int i = 0; i <= inv.getWidth(); ++i) {
            for (int j = 0; j <= inv.getHeight(); ++j) {
                if (inv.getItem(i + j * inv.getWidth()).getItem() instanceof IItemWithTier) {
                    stack = inv.getItem(i + j * inv.getWidth());
                    break search;
                }
            }
        }
        ItemStack result = super.assemble(inv, registryAccess);
        if (stack != null) {
            result.setTag(stack.getTag());
            result.setDamageValue(0);
        }
        return result;
    }

    @NotNull
    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.REPAIR_IITEMWITHTIER.get();
    }

    public static class Serializer extends ShapedRecipe.Serializer {

        public static final Codec<ShapedRecipe> CODEC = ShapedRecipe.Serializer.CODEC.xmap(ShapedItemWithTierRepair::new, ShapedItemWithTierRepair::new);

        @Override
        public @NotNull Codec<ShapedRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull ShapedRecipe fromNetwork(FriendlyByteBuf p_44240_) {
            return p_44240_.readJsonWithCodec(CODEC);
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buffer, @NotNull ShapedRecipe recipe) {
            buffer.writeJsonWithCodec(CODEC, recipe);
        }
    }
}