package de.teamlapen.vampirism.entity.player.hunter;

import de.teamlapen.lib.HelperLib;
import de.teamlapen.vampirism.REFERENCE;
import de.teamlapen.vampirism.VampirismMod;
import de.teamlapen.vampirism.advancements.critereon.VampireActionCriterionTrigger;
import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.VampirismAttachments;
import de.teamlapen.vampirism.api.entity.effect.EffectInstanceWithSource;
import de.teamlapen.vampirism.api.entity.factions.IDisguise;
import de.teamlapen.vampirism.api.entity.factions.IFaction;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import de.teamlapen.vampirism.api.entity.player.actions.IActionHandler;
import de.teamlapen.vampirism.api.entity.player.hunter.IHunterPlayer;
import de.teamlapen.vampirism.api.entity.player.skills.ISkillHandler;
import de.teamlapen.vampirism.config.VampirismConfig;
import de.teamlapen.vampirism.core.ModAdvancements;
import de.teamlapen.vampirism.core.ModAttachments;
import de.teamlapen.vampirism.core.ModEffects;
import de.teamlapen.vampirism.core.ModTags;
import de.teamlapen.vampirism.entity.factions.FactionPlayerHandler;
import de.teamlapen.vampirism.entity.minion.HunterMinionEntity;
import de.teamlapen.vampirism.entity.player.FactionBasePlayer;
import de.teamlapen.vampirism.entity.player.IVampirismPlayer;
import de.teamlapen.vampirism.entity.player.LevelAttributeModifier;
import de.teamlapen.vampirism.entity.player.VampirismPlayerAttributes;
import de.teamlapen.vampirism.entity.player.actions.ActionHandler;
import de.teamlapen.vampirism.entity.player.hunter.actions.HunterActions;
import de.teamlapen.vampirism.entity.player.hunter.skills.HunterSkills;
import de.teamlapen.vampirism.entity.player.skills.SkillHandler;
import de.teamlapen.vampirism.items.HunterCoatItem;
import de.teamlapen.vampirism.util.Helper;
import de.teamlapen.vampirism.util.OilUtils;
import de.teamlapen.vampirism.util.ScoreboardUtil;
import de.teamlapen.vampirism.world.MinionWorldData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.event.TickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

/**
 * Main class for hunter players
 */
public class HunterPlayer extends FactionBasePlayer<IHunterPlayer> implements IHunterPlayer {

    public static @NotNull HunterPlayer get(@NotNull Player player) {
        return player.getData(ModAttachments.HUNTER_PLAYER);
    }

    public static @NotNull Optional<HunterPlayer> getOpt(@NotNull Player player) {
        return Optional.ofNullable(player.getData(ModAttachments.HUNTER_PLAYER));
    }

    private final @NotNull ActionHandler<IHunterPlayer> actionHandler;
    private final @NotNull SkillHandler<IHunterPlayer> skillHandler;
    private final @NotNull Disguise disguise;

    public HunterPlayer(Player player) {
        super(player);
        actionHandler = new ActionHandler<>(this);
        skillHandler = new SkillHandler<>(this, VReference.HUNTER_FACTION);
        this.disguise = new Disguise();
    }

    @Override
    public void breakDisguise() {
    }

    @Override
    public boolean canLeaveFaction() {
        return true;
    }

    @Override
    public IActionHandler<IHunterPlayer> getActionHandler() {
        return actionHandler;
    }

    @Override
    public @NotNull ResourceLocation getAttachmentKey() {
        return VampirismAttachments.Keys.HUNTER_PLAYER;
    }

    @Override
    public IDisguise getDisguise() {
        return this.disguise;
    }

    @Override
    public int getLevel() {
        return ((IVampirismPlayer) player).getVampAtts().hunterLevel;
    }

    @Override
    public int getMaxLevel() {
        return REFERENCE.HIGHEST_HUNTER_LEVEL;
    }

    @Override
    public Predicate<LivingEntity> getNonFriendlySelector(boolean otherFactionPlayers, boolean ignoreDisguise) {
        if (otherFactionPlayers) {
            return entity -> true;
        } else {
            return VampirismAPI.factionRegistry().getPredicate(getFaction(), ignoreDisguise);
        }
    }

    @NotNull
    @Override
    public ISkillHandler<IHunterPlayer> getSkillHandler() {
        return skillHandler;
    }

    /**
     * You can use {@link VampirismPlayerAttributes#getHuntSpecial()} instead if you don't have the hunter player already
     */
    @NotNull
    public HunterPlayerSpecialAttribute getSpecialAttributes() {
        return ((IVampirismPlayer) player).getVampAtts().getHuntSpecial();
    }

