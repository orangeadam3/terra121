package io.github.terra121.control;

import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.core.server.CubeProviderServer;
import io.github.terra121.EarthTerrainProcessor;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.WrongUsageException;
import net.minecraft.command.CommandTP;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
// allow command usage via the permission node terra121.commands.tpll
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraft.entity.player.EntityPlayer;

public class TerraTeleport extends CommandBase {

	@Override
	public String getName() {
		return "tpll";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "terra121.commands.tpll.usage";
	}

	public int getRequiredPermissionLevel() {
		return 2;
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if(isOp(sender)) {

			World world = sender.getEntityWorld();
			IChunkProvider cp = world.getChunkProvider();

			if(!(cp instanceof CubeProviderServer)) {
				throw new CommandException("terra121.error.notcc", new Object[0]);
			}

			ICubeGenerator gen = ((CubeProviderServer)cp).getCubeGenerator();

			if(!(gen instanceof EarthTerrainProcessor)) {
				throw new CommandException("terra121.error.notterra", new Object[0]);
			}

			EarthTerrainProcessor terrain = (EarthTerrainProcessor)gen;

			if(args.length==0)
				throw new WrongUsageException(getUsage(sender), new Object[0]);

			String[] splitCoords = args[0].split(",");
			String alt = null;
			if(splitCoords.length==2&&args.length<3) { // lat and long in single arg
				if(args.length>1) alt = args[1];
				args = splitCoords;
			} else if(args.length==3) {
				alt = args[2];
			}
			if(args[0].endsWith(","))
				args[0] = args[0].substring(0, args[0].length() - 1);
			if(args.length>1&&args[1].endsWith(","))
				args[1] = args[1].substring(0, args[1].length() - 1);
			if(args.length!=2&&args.length!=3) {
				throw new WrongUsageException(getUsage(sender), new Object[0]);
			}

			double lon, lat;

			try {
				lat = Double.parseDouble(args[0]);
				lon = Double.parseDouble(args[1]);
				if(alt!=null) alt = Double.toString(Double.parseDouble(alt));
			} catch(Exception e) {
				throw new CommandException("terra121.error.numbers", new Object[0]);
			}

			double proj[] = terrain.projection.fromGeo(lon, lat);

			if(alt==null)
				alt = String.valueOf(terrain.heights.estimateLocal(lon, lat)+1);

			new CommandTP().execute(server, sender, new String[] {
				String.valueOf(proj[0]), alt, String.valueOf(proj[1])});
		}
	}
	
	private boolean isOp(ICommandSender sender) {
		if (sender instanceof EntityPlayer) {
			return PermissionAPI.hasPermission((EntityPlayer) sender, "terra121.commands.tpll");
		}
		return sender.canUseCommand(2, "");
	}

}
