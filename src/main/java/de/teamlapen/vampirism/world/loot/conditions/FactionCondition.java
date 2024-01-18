package de.teamlapen.vampirism.world.loot.conditions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFaction;
import de.teamlapen.vampirism.core.ModLoot;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class FactionCondition implements LootItemCondition {

    public static final Codec<FactionCondition> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            StringRepresentable.fromEnum(Type::values).fieldOf("type").forGetter(s -> s.type),
            ExtraCodecs.strictOptionalField(IFaction.CODEC, "faction").forGetter(a -> a.faction),
            ExtraCodecs.strictOptionalField(Codec.INT, "min_level").forGetter(a -> a.minLevel),
            ExtraCodecs.strictOptionalField(Codec.INT, "max_level").forGetter(a -> a.maxLevel)
    ).apply(inst, FactionCondition::new));

    private final @NotNull Type type;
    private final Optional<IFaction<?>> faction;
    private final Optional<Integer> minLevel;
    private final Optional<Integer> maxLevel;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private FactionCondition(@NotNull Type type, Optional<IFaction<?>> faction, Optional<Integer> minLevel, Optional<Integer> maxLevel) {
        this.type = type;
        this.faction = faction;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    public FactionCondition(@NotNull IFaction<?> faction, int minLevel, int maxLevel) {
        this.type = Type.FACTION;
        this.faction = Optional.of(faction);
        this.minLevel = Optional.of(minLevel);
        this.maxLevel = Optional.of(maxLevel);
    }

    public FactionCondition(int minLevel, int maxLevel) {
        this.type = Type.ANY_FACTION;
        this.faction = Optional.empty();
        this.minLevel = Optional.of(minLevel);
        this.maxLevel = Optional.of(maxLevel);
    }

    public FactionCondition(int minLevel) {
        this.type = Type.ANY_FACTION;
        this.faction = Optional.empty();
        this.minLevel = Optional.of(minLevel);
        this.maxLevel = Optional.empty();
    }

    public FactionCondition() {
        this.type = Type.NO_FACTION;
        this.faction = Optional.empty();
        this.minLevel = Optional.empty();
        this.maxLevel = Optional.empty();
    }

    @Override
    public @NotNull LootItemConditionType getType() {
        return ModLoot.FACTION.get();
    }

    @Override
    public boolean test(LootContext lootContext) {
        Entity entity = lootContext.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (entity instanceof Player player) {
            return VampirismAPI.getFactionPlayerHandler(player).map(handler -> switch (this.type) {
                case NO_FACTION -> handler.getCurrentFactionPlayer().isEmpty();
                case ANY_FACTION -> !this.minLevel.map(minLevel -> handler.getCurrentLevel() < minLevel).orElse(false) && !this.maxLevel.map(maxLevel -> handler.getCurrentLevel() > maxLevel).orElse(false);
                case FACTION ->
                        handler.isInFaction(this.faction.orElseThrow()) && !this.minLevel.map(minLevel -> handler.getCurrentLevel() < minLevel).orElse(false) && !this.maxLevel.map(maxLevel -> handler.getCurrentLevel() > maxLevel).orElse(false);
            }).orElse(false);
        }
        return false;
    }

    public enum Type implements StringRepresentable {
        NO_FACTION("no_faction"),
        ANY_FACTION("any_faction"),
        FACTION("faction");

        private final String name;

        Type(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.name;
        }
    }
}
