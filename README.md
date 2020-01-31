# Terra 1 to 1 Minecraft World Project
### Developed by TheAtomBomb92
### Co-Developed by shejan0
### Submod of [CubicChunks](https://github.com/OpenCubicChunks/CubicChunks) and [CubicWorldGen](https://github.com/OpenCubicChunks/CubicWorldGen/) from the [OpenCubicChunks](https://github.com/OpenCubicChunks) project. 



## Currently used APIs:
Data used for elevations is from [AWS Terrain Tiles](https://registry.opendata.aws/terrain-tiles/).

Data for the tree cover is from the [ARCGIS REST TreeCover2000 Image Server hosted by the World Resources Institute](https://gis-treecover.wri.org/arcgis/rest/services/TreeCover2000/ImageServer), originally from [Landsat 7 ETM+](http://glad.geog.umd.edu/).

Road (and soon, water) data is acquired from [OpenStreetMaps](https://www.openstreetmap.org/), downloaded in real-time using the public [Overpass API](http://overpass-api.de/) instance. (© OpenStreetMap contributors)

Climate (rain & temperature) data is from [The University of Delaware Center for Climatic Research's Climate Data Archive](http://climate.geog.udel.edu/~climate/html_pages/archive.html) (built into the mod)

Soil suborder data is from the [USDA Natural Resources Conservation Service's Global Soil Region Map](https://www.nrcs.usda.gov/wps/portal/nrcs/detail/soils/use/?cid=nrcs142p2_054013) (built into the mod)



## What is the difference between the other earth models in Minecraft?
Rather than being on a percentage scale of the actual world, such as 1:2000 the scale, this generator generates the world on a 1:1 scale (Approximately, actual scale varies based on latitude ). Every block is 1 meter of the real world in every dimension. 

**The heights of Mount Everest never felt so virtually high before.**



### THIS MOD DOWNLOADS DATA IN REALTIME FROM THE INTERNET!!!!! IT WILL NEED A DECENT INTERNET CONNECTION, AND WILL NOT WORK OFFLINE!!! DO NOT USE WITH MOBILE DATA CONNECTIONS!!!!



## How is it done?
**CubicChunks**, first of all, adds a 3rd dimensionality to the already existing Minecraft chunk system, allowing much more accessibility when it comes to vertical height. 

**CubicWorldGen** is an extension mod to *CubicChunks* to allow generation of worlds with 3 dimensions of chunks rather than the 2 dimensional generation of standard Minecraft.

This modification of CubicWorldGen generates the world using information from datasets regarding terrain, biome, and human structures with 3 dimensional chunks.



## Prerequisites

- **REQUIRED**: Minecraft Forge for the corrective version of Minecraft of the mod (currently Minecraft 1.12.2)
- **REQUIRED**: Standard CubicChunks for the corrective version of the mod.
  - [On GitHub](https://github.com/OpenCubicChunks/CubicChunks)
  - [On CurseForge](https://www.curseforge.com/minecraft/mc-mods/opencubicchunks)
  - (THE NEWEST VERSION OF THE CUBICCHUNKS MOD FOR ALL VERSIONS IS ALWAYS AVAILABLE ON THE [CUBIC CHUNKS DISCORD](https://discord.gg/kMfWg9m) under the **#info-new-cc** channel)
- **REQUIRED**: Standard CubicWorldGen for the corrective version of the mod.
  - [On GitHub](https://github.com/OpenCubicChunks/CubicWorldGen/)
  - [On CurseForge](https://www.curseforge.com/minecraft/mc-mods/cubicworldgen) 
  - (THE NEWEST VERSION OF THE CUBICCHUNKS MOD FOR ALL VERSIONS IS ALWAYS AVAILABLE ON THE [CUBIC CHUNKS DISCORD](https://discord.gg/kMfWg9m) under the **#info-new-cc** channel)
- Recomended: [Malisis Core](https://www.curseforge.com/minecraft/mc-mods/malisiscore):  (The Planet Earth generation at the current moment does not have any support for Malisis, but the original generation methods inside of CubicWorldGen do)

**You must have all required mods installed for the Planet Earth generation to work!!!**



## Obtaining

As of current, you must compile the mod yourself, luckily for pretty ol' you, here are the [building instructions](BUILD_INSTRUCTIONS.md)



## Client Usage
After completing the [Build Instructions](BUILD_INSTRUCTIONS.md). When creating a new world, under the World Type, you will now have an option called "**Planet Earth**" which will allow you to generate a world using the new generation method.

Upon creation, You will spawn near or on (0,0,0) (a.k.a. 0°N, 0°E or [Null Island](https://www.youtube.com/watch?v=bjvIpI-1w84)), This region is placeholder, meant to be a type of testing zone and also to not spawn under the ocean. You would need to [teleport away to see somewhere meaningful](COOL_LOCATIONS.md).

### Using you own coordinates/calculating your own coordinates
The block coordinates in Minecraft are calculated by (X, Y, Z). This mod will convert these values to coordinates on a world projection:
- X values are *(longitude × 10^5)*

- Y values is *the elevation in meters above the sea level*

- Z values are *(latitude × 10^5)*

**Remember that multiplying by 10^5 is the same as moving the decimal place 5 points to the right.**

#### Also remember that the longitude and latitude must be in decimal form (36.0660, -112.1172) and not degrees (36°03'57.6"N, 112°07'01.9"W)!!!



Example: **Yavapai Point, Grand Canyon, Arizona, USA** ([OpenStreetMap](https://www.openstreetmap.org/#map=16/36.0660/-112.1172), [Google Maps](https://www.google.com/maps/place/Yavapai+Point/@36.0660043,-112.1193887,17z))

has decimal coordinates of (36.0660, -112.1172) with an elevation just under 2200 meters, multiplying the latitude and longitude by 10^5 and setting Y to 2200 (the meters from sea level) gives the (X,Y,Z) coordinates of:
**(3606600, 2200, -11211720) **

or in tp command form: 
```java
/tp 3606600 2200 -11211720
```



## Having issues?

This mod is in very early stages, and based on another mod that is also in early stages, so the possibility of issues is EXTREMELY high. However, we have some [Troublshooting Tips](TROUBLESHOOT.md) that may help you in your endeavors to walk the earth in Minecraft.

