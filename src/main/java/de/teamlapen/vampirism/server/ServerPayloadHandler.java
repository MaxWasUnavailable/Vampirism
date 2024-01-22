package de.teamlapen.vampirism.server;

import de.teamlapen.lib.HelperLib;
import de.teamlapen.lib.lib.inventory.InventoryHelper;
import de.teamlapen.lib.lib.network.ISyncable;
import de.teamlapen.vampirism.api.entity.minion.IMinionTask;
import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.api.entity.player.actions.IAction;
import de.teamlapen.vampirism.api.entity.player.actions.IActionHandler;
import de.teamlapen.vampirism.api.entity.player.skills.ISkill;
import de.teamlapen.vampirism.api.entity.player.skills.ISkillHandler;
import de.teamlapen.vampirism.api.items.IVampirismCrossbow;
import de.teamlapen.vampirism.core.ModItems;
import de.teamlapen.vampirism.data.ServerSkillTreeData;
import de.teamlapen.vampirism.entity.factions.FactionPlayerHandler;
import de.teamlapen.vampirism.entity.minion.MinionEntity;
import de.teamlapen.vampirism.entity.minion.management.MinionData;
import de.teamlapen.vampirism.entity.minion.management.PlayerMinionController;
import de.teamlapen.vampirism.entity.player.TaskManager;
import de.teamlapen.vampirism.entity.player.actions.ActionHandler;
import de.teamlapen.vampirism.entity.player.skills.SkillHandler;
import de.teamlapen.vampirism.entity.player.vampire.VampirePlayer;
import de.teamlapen.vampirism.inventory.HunterBasicMenu;
import de.teamlapen.vampirism.inventory.HunterTrainerMenu;
import de.teamlapen.vampirism.inventory.RevertBackMenu;
import de.teamlapen.vampirism.inventory.VampireBeaconMenu;
import de.teamlapen.vampirism.inventory.diffuser.PlayerOwnedMenu;
import de.teamlapen.vampirism.items.OblivionItem;
import de.teamlapen.vampirism.items.VampirismVampireSwordItem;
import de.teamlapen.vampirism.network.*;
import de.teamlapen.vampirism.network.task.BloodValuesTask;
import de.teamlapen.vampirism.util.RegUtil;
import de.teamlapen.vampirism.world.MinionWorldData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.ConfigurationPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Optional;

import static de.teamlapen.vampirism.network.ServerboundSelectMinionTaskPacket.*;

public class ServerPayloadHandler {

    private static final ServerPayloadHandler INSTANCE = new ServerPayloadHandler();
    private static final Logger LOGGER = LogManager.getLogger();

    public static ServerPayloadHandler getInstance() {
        return INSTANCE;
    }

    public void handleActionBindingPacket(ServerboundActionBindingPacket msg, IPayloadContext context) {
        context.workHandler().execute(() -> FactionPlayerHandler.getOpt(context.player().get()).ifPresent(factionPlayerHandler -> factionPlayerHandler.setBoundAction(msg.actionBindingId(), msg.action(), false, false)));
    }

    public void handleAppearancePacket(ServerboundAppearancePacket msg, IPayloadContext context) {
        context.workHandler().execute(() -> {
            Entity entity = context.level().get().getEntity(msg.entityId());
            if (entity instanceof Player player) {
                VampirePlayer.getOpt(player).ifPresent(vampire -> vampire.setSkinData(msg.data()));
            } else if (entity instanceof MinionEntity<?> minion) {
                minion.getMinionData().ifPresent(minionData -> minionData.handleMinionAppearanceConfig(msg.name(), msg.data()));
                HelperLib.sync(minion);
            }
        });
    }

    public void handleDeleteRefinementPacket(ServerboundDeleteRefinementPacket msg, IPayloadContext context) {
        context.workHandler().execute(() -> {
            Optional<? extends IFactionPlayer<?>> factionPlayerOpt = FactionPlayerHandler.getOpt(context.player().get()).map(FactionPlayerHandler::getCurrentFactionPlayer).orElseGet(Optional::empty);
            factionPlayerOpt.ifPresent(fp -> fp.getSkillHandler().removeRefinementItem(msg.slot()));
        });
    }

