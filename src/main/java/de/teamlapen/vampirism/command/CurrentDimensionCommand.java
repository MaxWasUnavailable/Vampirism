package de.teamlapen.vampirism.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import de.teamlapen.lib.lib.util.BasicCommand;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;

/**
 * @authors Cheaterpaul, Maxanier
 */
public class CurrentDimensionCommand extends BasicCommand {

    public static ArgumentBuilder<CommandSource, ?> register() {
        return Commands.literal("currentDimension")
                .requires(context -> context.hasPermissionLevel(PERMISSION_LEVEL_ALL))
                .executes(context -> {
                    return currentDimension(context, context.getSource().asPlayer());
                });
    }

    private static int currentDimension(CommandContext<CommandSource> context, ServerPlayerEntity asPlayer) {
        context.getSource().sendFeedback(new TranslationTextComponent("command.vampirism.base.currentdimension.dimension", asPlayer.getEntityWorld().func_234923_W_().func_240901_a_().toString() + " (" + asPlayer.getServer().func_244267_aX().func_230520_a_().getKey(asPlayer.getEntityWorld().func_230315_m_())), false);
        return 0;
    }
}
