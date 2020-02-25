### CubicWorldGen 115 forward has code changes that makes the current release build incompatible with it, we are working dilligently to get the mod up to date, but for now please use versions between 85 and 115, a minor update has been released to allow compatibility with CWG 115+ but introduces issues elsewhere. If you want a stable game with CWG 115 forward, we recommend you wait for a major update.

### We deeply apologize for the wait, we are also working on adding features on top of fixing CWG 115+ so expect great new things to be added very soon.

*sniff sniff* **OwO** do I smell upcoming GUIs and projections in a major release?

![Logo](Pictures/TerraTextIconnocompress.png)

# Terra 1 to 1 Minecraft Earth Project

### Developed by orangeadam3
### Co-Developed and images by shejan0 
### Submod of [CubicChunks](https://github.com/OpenCubicChunks/CubicChunks) and [CubicWorldGen](https://github.com/OpenCubicChunks/CubicWorldGen/) from the [OpenCubicChunks](https://github.com/OpenCubicChunks) project. 

![Copper Canyon](Pictures/CopperCanyonMex.png)

Barranca del Cobre (Copper Canyon), Chihuahua, Mexico 

## Public Server

### We now have a public server that you can test the mod without ever having to install it!!!! 

The server is hosted by FyreBurns from the [Cubic Chunks Discord](https://discord.gg/kMfWg9m), you will need **AT LEAST** Forge and CubicChunks installed on your machine to join (download information can be found in the Prerequisites below)!

Either Direct Connect or Add a new server with the following IP:

### ert.g.akliz.net

This is also another work in progress, so permissions and things may change according to FyreBurns, we greatly appreciate the resources that FyreBurns is placing to run this server for the public.

**If you cannot join, then most likely it is off, do not spam message FyreBurns or us, check back at another time.**

*Thank you FyreBurns for your efforts, much appreciated*

*- shejan0 and orangeadam3* 😘

## Streaming? (we sure do)

So in order to get our mod onto [CurseForge](https://www.curseforge.com/minecraft/mc-mods/terra-1-to-1-minecraft-world-project#c6) (Hey to those who came over from CurseForge), we had to make a twitch account.

And because a Twitch account can stream to twitch, shejan0 has been thinking about streaming to Twitch with that account.

So give us a follow over at our [TheTerraDev](https://www.twitch.tv/theterradev) Twitch account, or watch live here (you need to go to the stream in order to chat, sorry GitHub is weird).

<dl>
  <iframe src="https://player.twitch.tv/?channel=theterradev" frameborder="0" allowfullscreen="true" scrolling="no" height="378" width="620"></iframe><a href="https://www.twitch.tv/theterradev?tt_content=text_link&tt_medium=live_embed" style="padding:2px 0px 4px; display:block; width:345px; font-weight:normal; font-size:10px; text-decoration:underline;">Watch live video from theterradev on www.twitch.tv</a>
</dl>


Also let shejan0#9517 know if you want us to host your stream on our page if you are using our mod in your stream.

## Creation showcase

Check out the mod showcase by [Baccar Wozat](https://www.youtube.com/channel/UClbmNgZmGahCDKcs8_aMN0A), Administrator and Moderator on the [Cubic Chunks Discord](https://discord.gg/kMfWg9m), and has been interested in the development of our mod since the beginning.

<dl>
  <iframe width="882" height="496" src="https://www.youtube.com/embed/XJG1XXOdJcc" frameborder="0" allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</dl>

or watch it [here](https://www.youtube.com/watch?v=XJG1XXOdJcc).

or watch it [here](https://www.youtube.com/watch?v=XJG1XXOdJcc).

-*Much love from orangeadam3 and shejan0* 😫😤😘

You can also see all the other creations in our [Full Creations showcase](CREATIONS.md)

## Currently used APIs:

Elevations data is downloaded in real time from [AWS Terrain Tiles](https://registry.opendata.aws/terrain-tiles/). (© [Mapzen](https://www.mapzen.com/rights), [OpenStreetMap](https://openstreetmap.org/copyright), and [others](https://mapzen.com/rights/#services-and-data-sources))

Tree cover data is downloaded in real time from the [ARCGIS REST TreeCover2000 Image Server hosted by the World Resources Institute](https://gis-treecover.wri.org/arcgis/rest/services/TreeCover2000/ImageServer), originally from [Landsat 7 ETM+](http://glad.geog.umd.edu/) (Originally [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/))

Road (and soon, water) data is acquired from [OpenStreetMap](https://www.openstreetmap.org/) under the [Open Database License](https://www.openstreetmap.org/copyright). It is downloaded in real-time using a public [Overpass API](http://overpass-api.de/) instance. (© OpenStreetMap contributors)

Climate (rain & temperature) data is from [The University of Delaware Center for Climatic Research's Climate Data Archive](http://climate.geog.udel.edu/~climate/html_pages/archive.html) (built into the mod)

Soil suborder data is from the [USDA Natural Resources Conservation Service's Global Soil Region Map](https://www.nrcs.usda.gov/wps/portal/nrcs/detail/soils/use/?cid=nrcs142p2_054013) (built into the mod)

### THIS MOD DOWNLOADS DATA IN REAL-TIME FROM THE INTERNET!!!!! IT WILL NEED A DECENT INTERNET CONNECTION, AND WILL NOT WORK OFFLINE!!! DO NOT USE WITH MOBILE DATA CONNECTIONS, UNLESS YOU HAVE UNLIMITED DATA!!!!

## What is the difference between the other earth models in Minecraft?
Rather than being on a percentage scale of the actual world, such as 1:2000 the scale, this generator generates the world on a 1:1 scale (Approximately, actual scale varies based on latitude ). Every block is 1 meter of the real world in every dimension. 

![Mount Everest](Pictures/MountEverestNepal.png)

**The heights of Mount Everest never felt so virtually high before.**

You can take a sneak peak with our [Screenshot Showcase](PICTURES.md)

## How is it done?
**CubicChunks**, first of all, adds a 3rd dimensionality to the already existing Minecraft chunk system, allowing much more accessibility when it comes to vertical height. 

**CubicWorldGen** is an extension mod to *CubicChunks* to allow generation of worlds with 3 dimensions of chunks rather than the 2 dimensional generation of standard Minecraft.

This modification of CubicWorldGen generates the world using information from datasets regarding terrain, biome, and human structures with 3 dimensional chunks.

## Prerequisites

- **REQUIRED**: [Minecraft Forge](https://files.minecraftforge.net/) for the corrective version of Minecraft of the mod (currently Minecraft 1.12.2, this mod is tested using 2847 and 2768)
- **REQUIRED**: Standard CubicChunks for the corrective Minecraft version of the mod.
  - [On GitHub](https://github.com/OpenCubicChunks/CubicWorldGen/) (releases may be out of date, but can be compiled)
  - [On CurseForge](https://www.curseforge.com/minecraft/mc-mods/cubicworldgen) (may be out of date)
  - (THE NEWEST VERSION OF THE CUBICCHUNKS MOD FOR ALL VERSIONS IS ALWAYS AVAILABLE ON THE [CUBIC CHUNKS DISCORD](https://discord.gg/kMfWg9m) under the **#info-new-cc** channel)
- **REQUIRED**: Standard CubicWorldGen for the corrective Minecraft version of the mod **VERSION 0.0.85.0 OR HIGHER IS HIGHLY RECOMMENDED**
  - [On GitHub](https://github.com/OpenCubicChunks/CubicWorldGen/) (releases may be out of date, but can be compiled)
  - [On CurseForge](https://www.curseforge.com/minecraft/mc-mods/cubicworldgen) (may be out of date)
  - (THE NEWEST VERSION OF THE CUBICCHUNKS MOD FOR ALL VERSIONS IS ALWAYS AVAILABLE ON THE [CUBIC CHUNKS DISCORD](https://discord.gg/kMfWg9m) under the **#info-new-cc** channel)
  - CubicWorldGen 0.0.85.0 [download direct from the developers](http://www.mediafire.com/file/57ki07oq2cw86bj/CubicWorldGen-1.12.2-0.0.85.0-SNAPSHOT-all.jar/file) if you are unsure about which service to use (still recommended to check the [Cubic Chunks Discord](https://discord.gg/kMfWg9m) channel if there is any newer versions)
- Recommended: [Malisis Core](https://www.curseforge.com/minecraft/mc-mods/malisiscore):  (The Planet Earth generation at the current moment does not have any support for Malisis, and is a future plan, but the original generation methods inside of CubicWorldGen do support Malisis)

**You must have all required mods installed for the Planet Earth generation to work!!!**

### CubicWorldGen 0.0.85.0 or later is HIGHLY recommended, as it has changes made that no longer causes conflicts with CubicWorldGen's default "Custom Cubic" generator, with the Terra 1-to-1 Planet Earth generator. 

### (Users who have standard Custom Cubic worlds with earlier CubicWorldGen versions and load Terra 1-to-1, might have broken generation on new chunks)!!!!



## Obtaining

We have compiled releases both on our [releases page](https://github.com/orangeadam3/terra121/releases), and on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/terra-1-to-1-minecraft-world-project). 

However, some of you are little itchy peepers, and want the most recent version of the code, fresh with all the testing and beta, and broken stuffs. 

Luckily for pretty ol' you, here are the [building instructions](BUILD_INSTRUCTIONS.md).

However, some of you are little itchy peepers, and want the most recent version of the code, fresh with all the testing and beta, and broken stuffs. 

Luckily for pretty ol' you, here are the [building instructions](BUILD_INSTRUCTIONS.md), or you can use builds from this [unofficial Jenkins CI](https://jenkins.altillimity.com/job/Terra/) that Aang23#7721 on the [Cubic Chunks Discord](https://discord.gg/kMfWg9m) created, this will always build the latest git master branch, so you don't have to build locally. (For the code that runs in Minecraft, make sure you get the file that does *not* end in **-sources.jar**)

Thanks Aang, much appreciated.



## Client Usage
After obtaining the binary (through a method listed above), when creating a new world, under the World Type, you will now have an option called "**Planet Earth**" which will allow you to generate a world using the new generation method.

Upon creation, You will spawn near or on (0,0,0) (a.k.a. 0°N, 0°E or [Null Island](https://www.youtube.com/watch?v=bjvIpI-1w84)), This region is placeholder, meant to be a type of testing zone and also to not spawn under the ocean. You would need to [teleport away to see somewhere meaningful](COOL_LOCATIONS.md).

## Server Usage
Instructions on how to use this mod in a Minecraft Forge Server can be found in our [Server Run Instructions](USING_SERVER.md)

## Using you own coordinates/calculating your own coordinates
The block coordinates in Minecraft are calculated by (X, Y, Z). This mod will convert these values to coordinates on a world projection:
- X values are *(longitude × 10^5)*

- Y values is *the elevation in meters above the sea level*

- Z values are *(latitude × 10^5)*

**Remember that multiplying by 10^5 is the same as moving the decimal place 5 points to the right.**

#### Also remember that the longitude and latitude must be in decimal form (36.0660, -112.1172) and not degrees (36°03'57.6"N, 112°07'01.9"W)!!!

Example: **Yavapai Point, Grand Canyon, Arizona, USA** ([OpenStreetMap](https://www.openstreetmap.org/#map=16/36.0660/-112.1172), [Google Maps](https://www.google.com/maps/place/Yavapai+Point/@36.0660043,-112.1193887,17z))

![Yavapai Point](Pictures/YavapaiPointGrandCanyonUS1.png)

has decimal coordinates of (36.0660, -112.1172) with an elevation just under 2200 meters, multiplying the latitude and longitude by 10^5 and setting Y to 2200 (the meters from sea level) gives the (X,Y,Z) coordinates of:
**(3606600, 2200, -11211720) **

or in tp command form: 
```java
/tp 3606600 2200 -11211720
```



## Having issues?

This mod is in very early stages, and based on another mod that is also in early stages, so the possibility of issues is EXTREMELY high. However, we have some [Troubleshooting Tips](TROUBLESHOOT.md) that may help you in your endeavors to walk the earth in Minecraft.

However, if you find issues with the mod that are not resolved by doing things in the Troubleshooting page, then feel free to drop an [issue request](https://github.com/orangeadam3/terra121/issues).

## Known problems

This mod is still in development, and we are still resolving problems that we have found, and are being found, but here is a small list of some of the issues we know exist with this mod:

- The mod is not compatible with CWG 115 or later.
- As a rule, if it involves water it is probably broken, most of these problems should be fixed when the new water system is added (see Future Plans):
  - Areas on land but below sea level (ex. parts of The Netherlands, Caspian Sea Depression, Dead Sea region, Imperial Valley, etc.) are covered in water as if they were below sea level.
  - Coastlines are very broken/blockly, no non-accidental beaches
  - There are no above sea level rivers or lakes except for the standard procedural minecraft sources
  - Parts of the ocean make odd shapes or appear as land (ex. the prime meridian ridge near null island) (this may never be fully fixed as it is fundamentally caused by glitches in the terrain tile's barometry data, but it's severity can be reduced)
- The shape of biomes usually comes in 4-km blocks and the boundries are strait lines (this could be fixed with some smart interpolation and/or perlin noise)
- Biomes are classified incorrectly in some places (this could be improved by more thorough classification)
- The terrain looks very linear in some places (also could be fixed by minor perlin noise)
- Seed and Flower item drops will sometimes appear on roads (I honestly don't fully understand this one)
- Most ores only spawn in or below their default locations (around 0-63 Y), this will hopefully be fixed with a new ore system (see Future Plans)

## Future Plans

- Smarter ore generation that varies based on surface altitude, so that you don't have to dig 5000+ blocks down to find basic ores if you setup a base in the Himalayas.

- Water based on actual river/lake/coastline locations from OpenStreetMap

- Update forest data from 2000 to 2012 (we found a newer one wooooo)

- [Malisis Core](https://www.curseforge.com/minecraft/mc-mods/malisiscore) GUI for world customization and bonus features such as:
  - Changing the [projection](https://en.wikipedia.org/wiki/Map_projection) from Mercator to something else (such as [Sinusoidal](https://en.wikipedia.org/wiki/Sinusoidal_projection))
  - Changing the scale of the world (both vertical and horizontal)
  - Disabling Roads and other features
  - Enabling esoteric features that might not be appreciated by everyone (ex. Road signs with names at evrey intersection).
  - Custom spawnpoint
  - Normal generation options like cave/ore frequency, etc.
  
- A custom set of commands to help you navigate the world by doing things such as:
  - Converting Latitude and Longitude to Minecraft coordinates (and vice versa, especially if custom projections were added)
  - Tell you things about the surrounding area such as street names and addresses

- An organic way to dealing with connecting both sides of the [antimeridian](https://en.wikipedia.org/wiki/180th_meridian) (maybe by simply teleporting the player from one side to the other?)

- A nether where 1 block in the nether = 1000 blocks (1km) in the overworld, instead of the vanilla 1:8 ratio. This would make legitimate globe spanning survival practical as traveling to the other side of the earth would only be 20,000 blocks in the nether (not exactly a short trip but better than the 20,000,000 blocks (2/3s the way to the World Border) that it would take in the overworld)