    public void handleNameItemPacket(ServerboundNameItemPacket msg, IPayloadContext context) {
        context.workHandler().execute(() -> {
            if (VampirismVampireSwordItem.DO_NOT_NAME_STRING.equals(msg.name())) {
                ItemStack stack = context.player().get().getMainHandItem();
                if (stack.getItem() instanceof VampirismVampireSwordItem) {
                    ((VampirismVampireSwordItem) stack.getItem()).doNotName(stack);
                }
            } else if (!org.apache.commons.lang3.StringUtils.isBlank(msg.name())) {
                ItemStack stack = context.player().get().getMainHandItem();
                stack.setHoverName(Component.literal(msg.name()).withStyle(ChatFormatting.AQUA));
            }
        });
    }

    public void handleSelectAmmoTypePacket(ServerboundSelectAmmoTypePacket msg, IPayloadContext context) {
        context.workHandler().execute(() -> {
            ItemStack stack = context.player().get().getMainHandItem();
            if (stack.getItem() instanceof IVampirismCrossbow crossbow && crossbow.canSelectAmmunition(stack)) {
                crossbow.setAmmunition(stack, msg.ammoId());
            }
        });
    }

    public void handleSelectMinionTaskPacket(ServerboundSelectMinionTaskPacket msg, IPayloadContext context) {
        context.workHandler().execute(() -> {
            FactionPlayerHandler fp = FactionPlayerHandler.getOpt(context.player().get()).get();
            PlayerMinionController controller = MinionWorldData.getData(context.level().get()).get().getOrCreateController(fp);
            if (RECALL.equals(msg.taskID())) {
                if (msg.minionID() < 0) {
                    Collection<Integer> ids = controller.recallMinions(false);
                    for (Integer id : ids) {
                        controller.createMinionEntityAtPlayer(id, context.player().get());
                    }
                    printRecoveringMinions(((ServerPlayer) context.player().get()), controller.getRecoveringMinionNames());

                } else {
                    if (controller.recallMinion(msg.minionID())) {
                        controller.createMinionEntityAtPlayer(msg.minionID(), context.player().get());
                    } else {
                        context.player().get().displayClientMessage(Component.translatable("text.vampirism.minion_is_still_recovering", controller.contactMinionData(msg.minionID(), MinionData::getFormattedName).orElseGet(() -> Component.literal("1"))), true);
                    }
                }
            } else if (RESPAWN.equals(msg.taskID())) {
                Collection<Integer> ids = controller.getUnclaimedMinions();
                for (Integer id : ids) {
                    controller.createMinionEntityAtPlayer(id, context.player().get());
                }
                printRecoveringMinions(((ServerPlayer) context.player().get()), controller.getRecoveringMinionNames());

            } else {
                //noinspection unchecked
                IMinionTask<?, MinionData> task = (IMinionTask<?, MinionData>) RegUtil.getMinionTask(msg.taskID());
                if (task == null) {
                    LOGGER.error("Cannot find action to activate {}", msg.taskID());
                } else if (msg.minionID() < -1) {
                    LOGGER.error("Illegal minion id {}", msg.minionID());
                } else {
                    controller.activateTask(msg.minionID(), task);
                }
            }
        });
    }

    public void handleSetVampireBeaconPacket(ServerboundSetVampireBeaconPacket msg, IPayloadContext context) {
        context.workHandler().execute(() -> {
            if (context.player().get().containerMenu instanceof VampireBeaconMenu beaconMenu && beaconMenu.stillValid(context.player().get())) {
                beaconMenu.updateEffects(msg.effect(), msg.amplifier());
            }
        });
    }

