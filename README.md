# Immersive Melodies in Backpacks

Put [Immersive Melodies](https://www.curseforge.com/minecraft/mc-mods/immersive-melodies) instruments in the Jukebox upgrade slot on [Sophisticated Backpacks](https://www.curseforge.com/minecraft/mc-mods/sophisticated-backpacks) or [Traveler's Backpack](https://www.curseforge.com/minecraft/mc-mods/travelers-backpack). Pick melodies from the same UI as music discs.

Forge 1.20.1, NeoForge 1.21.1, Fabric 1.20.1, Fabric 1.21.1.

## Features

- Instrument in the jukebox slot switches the picker to melodies; the disc/melody toggle hides when an instrument is loaded
- Same paginated list as vanilla discs
- Shuffle and repeat like discs
- Keeps playing through GUI close, dimension change, and relog

Fabric uses Salandora's [Sophisticated Backpacks port](https://modrinth.com/mod/sophisticated-backpacks-(unoffical-fabric-port)).

## Loaders

| Loader | Minecraft | Mod version |
|---|---|---|
| Forge | 1.20.1 | 1.0.1 |
| NeoForge | 1.21.1 | 1.0.0 |
| Fabric | 1.20.1 | 1.0.0 |
| Fabric | 1.21.1 | 1.0.0 |

## Requires

- Immersive Melodies 0.6.0+
- Sophisticated Backpacks 3.24.0+ (Core 1.3.0+) on Forge/NeoForge, or Salandora Fabric 3.23.4+ (Core 1.2.7+), and/or Traveler's Backpack 9.1.0+
- Fabric API on Fabric

## Usage

1. Jukebox upgrade in the backpack.
2. Put an Immersive Melodies instrument in the disc slot.
3. Open the picker, pick a melody, play.

## Building

```
java -classpath "gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain build
```

Branch per loader: `main` (Forge 1.20.1), `1.21.1-neoforge`, `1.20.1-fabric`, `1.21.1-fabric`. OneDrive paths may need `subst X:` so Gradle can spawn.

## License

MIT — see [LICENSE](LICENSE).
