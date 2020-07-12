package io.github.terra121.control;

import io.github.terra121.TerraConfig;
import io.github.terra121.dataset.OpenStreetMaps;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

public class TerraOverpassCommand extends CommandBase {

	public static final String COMMAND_NAME = "overpass";
	
	@Override
	public String getName() {
		return COMMAND_NAME;
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return COMMAND_NAME + " <status|list|default|fallback>";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if(args.length != 1) throw new CommandException("Invalid number of arguments"); //TODO Localize
		String inUse = OpenStreetMaps.getOverpassEndpoint();
		boolean isDefault = inUse.equals(TerraConfig.serverOverpassDefault);
		boolean hasFallback = !TerraConfig.serverOverpassFallback.equals("");
		switch(args[0]) {
		case "status":
			String t = isDefault? "default": "fallback";
			ITextComponent msg = new TextComponentString("Currently using " + t + " Overpass endpoint: " + inUse); //TODO Localize
			sender.sendMessage(msg);
			break;
		case "list":
			ITextComponent msg1 = new TextComponentString("Default:  " + TerraConfig.serverOverpassDefault); //TODO Localize
			ITextComponent msg2 = new TextComponentString(hasFallback?
					"Fallback: " + TerraConfig.serverOverpassFallback:
					"No fallback endpoint is set in the config"); //TODO Localize
			sender.sendMessage(msg1);
			sender.sendMessage(msg2);
			break;
		case "default":
			OpenStreetMaps.setOverpassEndpoint(TerraConfig.serverOverpassDefault);
			msg = new TextComponentString("Now using the default endpoint"); //TODO Localize
			sender.sendMessage(msg);
			break; //TODO main
		case "fallback":
			if(!hasFallback) throw new CommandException("No fallback endpoint is set in the config"); //TODO Localize
			OpenStreetMaps.setOverpassEndpoint(TerraConfig.serverOverpassFallback);
			msg = new TextComponentString("Now using the fallback endpoint"); //TODO Localize
			sender.sendMessage(msg);
			break; //TODO fallback
		default:
			throw new CommandException("Invalid argument"); //TODO Localize
		}
	}

}