    public void handleSimpleInputEvent(ServerboundSimpleInputEvent msg, IPayloadContext context) {
        context.workHandler().execute(() -> {
            ServerPlayer player = (ServerPlayer) context.player().get();
            Optional<? extends IFactionPlayer<?>> factionPlayerOpt = FactionPlayerHandler.getOpt(player).map(FactionPlayerHandler::getCurrentFactionPlayer).orElseGet(Optional::empty);
            //Try to keep this simple
            switch (msg.type()) {
                case FINISH_SUCK_BLOOD -> VampirePlayer.getOpt(player).ifPresent(vampire -> vampire.endFeeding(true));
                case RESET_SKILLS -> {
                    InventoryHelper.removeItemFromInventory(player.getInventory(), new ItemStack(ModItems.OBLIVION_POTION.get()));
                    factionPlayerOpt.ifPresent(OblivionItem::applyEffect);
                }
                case REVERT_BACK -> {
                    if (player.containerMenu instanceof RevertBackMenu menu) {
                        menu.consume();
                    }
                    FactionPlayerHandler.getOpt(player).ifPresent(handler -> {
                        handler.leaveFaction(!player.server.isHardcore());
                    });
                }
                case TOGGLE_VAMPIRE_VISION -> VampirePlayer.getOpt(player).ifPresent(VampirePlayer::switchVision);
                case TRAINER_LEVELUP -> {
                    if (player.containerMenu instanceof HunterTrainerMenu) {
                        ((HunterTrainerMenu) player.containerMenu).onLevelupClicked();
                    }
                }
                case BASIC_HUNTER_LEVELUP -> {
                    if (player.containerMenu instanceof HunterBasicMenu) {
                        ((HunterBasicMenu) player.containerMenu).onLevelUpClicked();
                    }
                }
                case SHOW_MINION_CALL_SELECTION -> ClientboundRequestMinionSelectPacket.createRequestForPlayer(player, ClientboundRequestMinionSelectPacket.Action.CALL).ifPresent(a -> player.connection.send(a));
                case VAMPIRISM_MENU -> factionPlayerOpt.ifPresent(fPlayer -> fPlayer.getTaskManager().openVampirismMenu());
                case RESURRECT -> VampirePlayer.getOpt(player).ifPresent(VampirePlayer::tryResurrect);
                case GIVE_UP -> VampirePlayer.getOpt(player).ifPresent(VampirePlayer::giveUpDBNO);
            }
        });
    }

    public void handleStartFeedingPacket(ServerboundStartFeedingPacket msg, IPayloadContext context) {
        context.workHandler().execute(() -> {
            VampirePlayer.getOpt(context.player().get()).ifPresent(vampire -> {
                msg.target().ifLeft(vampire::biteEntity);
                msg.target().ifRight(vampire::biteBlock);
            });
        });
    }

    public void handleTaskActionPacket(ServerboundTaskActionPacket msg, IPayloadContext context) {
        context.workHandler().execute(() -> {
            FactionPlayerHandler.getCurrentFactionPlayer(context.player().get()).map(IFactionPlayer::getTaskManager).ifPresent(m -> ((TaskManager) m).handleTaskActionMessage(msg));
        });
    }

    public void handleToggleActionPacket(ServerboundToggleActionPacket msg, IPayloadContext context) {
        context.workHandler().execute(() -> {
            Player player = context.player().get();
            Optional<? extends IFactionPlayer<?>> factionPlayerOpt = FactionPlayerHandler.getOpt(player).map(FactionPlayerHandler::getCurrentFactionPlayer).orElseGet(Optional::empty);
            factionPlayerOpt.ifPresent(factionPlayer -> {
                IAction.ActivationContext activationContext = msg.target() != null ? msg.target().map(entityId -> {
                    Entity e = player.getCommandSenderWorld().getEntity(entityId);
                    if (e == null) {
                        LOGGER.warn("Could not find entity {} the player was looking at when toggling action", entityId);
                    }
                    return new ActionHandler.ActivationContext(e);
                }, ActionHandler.ActivationContext::new) : new ActionHandler.ActivationContext();

                IActionHandler<?> actionHandler = factionPlayer.getActionHandler();
                IAction action = RegUtil.getAction(msg.actionId());
                if (action != null) {
                    IAction.PERM r = actionHandler.toggleAction(action, activationContext);
                    switch (r) {
                        case NOT_UNLOCKED -> player.displayClientMessage(Component.translatable("text.vampirism.action.not_unlocked"), true);
                        case DISABLED -> player.displayClientMessage(Component.translatable("text.vampirism.action.deactivated_by_serveradmin"), false);
                        case COOLDOWN -> player.displayClientMessage(Component.translatable("text.vampirism.action.cooldown_not_over"), true);
                        case DISALLOWED -> player.displayClientMessage(Component.translatable("text.vampirism.action.disallowed"), true);
                        case PERMISSION_DISALLOWED -> player.displayClientMessage(Component.translatable("text.vampirism.action.permission_disallowed"), false);
                        default -> {
                            //Everything alright
                        }
                    }
                } else {
                    LOGGER.error("Failed to find action with id {}", msg.actionId());
                }
            });
        });
    }

