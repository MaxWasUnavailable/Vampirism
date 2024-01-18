package de.teamlapen.vampirism.command.test;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import de.teamlapen.lib.lib.util.BasicCommand;
import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFactionRegistry;
import de.teamlapen.vampirism.api.entity.factions.IMinionBuilder;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFactionBuilder;
import de.teamlapen.vampirism.api.entity.hunter.IBasicHunter;
import de.teamlapen.vampirism.api.entity.minion.IMinionData;
import de.teamlapen.vampirism.api.entity.minion.IMinionEntity;
import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.api.entity.vampire.IBasicVampire;
import de.teamlapen.vampirism.core.ModEntities;
import de.teamlapen.vampirism.entity.factions.FactionPlayerHandler;
import de.teamlapen.vampirism.entity.minion.HunterMinionEntity;
import de.teamlapen.vampirism.entity.minion.MinionEntity;
import de.teamlapen.vampirism.entity.minion.VampireMinionEntity;
import de.teamlapen.vampirism.entity.minion.management.MinionData;
import de.teamlapen.vampirism.entity.minion.management.PlayerMinionController;
import de.teamlapen.vampirism.entity.player.hunter.HunterPlayer;
import de.teamlapen.vampirism.entity.player.hunter.skills.HunterSkills;
import de.teamlapen.vampirism.entity.player.vampire.VampirePlayer;
import de.teamlapen.vampirism.entity.player.vampire.skills.VampireSkills;
import de.teamlapen.vampirism.world.MinionWorldData;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;


public class MinionCommand extends BasicCommand {
    private static final DynamicCommandExceptionType fail = new DynamicCommandExceptionType((msg) -> Component.literal("Failed: " + msg));

