package io.github.terra121.control;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

public class TerraCommand extends CommandBase {
	@Override
	public String getName() {
		return "terra";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "work in progress";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		
	}
	
	public int getRequiredPermissionLevel() {
		return 0;
	}
	
	public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
		return true;
	}
}
