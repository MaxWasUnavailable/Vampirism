package de.teamlapen.lib.lib.util;


import de.teamlapen.lib.VampLib;
import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Basic command which manages subcommands
 */
public abstract class BasicCommand extends CommandBase {
    public static void sendMessage(ICommandSender target, String message) {
        String[] lines = message.split("\\n");
        for (String line : lines) {
            target.sendMessage(new TextComponentString(line));
        }

    }

    protected final int PERMISSION_LEVEL_CHEAT = 2;
    protected final int PERMISSION_LEVEL_ADMIN = 3;
    protected final int PERMISSION_LEVEL_FULL = 4;
    protected List<String> aliases;
    private List<SubCommand> subCommands;
    private SubCommand unknown;

    public BasicCommand() {
        aliases = new ArrayList<>();
        subCommands = new ArrayList<SubCommand>();
        unknown = new SubCommand() {
            @Override
            public List addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
                return null;
            }

            @Override
            public boolean canSenderUseCommand(ICommandSender sender) {
                return true;
            }

            @Override
            public String getCommandName() {
                return "unknown";
            }

            @Override
            public String getCommandUsage(ICommandSender sender) {
                return BasicCommand.this.getUsage(sender);
            }

            @Override
            public void processCommand(MinecraftServer server, ICommandSender sender, String[] args) {
                sendMessage(sender, "Unknown command");
            }
        };
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    @Override
    public int compareTo(ICommand o) {
        return 0;
    }

    @Override
    public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] param) throws CommandException {
        if (param.length == 0) {
            throw new WrongUsageException(getUsage(sender));
        }
        if ("help".equals(param[0])) {
            if (param.length > 1) {
                sendMessage(sender, String.format("/%s %s", this.getName(), getSub(param[1]).getCommandUsage(sender)));
            } else {
                StringBuilder builder = new StringBuilder(UtilLib.translate("text.vampirism.command.available_subcommands"));
                builder.append(' ');
                for (SubCommand s : subCommands) {
                    builder.append(s.getCommandName()).append(", ");
                }
                builder.append(UtilLib.translateFormatted("text.vampirism.command.subcommand_help", getName()));
                sendMessage(sender, builder.toString());
            }
            return;

        }
        SubCommand cmd = getSub(param[0]);
        if (cmd.canSenderUseCommand(sender)) {

            try {
                cmd.processCommand(server, sender, ArrayUtils.subarray(param, 1, param.length));
            } catch (CommandException e) {
                throw e;
            } catch (Exception e) {
                VampLib.log.e("BasicCommand", e, "Failed to execute command %s with params %s", cmd, Arrays.toString(ArrayUtils.subarray(param, 1, param.length)));
                throw new CommandException("commands.vampirism.failed_to_execute");
            }
        } else {
            TextComponentTranslation textcomponenttranslation1 = new TextComponentTranslation("commands.generic.permission");
            textcomponenttranslation1.getStyle().setColor(TextFormatting.RED);
            sender.sendMessage(textcomponenttranslation1);
        }
    }


    @Nonnull
    @Override
    public List<String> getAliases() {
        return aliases;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
        return (args.length == 1) ? getListOfStringsMatchingLastWord(args, getSubNames()) : getSubcommandTabCompletion(sender, args, pos);
    }

    @Nonnull
    @Override
    public String getUsage(@Nonnull ICommandSender sender) {
        return UtilLib.translateFormatted("text.vampirism.command.usage", this.getName(), this.getName());
    }

    @Override
    public boolean isUsernameIndex(String[] p_82358_1_, int p_82358_2_) {
        return false;
    }

    protected void addSub(SubCommand s) {
        subCommands.add(s);
    }

    protected boolean canCommandSenderUseCheatCommand(ICommandSender sender) {
        return sender.canUseCommand(PERMISSION_LEVEL_CHEAT, this.getName()) || (sender instanceof EntityPlayer) && ((EntityPlayer) sender).capabilities.isCreativeMode;
    }

    /**
     * Returns the subcommand matching the given name.
     * If no command is found, returns a default "unknown" command.
     *
     */
    private SubCommand getSub(String name) {
        for (SubCommand s : subCommands) {
            if (s.getCommandName().equals(name)) return s;
        }
        return unknown;
    }

    private String[] getSubNames() {
        String[] names = new String[subCommands.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = subCommands.get(i).getCommandName();
        }
        return names;
    }

    private
    @Nonnull
    List getSubcommandTabCompletion(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length < 2) return Collections.EMPTY_LIST;
        List options = getSub(args[0]).addTabCompletionOptions(sender, ArrayUtils.subarray(args, 1, args.length), pos);
        return options == null ? Collections.emptyList() : options;
    }

    public interface SubCommand {
        List addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos);

        boolean canSenderUseCommand(ICommandSender sender);

        String getCommandName();

        String getCommandUsage(ICommandSender sender);

        void processCommand(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException;
    }

}