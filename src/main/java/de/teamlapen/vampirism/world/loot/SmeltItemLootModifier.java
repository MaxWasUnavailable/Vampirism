package de.teamlapen.vampirism.world.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.teamlapen.vampirism.core.ModLoot;
import de.teamlapen.vampirism.core.ModOils;
import de.teamlapen.vampirism.util.OilUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collector;

public class SmeltItemLootModifier extends LootModifier {

    public static final Codec<SmeltItemLootModifier> CODEC = RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, SmeltItemLootModifier::new));

    /**
     * Constructs a LootModifier.
     *
     * @param conditionsIn the ILootConditions that need to be matched before the loot is modified.
     */
    protected SmeltItemLootModifier(LootItemCondition @NotNull [] conditionsIn) {
        super(conditionsIn);
    }

    @NotNull
    @Override
    protected ObjectArrayList<ItemStack> doApply(@NotNull ObjectArrayList<ItemStack> generatedLoot, @NotNull LootContext context) {
        ItemStack stack = context.getParamOrNull(LootContextParams.TOOL);
        Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (!(entity instanceof LivingEntity) || stack == null || OilUtils.getAppliedOil(stack).filter(oil -> oil == ModOils.SMELT.get()).isEmpty()) {
            return generatedLoot;
        }
        stack = ((LivingEntity) entity).getMainHandItem();
        OilUtils.reduceAppliedOilDuration(stack);
        return trySmelting(generatedLoot, context.getLevel());
    }

    private ObjectArrayList<ItemStack> trySmelting(@NotNull ObjectArrayList<ItemStack> generatedLoot, @NotNull ServerLevel level) {
        RecipeManager recipeManager = level.getRecipeManager();
        return generatedLoot.stream().map(stack -> recipeManager.getRecipeFor(RecipeType.SMELTING, new SimpleContainer(stack), level).map(recipe -> recipe.value().getResultItem(level.registryAccess())).filter(result -> !result.isEmpty()).orElse(stack)).collect(Collector.of(ObjectArrayList::new, ObjectArrayList::add, (left, right) -> {
            left.addAll(right);
            return left;
        }));
    }

    @Override
    public @NotNull Codec<? extends IGlobalLootModifier> codec() {
        return ModLoot.SMELTING.get();
    }
}