    public static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("minion")
                .requires(context -> context.hasPermission(PERMISSION_LEVEL_CHEAT))
                .then(registerNew())
                .then(Commands.literal("recall")
                        .executes(context -> recall(context.getSource(), context.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> recall(context.getSource(), EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("respawnAll")
                        .executes(context -> respawn(context.getSource(), context.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> respawn(context.getSource(), EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("purge")
                        .executes(context -> purge(context.getSource(), context.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> purge(context.getSource(), EntityArgument.getPlayer(context, "target")))));
    }

    @SuppressWarnings("unchecked")
    public static ArgumentBuilder<CommandSourceStack, ?> registerNew() {
        LiteralArgumentBuilder<CommandSourceStack> spawnNew = Commands.literal("spawnNew");
        Collection<IFactionRegistry.IMinionEntry<?,?>> minions = VampirismAPI.factionRegistry().getMinions();
        for (IFactionRegistry.IMinionEntry<?,?> minion : minions) {
            if (minion.type() == null){
                continue;
            }
            ArgumentBuilder<CommandSourceStack, ?> currentCommand = null;
            List<? extends IMinionBuilder.IMinionCommandBuilder.ICommandEntry<?, ?>> iCommandEntries = minion.commandArguments();
            for (int i = iCommandEntries.size() - 1; i >= 0; i--) {
                IMinionBuilder.IMinionCommandBuilder.ICommandEntry<?, ?> iCommandEntry = iCommandEntries.get(i);
                int finalI = i;
                var builder  = Commands.argument(iCommandEntry.name(), iCommandEntry.type()).executes(context -> spawnNewMinionExtra(context, context.getSource(), minion.faction(), (Supplier<MinionData>) minion.data(), minion.type(),  (Collection<IMinionBuilder.IMinionCommandBuilder.ICommandEntry<MinionData,?>>) iCommandEntries.subList(0, finalI+1), (Collection<IMinionBuilder.IMinionCommandBuilder.ICommandEntry<MinionData,?>>) iCommandEntries.subList(finalI+1, iCommandEntries.size())));
                if (currentCommand != null) {
                    builder.then(currentCommand);
                }
                currentCommand = builder;
            }
            spawnNew.then(Commands.literal(minion.faction().getID().toString()).executes(context -> spawnNewMinionExtra(context, context.getSource(), minion.faction(), (Supplier<MinionData>)minion.data(), minion.type(),List.of(), (Collection<IMinionBuilder.IMinionCommandBuilder.ICommandEntry<MinionData,?>>) iCommandEntries)).then(currentCommand));
        }
        return spawnNew;
    }

    @SuppressWarnings("unchecked")
    private static <T extends MinionData> int spawnNewMinionExtra(@NotNull CommandContext<CommandSourceStack> source, @NotNull CommandSourceStack ctx, IPlayableFaction<?> faction, @NotNull Supplier<T> data, Supplier<EntityType<? extends IMinionEntity>> type, Collection<IMinionBuilder.IMinionCommandBuilder.ICommandEntry<T,?>> contextProvider, Collection<IMinionBuilder.IMinionCommandBuilder.ICommandEntry<T,?>> defaultProvider) throws CommandSyntaxException{
        T t = data.get();
        for (IMinionBuilder.IMinionCommandBuilder.ICommandEntry<T, ?> tiCommandEntry : contextProvider) {
            ((BiConsumer<T,Object>)tiCommandEntry.setter()).accept(t, tiCommandEntry.getter().apply(source, tiCommandEntry.name()));
        }
        for (IMinionBuilder.IMinionCommandBuilder.ICommandEntry<T, ?> tiCommandEntry : defaultProvider) {
            ((BiConsumer<T,Object>)tiCommandEntry.setter()).accept(t, tiCommandEntry.defaultValue());
        }
        t.setHealth(t.getMaxHealth());
        return spawnNewMinion(ctx, faction, t, type.get());
    }

    @SuppressWarnings("SameReturnValue")
    private static <T extends IMinionData> int spawnNewMinion(@NotNull CommandSourceStack ctx, IPlayableFaction<?> faction, @NotNull T data, EntityType<? extends IMinionEntity> type) throws CommandSyntaxException {
        Player p = ctx.getPlayerOrException();
        FactionPlayerHandler fph = FactionPlayerHandler.getOpt(p).filter(s -> s.getMaxMinions() > 0).orElseThrow(() -> fail.create("Can't have minions"));
            PlayerMinionController controller = MinionWorldData.getData(ctx.getServer()).getOrCreateController(fph);
            if (controller.hasFreeMinionSlot()) {

                if (fph.getCurrentFaction() == faction) {
                    @SuppressWarnings("unchecked") int id = controller.createNewMinionSlot((MinionData) data, (EntityType<? extends MinionEntity<?>>) type);
                    if (id < 0) {
                        throw fail.create("Failed to get new minion slot");
                    }
                    controller.createMinionEntityAtPlayer(id, p);
                } else {
                    throw fail.create("Wrong faction");
                }


            } else {
                throw fail.create("No free slot");
            }

        return 0;
    }

    @SuppressWarnings("SameReturnValue")
    private static int recall(@NotNull CommandSourceStack ctx, ServerPlayer player) throws CommandSyntaxException {
        FactionPlayerHandler factionPlayerHandler = FactionPlayerHandler.getOpt(player).filter(s -> s.getMaxMinions() > 0).orElseThrow(() -> fail.create("Can't have minions"));
        PlayerMinionController controller = MinionWorldData.getData(ctx.getServer()).getOrCreateController(factionPlayerHandler);
        Collection<Integer> ids = controller.recallMinions(true);
        for (Integer id : ids) {
            controller.createMinionEntityAtPlayer(id, player);
        }

        return 0;
    }


    @SuppressWarnings("SameReturnValue")
    private static int respawn(@NotNull CommandSourceStack ctx, ServerPlayer player) throws CommandSyntaxException {
        FactionPlayerHandler fph = FactionPlayerHandler.getOpt(player).filter(s -> s.getMaxMinions() > 0).orElseThrow(() -> fail.create("Can't have minions"));
            PlayerMinionController controller = MinionWorldData.getData(ctx.getServer()).getOrCreateController(fph);
            Collection<Integer> ids = controller.getUnclaimedMinions();
            for (Integer id : ids) {
                controller.createMinionEntityAtPlayer(id, player);
            }
        return 0;
    }

    @SuppressWarnings("SameReturnValue")
    private static int purge(@NotNull CommandSourceStack ctx, ServerPlayer player) throws CommandSyntaxException {
        MinionWorldData.getData(ctx.getServer()).purgeController(player.getUUID());
        ((Player) player).displayClientMessage(Component.literal("Reload world"), false);
        return 0;
    }
}