    public void handleToggleMinionTaskLock(ServerboundToggleMinionTaskLock msg, IPayloadContext context) {
        context.workHandler().execute(() -> {
            FactionPlayerHandler.getOpt(context.player().get()).ifPresent(fp -> {
                PlayerMinionController controller = MinionWorldData.getData(context.level().get()).get().getOrCreateController(fp);
                controller.contactMinionData(msg.minionID(), data -> data.setTaskLocked(!data.isTaskLocked()));
                controller.contactMinion(msg.minionID(), MinionEntity::onTaskChanged);

            });
        });
    }

    public void handleUnlockSkillPacket(ServerboundUnlockSkillPacket msg, IPayloadContext context) {
        context.workHandler().execute(() -> {
        Player player = context.player().get();
        Optional<? extends IFactionPlayer<?>> factionPlayerOpt = FactionPlayerHandler.getOpt(player).map(FactionPlayerHandler::getCurrentFactionPlayer).orElseGet(Optional::empty);
        factionPlayerOpt.ifPresent(factionPlayer -> {
            ISkill skill = RegUtil.getSkill(msg.skillId());
            if (skill != null) {
                ISkillHandler<?> skillHandler = factionPlayer.getSkillHandler();
                ISkillHandler.Result result = skillHandler.canSkillBeEnabled(skill);
                if (result == ISkillHandler.Result.OK) {
                    skillHandler.enableSkill(skill);
                    if (factionPlayer instanceof ISyncable.ISyncableAttachment && skillHandler instanceof SkillHandler) {
                        //does this cause problems with addons?
                        CompoundTag sync = new CompoundTag();
                        CompoundTag tag = new CompoundTag();
                        ((SkillHandler<?>) skillHandler).writeUpdateForClient(tag);
                        sync.put("skill_handler", tag);
                        HelperLib.sync((ISyncable.ISyncableAttachment) factionPlayer, sync, factionPlayer.getRepresentingPlayer(), false);
                    }

                } else {
                    LOGGER.warn("Skill {} cannot be activated for {} ({})", skill, player, result);
                }
            } else {
                LOGGER.warn("Skill {} was not found so {} cannot activate it", msg.skillId(), player);
            }
        });
        });
    }

    public void handleUpgradeMinionStatPacket(ServerboundUpgradeMinionStatPacket msg, IPayloadContext context) {
        context.workHandler().execute(() -> {
            Player player = context.player().get();
            if (player != null) {
                Entity entity = player.level().getEntity(msg.entityId());
                if (entity instanceof MinionEntity) {
                    if (((MinionEntity<?>) entity).getMinionData().map(d -> d.upgradeStat(msg.statId(), (MinionEntity<?>) entity)).orElse(false)) {
                        HelperLib.sync((MinionEntity<?>) entity);
                    }
                }
            }
        });
    }

    public void handleRequestSkillTreePacket(ServerboundRequestSkillTreePacket msg, PlayPayloadContext context) {
        context.replyHandler().send(ClientboundSkillTreePacket.of(ServerSkillTreeData.instance().getConfigurations()));
    }

    public void handleBloodValuesCompletedPacket(CustomPacketPayload packetPayload, ConfigurationPayloadContext configurationPayloadContext) {
        configurationPayloadContext.taskCompletedHandler().onTaskCompleted(BloodValuesTask.TYPE);
    }

    public void handlePlayerOwnedBlockEntityLockPacket(PlayerOwnedBlockEntityLockPacket msg, PlayPayloadContext context) {
        context.player().ifPresent(player -> {
            if (player.containerMenu instanceof PlayerOwnedMenu menu && player.containerMenu.containerId == msg.menuId() && menu.isOwner(player)) {
                menu.updateLockStatus(msg.lockData().getLockStatus());
                context.replyHandler().send(menu.updatePackage());
            }
        });
    }
}
