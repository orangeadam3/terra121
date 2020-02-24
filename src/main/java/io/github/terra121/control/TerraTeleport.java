package io.github.terra121.control;

import org.apache.commons.codec.DecoderException;

import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.core.server.CubeProviderServer;
import io.github.terra121.EarthTerrainProcessor;
import io.github.terra121.projection.GeographicProjection;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandTP;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.server.permission.PermissionAPI;

public class TerraTeleport extends CommandBase {
	
	@Override
	public String getName() {
		return "tpll";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/tpll lat lon [altitude]";
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
				sender.sendMessage(new TextComponentString("Must be in a cubic chunks world"));
				return;
			}

			ICubeGenerator gen = ((CubeProviderServer)cp).getCubeGenerator();
			
			if(!(gen instanceof EarthTerrainProcessor)) {
				sender.sendMessage(new TextComponentString("Must be in a Terra 1-1 world!"));
				return;
			}
			
			EarthTerrainProcessor terrain = (EarthTerrainProcessor)gen;
			
			if(args.length!=2&&args.length!=3) {
				sender.sendMessage(new TextComponentString("Usage: "+getUsage(sender)));
				return;
			}
			
			double lon, lat;
			String alt = null;
			
			try {
				lat = Double.parseDouble(args[0]);
				lon = Double.parseDouble(args[1]);
				if(args.length>2) {
					alt = args[2];
				}
			} catch(Exception e) {
				sender.sendMessage(new TextComponentString("All arguments must be numbers"));
				return;
			}
			
			double proj[] = terrain.projection.fromGeo(lon, lat);
			
			if(alt==null)
				alt = ""+(terrain.heights.estimateLocal(lon, lat)+1);
			
			(new CommandTP()).execute(server, sender, new String[] {""+(proj[0]), alt, ""+(proj[1])});
		}
	}
	
	private boolean isOp(ICommandSender sender) {
		return sender.canUseCommand(2, "");
	}

}
