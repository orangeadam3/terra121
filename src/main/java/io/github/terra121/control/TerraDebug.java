package io.github.terra121.control;

import io.github.terra121.TerraConfig;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

public class TerraDebug extends CommandBase {
    @Override
    public String getName() {
        return "terradebug";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/terradebug - that's it";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {

        if (TerraConfig.debugModeActive == false) {
            TerraConfig.debugModeActive = true;
            sender.sendMessage(new TextComponentString("Terra 1:1 | Enabled debug mode. Use command again to disable."));
        } else {
            TerraConfig.debugModeActive = false;
            sender.sendMessage(new TextComponentString("Terra 1:1 | Disabled debug mode. Use command again to enable."));
        }

    }
}