    @Override
    public int getTheEntityID() {
        return player.getId();
    }

    @Override
    public boolean isDisguised() {
        return player.hasEffect(ModEffects.DISGUISE_AS_VAMPIRE.get());
    }

    @Override
    public void onChangedDimension(ResourceKey<Level> from, ResourceKey<Level> to) {

    }

    @Override
    public void onDeath(@NotNull DamageSource src) {
        super.onDeath(src);
        actionHandler.deactivateAllActions();
        if (src.getEntity() instanceof ServerPlayer && Helper.isVampire(((Player) src.getEntity())) && this.getRepresentingPlayer().getEffect(ModEffects.FREEZE.get()) != null) {
            ModAdvancements.TRIGGER_VAMPIRE_ACTION.get().trigger(((ServerPlayer) src.getEntity()), VampireActionCriterionTrigger.Action.KILL_FROZEN_HUNTER);
        }
    }

    @Override
    public boolean onEntityAttacked(DamageSource src, float amt) {
        return false;
    }

    @Override
    public void onJoinWorld() {
        if (getLevel() > 0) {
            actionHandler.onActionsReactivated();
        }
    }

    @Override
    public void onLevelChanged(int level, int oldLevel) {
        super.onLevelChanged(level, oldLevel);
        if (!isRemote()) {
            ScoreboardUtil.updateScoreboard(player, ScoreboardUtil.HUNTER_LEVEL_CRITERIA, level);
            LevelAttributeModifier.applyModifier(player, Attributes.ATTACK_DAMAGE, "Hunter", level, getMaxLevel(), VampirismConfig.BALANCE.hpStrengthMaxMod.get(), VampirismConfig.BALANCE.hpStrengthType.get(), AttributeModifier.Operation.MULTIPLY_BASE, false);
            if (level == 0) {
                EffectInstanceWithSource.removePotionEffect(player, MobEffects.JUMP, HunterSkills.ARMOR_JUMP.getId());
                EffectInstanceWithSource.removePotionEffect(player, MobEffects.MOVEMENT_SPEED, HunterSkills.ARMOR_SPEED.getId());
            }
        }
    }

    @Override
    public void onPlayerLoggedIn() {

    }

    @Override
    public void onPlayerLoggedOut() {

    }

    @Override
    public void onUpdate() {
        player.getCommandSenderWorld().getProfiler().push("vampirism_hunterPlayer");
        super.onUpdate();
        int level = getLevel();
        if (level > 0) {
            if (!isRemote()) {
                if (player.level().getGameTime() % 100 == 8) {
                    if (StreamSupport.stream(player.getArmorSlots().spliterator(), false).allMatch(i -> i.is(ModTags.Items.HUNTER_ARMOR))) {
                        if (skillHandler.isSkillEnabled(HunterSkills.ARMOR_JUMP)) {
                            MobEffectInstance mobEffectInstance = new MobEffectInstance(MobEffects.JUMP, -1, 0, false, false);
                            ((EffectInstanceWithSource) mobEffectInstance).setSource(HunterSkills.ARMOR_JUMP.getId());
                            player.addEffect(mobEffectInstance);
                        }
                        if (skillHandler.isSkillEnabled(HunterSkills.ARMOR_SPEED)) {
                            MobEffectInstance mobEffectInstance = new MobEffectInstance(MobEffects.MOVEMENT_SPEED, -1, 0, false, false);
                            ((EffectInstanceWithSource) mobEffectInstance).setSource(HunterSkills.ARMOR_SPEED.getId());
                            player.addEffect(mobEffectInstance);
                        }
                    } else {
                        EffectInstanceWithSource.removePotionEffect(player, MobEffects.JUMP, HunterSkills.ARMOR_JUMP.getId());
                        EffectInstanceWithSource.removePotionEffect(player, MobEffects.MOVEMENT_SPEED, HunterSkills.ARMOR_SPEED.getId());
                    }
                }
                boolean sync = false;
                boolean syncToAll = false;
                CompoundTag syncPacket = new CompoundTag();
                if (actionHandler.updateActions()) {
                    sync = true;
                    syncToAll = true;
                    CompoundTag tag = new CompoundTag();
                    actionHandler.writeUpdateForClient(tag);
                    syncPacket.put("action_handler", tag);
                }
                if (skillHandler.isDirty()) {
                    sync = true;
                    CompoundTag tag = new CompoundTag();
                    skillHandler.writeUpdateForClient(tag);
                    syncPacket.put("skill_handler", tag);
                }
                if (sync) {
                    sync(syncPacket, syncToAll);
                }
            } else {
                if (getSpecialAttributes().blessingSoundReference != null && !player.isUsingItem()) {
                    //Make sure the blessing sound is stopped when player is not using {@link BlessableItem}. This is necessary because onReleaseUsing is not called for other client players.
                    getSpecialAttributes().blessingSoundReference.stopPlaying();
                    getSpecialAttributes().blessingSoundReference = null;
                }
                actionHandler.updateActions();
                VampirismMod.proxy.handleSleepClient(player);

            }
        } else {
            if (this.player.level().getGameTime() % 100 == 16) {
                if (OilUtils.getEquippedArmorOils(this.player).size() > 0) {
                    this.player.addEffect(new MobEffectInstance(ModEffects.POISON.get(), 120, 0, false, false));
                }
            }
        }
        getSpecialAttributes().fullHunterCoat = level > 0 ? HunterCoatItem.isFullyEquipped(player) : null;

        player.getCommandSenderWorld().getProfiler().pop();
    }

