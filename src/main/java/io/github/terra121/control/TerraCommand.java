package io.github.terra121.control;

import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.core.server.CubeProviderServer;
import net.minecraft.command.CommandBase;
import net.minecraft.entity.Entity;
import net.minecraft.command.CommandException;
import net.minecraft.command.WrongUsageException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.chunk.IChunkProvider;
import io.github.terra121.EarthTerrainProcessor;
import io.github.terra121.projection.GeographicProjection;
import io.github.terra121.EarthBiomeProvider;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.event.ClickEvent;
import java.util.List;
import java.util.Arrays;
import net.minecraft.client.resources.I18n;

public class TerraCommand extends CommandBase {
	@Override
	public String getName() {
		return "terra";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "terra121.commands.terra.usage";
	}

	/*@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args) {
		if(args.length==0)
			return Arrays.asList("where","ou","world","osm","convert");
		return null;
	}*/

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		World world = sender.getEntityWorld();
		IChunkProvider cp = world.getChunkProvider();

		if(!(cp instanceof CubeProviderServer)) {
			throw new CommandException("terra121.error.notcc", new Object[0]);
		}

		ICubeGenerator gen = ((CubeProviderServer)cp).getCubeGenerator();

		if(!(gen instanceof EarthTerrainProcessor)) {
			throw new CommandException("terra121.error.notterra", new Object[0]);
		}

		String result = "";
		double[] c;
		GeographicProjection projection = ((EarthTerrainProcessor)gen).projection;

		switch (args.length==0?"":args[0].toLowerCase())
		{
		case "": case "where": case "ou":
			c = getPlayerCoords(sender, args.length<2?null:args[1], projection);
			if(c==null)throw new CommandException("terra121.error.getcoords", new Object[0]);
			else result = I18n.format("terra121.commands.terra.latlon", c[1], c[0]);
			break;

		case "world":
			//TODO: specifiy what setting to get
			result = I18n.format("terra121.commands.terra.gensettings") + ((EarthTerrainProcessor)gen).cfg.toString();
			break;

		case "osm":
			c = getPlayerCoords(sender, args.length<2?null:args[1], projection);
			if(c==null)throw new CommandException("terra121.error.getcoords", new Object[0]);

			String url = String.format("https://www.openstreetmap.org/#map=17/%.5f/%.5f",c[1],c[0]);
			ITextComponent out = new TextComponentString(url);
			out.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
			sender.sendMessage(out);
			result = null;
			break;

		case "conv": case "convert":
			if(args.length<3) {
				throw new WrongUsageException(getUsage(sender), new Object[0]);
			}

			c = getNumbers(args[1],args[2]);
			if(c==null)break;

			if(-180<=c[1]&&c[1]<=180&&-90<=c[0]&&c[0]<=90) {
				c = projection.fromGeo(c[1], c[0]);
				result = I18n.format("terra121.commands.terra.xy", c[0], c[1]);
			}
			else {
				c = projection.toGeo(c[0], c[1]);
				result = I18n.format("terra121.commands.terra.latlon", c[1], c[0]);
			}
			break;

		case "env": case "environment":
			BiomeProvider bp = world.getBiomeProvider();
			if(!(bp instanceof EarthBiomeProvider)) { //must have normal biome provider
				throw new CommandException("terra121.error.notterra", new Object[0]);
			}

			c = getCoordArgs(sender, args, projection);

			c = ((EarthBiomeProvider)bp).getEnv(c[0], c[1]);

			result =  I18n.format("terra121.commands.terra.environment", c[1], c[2], (int)c[0]);
			break;

		case "distortion": case "tissot": case "tiss":
			c = getCoordArgs(sender, args, projection);
			c = projection.tissot(c[0], c[1], 0.0000001);

			result =  I18n.format("terra121.commands.terra.tissot", Math.sqrt(Math.abs(c[0])), c[1]*180.0/Math.PI);
			break;

		default:
			throw new WrongUsageException(getUsage(sender), new Object[0]);
		}

		if(result!=null)sender.sendMessage(new TextComponentString(result));
	}

	double[] getCoordArgs(ICommandSender sender, String[] args, GeographicProjection projection) throws CommandException {
		if(args.length==3) {
			return getNumbers(args[2], args[1]);
		}
		else if(args.length==2) {
			double[] c = getPlayerCoords(sender, args[1], projection);
			if(c==null)throw new CommandException("terra121.error.getcoords", new Object[0]);
			return c;
		}
		else {
			double[] c = getPlayerCoords(sender, null, projection);
			if(c==null)throw new CommandException("terra121.error.getcoords", new Object[0]);
			return c;
		}
	}

	double[] getNumbers(String s1, String s2) throws CommandException {
		double x,y;
		try {
			x = Double.parseDouble(s1);
			y = Double.parseDouble(s2);
		} catch(Exception e) {
			throw new CommandException("terra121.error.numbers", new Object[0]);
		}

		return new double[] {x,y};
	}

	double[] getPlayerCoords(ICommandSender sender, String arg, GeographicProjection projection) throws CommandException {
		Vec3d pos;
		Entity e = sender.getCommandSenderEntity();
		if(arg!=null) {
			if(!isOp(sender)) {
				throw new CommandException("terra121.error.notopothers", new Object[0]);
			}
			e = sender.getEntityWorld().getPlayerEntityByName(arg);
			if(e==null) {
				throw new CommandException("terra121.error.unknownplayer", new Object[0]);
			}
			pos = e.getPositionVector();
		}
		else if(e!=null)
			pos = sender.getPositionVector();
		else {
			throw new CommandException("terra121.error.notplayer", new Object[0]);
		}

		double[] proj = projection.toGeo(pos.x, pos.z);

		return proj;
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
