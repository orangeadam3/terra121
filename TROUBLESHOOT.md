# Building Troubleshooting
### BuildSrc is empty
Like we said in the [Build instructions](BUILD_INSTRUCTIONS.md), you must use Git installed on your machine and run recursively, using the simple Download .zip option does not work.

If you did do that, and it still did not work, you can bypass recursively grabbing all the files at once, and manually downloading a ZIP or git cloning all of the contents of [CubicGradle](https://github.com/OpenCubicChunks/CubicGradle), into the *BuildSrc* folder, if the folder doesn't exist then manually create it.

### If you do not reach BUILD SUCCESSFUL during the building process
Most likely the Workspace was not created properly, the build cache is on automatic garbage collection mode, or the contents of the *BuildSrc* folder is missing.
Ensuring that *buildSrc* is not in fact empty. You can try building the CIWorkspace (or the Workspace used for automatic compiling by GitLab), and seeing if that allows the mod to compile by doing:
On Windows:
```
gradlew.bat setupCIWorkspace -g TEST_CACHE
```
On macOS/Linux
```
./gradlew setupCIWorkspace -g TEST_CACHE
```
and try rebuilding by using:
On Windows:
```
gradlew.bat build -g TEST_CACHE_BUILD
```
On macOS/Linux:
```
./gradlew build -g TEST_CACHE_BUILD
```



# In-game/running Troubleshooting

### Where on earth am I?
As of current, when you spawn you will not be anywhere near any real-world position. You will need to first teleport yourself to a world location, here are some example locations that might be of interest:

You can use some of our [Cool locations](COOL_LOCATIONS.md) to find somewhere interesting to teleport to.

### " java.lang.OutOfMemoryError: GC overhead limit exceeded", what is that, how do I fix?
You exceeded the amount of memory that the JVM (Minecraft's running environment) has to use, there are a lot of chunks in the cubic space, and a lot of blocks in those chunks. Especially considering that you will most likely be moving great lengths to get anywhere, you will a lot of memory available to the game to run properly.

Inside of Minecraft's Launcher, go to the Installations tab, find your installation of Forge, and select Edit:

![Editing JVM](Pictures/InstallationsEdit.png)

Select **"More Options"**, and find the **"JVM Arguments"** section:

![Editing JVM](Pictures\EditJVMArgs.png)

Adding or changing the following value:

```
-Xmx####
```
will drastically change the amount of RAM that the JVM can use.
*-Xmx1G* is 1 gigabyte
*-Xmx8G* is 8 gigabytes
*-Xmx16G* is 16 gigabytes
......and so on.