package io.github.terra121.control;

import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.core.server.CubeProviderServer;
import io.github.terra121.EarthTerrainProcessor;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandTP;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

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
