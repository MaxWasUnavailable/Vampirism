package de.teamlapen.vampirism.api.entity.player.skills;

import de.teamlapen.vampirism.api.VampirismRegistries;
import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.api.entity.player.actions.IAction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.*;
import java.util.function.Supplier;

/**
 * Default implementation of ISkill. Handles entity modifiers and actions
 */
public abstract class DefaultSkill<T extends IFactionPlayer<T>> implements ISkill<T> {

    private final Map<Attribute, AttributeHolder> attributeModifierMap = new HashMap<>();
    @Range(from = 0, to = 9)
    private final int skillPointCost;
    private Component name;

    public DefaultSkill() {
        this(1);
    }

    public DefaultSkill(@Range(from = 0, to = 9) int skillPointCost) {
        this.skillPointCost = skillPointCost;
    }

    @Override
    public Component getName() {
        return name == null ? name = Component.translatable(getTranslationKey()) : name;
    }

    public @NotNull DefaultSkill<T> setName(Component name) {
        this.name = name;
        return this;
    }


    @Deprecated
    @Override
    public String getTranslationKey() {
        return "skill." + getRegistryName().getNamespace() + "." + getRegistryName().getPath();
    }

    @Override
    public final void onDisable(@NotNull T player) {
        removeAttributesModifiersFromEntity(player.getRepresentingPlayer());
        player.getActionHandler().relockActions(getActions());
        if (this.getFaction().map(f -> f.getFactionPlayerInterface().isInstance(player)).orElse(true)) {
            onDisabled(player);
        } else {
            throw new IllegalArgumentException("Faction player instance is of wrong class " + player.getClass() + " instead of " + this.getFaction().get().getFactionPlayerInterface());
        }
    }

    @Override
    public final void onEnable(@NotNull T player) {
        applyAttributesModifiersToEntity(player.getRepresentingPlayer());

        player.getActionHandler().unlockActions(getActions());
        if (this.getFaction().map(f -> f.getFactionPlayerInterface().isInstance(player)).orElse(true)) {
            onEnabled(player);
        } else {
            throw new IllegalArgumentException("Faction player instance is of wrong class " + player.getClass() + " instead of " + this.getFaction().get().getFactionPlayerInterface());
        }
    }


    public @NotNull DefaultSkill<T> registerAttributeModifier(Attribute attribute, @NotNull String uuid, double amount, AttributeModifier.@NotNull Operation operation) {
        this.attributeModifierMap.put(attribute, new AttributeHolder(attribute, UUID.fromString(uuid), () -> amount, operation));
        return this;
    }

    public @NotNull DefaultSkill<T> registerAttributeModifier(Attribute attribute, @NotNull String uuid, @NotNull Supplier<Double> amountSupplier, AttributeModifier.@NotNull Operation operation) {
        this.attributeModifierMap.put(attribute, new AttributeHolder(attribute, UUID.fromString(uuid), amountSupplier, operation));
        return this;
    }

    @Override
    public @NotNull String toString() {
        return getRegistryName() + "(" + getClass().getSimpleName() + ")";
    }

    /**
     * Add actions that should be added to the list
     */
    protected void getActions(Collection<IAction<T>> list) {

    }

    /**
     * Called when the skill is being disabled.
     */
    protected void onDisabled(T player) {
    }

    /**
     * Called when the skill is being enabled
     */
    protected void onEnabled(T player) {
    }

    private void applyAttributesModifiersToEntity(@NotNull Player player) {
        for (Map.Entry<Attribute, AttributeHolder> entry : this.attributeModifierMap.entrySet()) {
            AttributeInstance instance = player.getAttribute(entry.getKey());

            if (instance != null) {
                instance.removeModifier(entry.getValue().uuid);
                instance.addPermanentModifier(entry.getValue().create());
            }
        }
    }

    private @NotNull Collection<IAction<T>> getActions() {
        Collection<IAction<T>> collection = new ArrayList<>();
        getActions(collection);
        collection.forEach((iAction -> {
            if (iAction.getFaction().isPresent() && iAction.getFaction().get() != this.getFaction().orElse(null)) {
                throw new IllegalArgumentException("Can't register action of faction " + iAction.getFaction().map(Object::toString).orElse(null) + " for skill of faction" + this.getFaction().map(Object::toString).orElse("all"));
            }
        }));
        return collection;
    }

    private void removeAttributesModifiersFromEntity(@NotNull Player player) {
        for (Map.Entry<Attribute, AttributeHolder> entry : this.attributeModifierMap.entrySet()) {
            AttributeInstance attribute = player.getAttribute(entry.getKey());

            if (attribute != null) {
                attribute.removeModifier(entry.getValue().uuid);
            }
        }
    }

    private @Nullable ResourceLocation getRegistryName() {
        return VampirismRegistries.SKILLS.get().getKey(this);
    }

    @Range(from = 0, to = 9)
    @Override
    public int getSkillPointCost() {
        return this.skillPointCost;
    }

    protected class AttributeHolder {
        public final Attribute attribute;
        public final @NotNull UUID uuid;
        public final @NotNull Supplier<Double> amountSupplier;
        public final AttributeModifier.@NotNull Operation operation;

        private AttributeHolder(Attribute attribute, @NotNull UUID uuid, @NotNull Supplier<Double> amountSupplier, AttributeModifier.@NotNull Operation operation) {
            this.attribute = attribute;
            this.uuid = uuid;
            this.amountSupplier = amountSupplier;
            this.operation = operation;
        }

        public AttributeModifier create() {
            return new AttributeModifier(uuid, DefaultSkill.this.getRegistryName().toString(), amountSupplier.get(), operation);
        }
    }
}
