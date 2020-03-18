package io.github.terra121.control;

import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.core.server.CubeProviderServer;
import net.minecraft.command.CommandBase;
import net.minecraft.entity.Entity;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandTP;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import io.github.terra121.EarthTerrainProcessor;
import io.github.terra121.projection.GeographicProjection;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.event.ClickEvent;

public class TerraCommand extends CommandBase {
	@Override
	public String getName() {
		return "terra";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/terra [where:world:osm] [player]";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		World world = sender.getEntityWorld();
		IChunkProvider cp = world.getChunkProvider();

		if(!(cp instanceof CubeProviderServer)) {
			sender.sendMessage(new TextComponentString("This is not a cubic chunks world"));
			return;
		}

		ICubeGenerator gen = ((CubeProviderServer)cp).getCubeGenerator();

		if(!(gen instanceof EarthTerrainProcessor)) {
			sender.sendMessage(new TextComponentString("This is not a Terra 1-1 world"));
			return;
		}

		String result = "";
		double[] c;

		switch (args.length==0?"":args[0].toLowerCase())
		{
		case "": case "where": case "ou":
			c = getPlayerCoords(sender, args.length<2?null:args[1], (EarthTerrainProcessor)gen);
			if(c==null)result = "failed to get coords";
			else result = "latitude:"+c[0]+" longitude:"+c[1]+", "+c[2]+" above ground";
			break;
		case "world":
			result = "Generator Settings: " + ((EarthTerrainProcessor)gen).cfg.toString();
			break;
		case "osm":
			c = getPlayerCoords(sender, args.length<2?null:args[1], (EarthTerrainProcessor)gen);
			if(c==null)result = "failed to get coords";
			else {
				String url = "https://www.openstreetmap.org/#map=17/"+c[0]+"/"+c[1];
				ITextComponent out = new TextComponentString(url);
				out.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
				sender.sendMessage(out);
				result = null;
			}
			break;
		default:
			result = getUsage(sender);
		}

		if(result!=null)sender.sendMessage(new TextComponentString(result));
	}

	double[] getPlayerCoords(ICommandSender sender, String arg, EarthTerrainProcessor gen) {
		Vec3d pos;
		Entity e = sender.getCommandSenderEntity();
		if(arg!=null) {
			if(!isOp(sender)) {
				sender.sendMessage(new TextComponentString("OP required for info on other players"));
				return null;
			}
			e = sender.getEntityWorld().getPlayerEntityByName(arg);
			if(e==null) {
				sender.sendMessage(new TextComponentString("Unknown player"));
				return null;
			}
			pos = e.getPositionVector();
		}
		else if(e!=null)
			pos = sender.getPositionVector();
		else {
			sender.sendMessage(new TextComponentString("you are not a player, please specify one"));
			return null;
		}

		double[] proj = gen.projection.toGeo(pos.x, pos.z);

		return new double[]{proj[0],proj[1],pos.y-gen.heights.estimateLocal(proj[0], proj[1])};
	}

	private boolean isOp(ICommandSender sender) {
		return sender.canUseCommand(2, "");
	}

	public int getRequiredPermissionLevel() {
		return 0;
	}
	
	public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
		return true;
	}
}
