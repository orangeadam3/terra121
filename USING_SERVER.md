

# Server Run Instructions

After completing the [Build Instructions](BUILD_INSTRUCTIONS.md). Using a Forge server (can be installed through [standard Forge installer](http://files.minecraftforge.net/)), run the server once completely (enable the EULA, and generate a new world), move CubicChunks, CubicWorldGen, and the Terra 1-to-1 jar to the server's mod folder.

- Inside of the server.properties file change:

```properties
level-type=default
```

to 

```properties
level-type=EarthCubic
```

- change the name of the 

```properties
level-name=world
```

to another name, so a new world will be generated from the beginning with the Terra 1-to-1 generator. (Not changing the name will keep the previous generation type of the world, which is default)

- **RECOMMENDED**: After re-running your server for the second time, you should now have a Terra 1-to-1 world loaded and generating properly, however to limit load on the server side, change the:

  ```properties
  view-distance=16
  ```

  and

  ```properties
  vertical-view-distance=-1
  ```

  to a number of chunks that your server can handle, and re-run your server (using -1 is whatever the player's vertical distance is, for all players), you may will need to mess with these numbers until you get a stable server setting.

We also recommend launching your Forge Server allocating more RAM in the JVM arguments:

```bash
<JAVA_HOME>/bin/java -Xmx####(suffix) -jar forge-<MCVERSION>-<FORGEVERSION>.jar
```

**(Remember using a capital letter as the suffix is different from using a lowercase letter when specifying memory size)**:

**-Xmx1G** is 1 gigabyte

**-Xmx8G** is 8 gigabytes

**-Xmx16G** is 16 gigabytes

......and so on.

However,

**-Xmx1g** is 1 gigabit (125 Megabytes)

**-Xmx8g** is 8 gigabit (1 Gigabyte)

**-Xmx16g** is 16 gigabit (2 Gigabytes, default Minecraft memory)
......and so on.



Join the server (ensure you have CubicChunks loaded in your client at least when joining), if you end up on Null Island upon spawning, then you have properly gotten Terra 1-to-1 loaded on your server,   assuming you have Server operator power, try teleporting to a [real-world location](COOL_LOCATIONS.md).