    @Override
    public void onUpdatePlayer(TickEvent.Phase phase) {

    }

    @Override
    public void loadUpdateFromNBT(CompoundTag nbt) {
        super.loadUpdateFromNBT(nbt);
        actionHandler.readUpdateFromServer(nbt.getCompound("action_handler"));
        skillHandler.readUpdateFromServer(nbt.getCompound("skill_handler"));
    }

    @Override
    public void loadFromNBT(CompoundTag nbt) {
        super.loadFromNBT(nbt);
        actionHandler.readUpdateFromServer(nbt.getCompound("action_handler"));
        skillHandler.readUpdateFromServer(nbt.getCompound("skill_handler"));
    }

    @Override
    public CompoundTag writeFullUpdateToNBT() {
        var tag = super.writeFullUpdateToNBT();
        CompoundTag actionHandler = new CompoundTag();
        CompoundTag skillHandler = new CompoundTag();
        this.actionHandler.writeUpdateForClient(actionHandler);
        this.skillHandler.writeUpdateForClient(skillHandler);
        tag.put("action_handler", actionHandler);
        tag.put("skill_handler", skillHandler);
        return tag;
    }

    @Override
    public CompoundTag writeToNBT() {
        var tag = super.writeToNBT();
        CompoundTag actionHandler = new CompoundTag();
        CompoundTag skillHandler = new CompoundTag();
        this.actionHandler.writeUpdateForClient(actionHandler);
        this.skillHandler.writeUpdateForClient(skillHandler);
        tag.put("action_handler", actionHandler);
        tag.put("skill_handler", skillHandler);
        return tag;
    }

    @Override
    public void updateMinionAttributes(boolean increasedStats) {
        MinionWorldData.getData(this.player.level()).flatMap(a -> FactionPlayerHandler.getOpt(this.player).map(a::getOrCreateController)).ifPresent(controller -> controller.contactMinions((minion) -> {
            (minion.getMinionData()).ifPresent(b -> ((HunterMinionEntity.HunterMinionData) b).setIncreasedStats(increasedStats));
            HelperLib.sync(minion);
        }));
    }

    public class Disguise implements IDisguise {

        @Override
        public @NotNull IPlayableFaction<?> getOriginalFaction() {
            return getFaction();
        }

        @Override
        public @Nullable IPlayableFaction<?> getViewedFaction(@Nullable IFaction<?> viewerFaction) {
            return player.hasEffect(ModEffects.DISGUISE_AS_VAMPIRE.get()) ? VReference.VAMPIRE_FACTION : getOriginalFaction();
        }

        @Override
        public void disguiseAs(@Nullable IPlayableFaction<?> faction) {

        }

        @Override
        public void unDisguise() {

        }

        @Override
        public boolean isDisguised() {
            return false;
        }
    }

    public static class Serializer implements IAttachmentSerializer<CompoundTag, HunterPlayer> {

        @Override
        public HunterPlayer read(IAttachmentHolder holder, CompoundTag tag) {
            if(holder instanceof Player player) {
                var hunter = new HunterPlayer(player);
                hunter.loadFromNBT(tag);
                return hunter;
            }
            throw new IllegalArgumentException("Expected Player, got " + holder.getClass().getSimpleName());
        }

        @Override
        public CompoundTag write(HunterPlayer attachment) {
            return attachment.writeToNBT();
        }
    }

    public static class Factory implements Function<IAttachmentHolder, HunterPlayer> {

        @Override
        public HunterPlayer apply(IAttachmentHolder holder) {
            if (holder instanceof Player player) {
                return new HunterPlayer(player);
            }
            throw new IllegalArgumentException("Cannot create hunter player attachment for holder " + holder.getClass() + ". Expected Player");
        }
    }
}
