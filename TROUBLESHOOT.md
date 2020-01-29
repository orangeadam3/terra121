# Building Troubleshooting
### BuildSrc is empty
Like we said in the [Build instructions](BUILD_INSTRUCTIONS.md), you must use Git installed on your machine and run recursively, using the simple Download .zip option does not work.

If you did do that, and it still did not work, you can bypass recursively grabbing all the files at once, and manually downloading a ZIP or git cloning all of the contents of [CubicGradle](https://github.com/OpenCubicChunks/CubicGradle), into the *BuildSrc* folder, if the folder doesn't exist then manually create it.

### If you do not reach BUILD SUCCESSFUL during the building process
Most likely the Workspace was not created properly, or the build cache is on automatic garbage collection mode. You can try building the DecompWorkspace with an external cache (the workspace to allow code modification with referneces to outside libraries, and seeing if that allows the mod to compile by doing:

On Windows:

```bash
gradlew.bat setupDecompWorkspace -g TEST_CACHE
```
On macOS/Linux
```bash
./gradlew setupDecompWorkspace -g TEST_CACHE
```
and try rebuilding by using:

On Windows:

```bash
gradlew.bat build -g TEST_CACHE_BUILD
```
On macOS/Linux:
```bash
./gradlew build -g TEST_CACHE_BUILD
```



# In-game/running Troubleshooting

### Where on earth am I?
As of current, when you spawn you will not be anywhere near any real-world position, also called Null Island. You will need to first teleport yourself to a world location, here are some example locations that might be of interest:

You can use some of our [Cool locations](COOL_LOCATIONS.md) to find somewhere interesting to teleport to.

### " java.lang.OutOfMemoryError: GC overhead limit exceeded", what is that, how do I fix?
You exceeded the amount of memory that the JVM (Minecraft's running environment) has to use, there are a lot of chunks in the cubic space, and a lot of blocks in those chunks. Especially considering that you will most likely be moving great lengths to get anywhere, you will a lot of memory available to the game to run properly.

Inside of Minecraft's Launcher, go to the Installations tab, find your installation of Forge, and select Edit:

![Editing JVM](Pictures/InstallationsEdit.png)

Select **"More Options"**, and find the **"JVM Arguments"** section:

![Editing JVM](Pictures/EditJVMArgs.png)

Adding or changing the following value:

```
-Xmx####(suffix)
```
will change the amount of RAM that the JVM can use. **(Remember using a capital letter as the suffix is different from using a lowercase letter)**:

**-Xmx1G** is 1 gigabyte
**-Xmx8G** is 8 gigabytes
**-Xmx16G** is 16 gigabytes
......and so on.

However,

**-Xmx1g** is 1 gigabit (125 Megabytes)
**-Xmx8g** is 8 gigabit (1 Gigabyte)
**-Xmx16g** is 16 gigabit (2 Gigabytes, default Minecraft memory)
......and so on.

### "java.lang.NoSuchFieldError" for a func number or a missing biome
This error seems to originate with an uncompleted reference build, although Gradle will finish Building and the build will be successful, when it comes to run the mod, it builds with placeholder locations for references to other neccessary code, such as the code for Minecraft itself (like Biome information).

A good work around is to setup the Decompiliation cache on your system for Minecraft Forge Gradle, which decompiles Minecraft, Forge, and any extra libraries (the stuff in the lib folder). Normally just building should not need this step, unless you are intending to change the code of the mod.

If you are on Windows:
```bash
gradlew.bat setupDecompWorkspace
```
If you are on macOS or Linux:
```bash
./gradlew setupDecompWorkspace
```
The build with the same commands as before:

If you are on Windows:
```bash
gradlew.bat build
```
If you are on macOS or Linux:
```bash
./gradlew build
```
