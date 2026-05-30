# Immersive Melodies in Backpacks

Fabric 1.20.1 mod that lets you put [Immersive Melodies](https://www.curseforge.com/minecraft/mc-mods/immersive-melodies) instruments into the Jukebox upgrade slot of Sophisticated Backpacks or [Traveler's Backpack](https://www.curseforge.com/minecraft/mc-mods/travelers-backpack), then pick songs through a small in-GUI picker.

Sophisticated Backpacks on Fabric is Salandora's [unofficial port](https://modrinth.com/mod/sophisticated-backpacks-(unoffical-fabric-port)).

## Requirements

- Minecraft 1.20.1
- Fabric Loader 0.15+ and Fabric API
- Immersive Melodies 0.6.0+
- At least one of:
  - Sophisticated Backpacks (unofficial Fabric port) 3.23.4+ with Sophisticated Core 1.2.7+
  - Traveler's Backpack 9.1.0+

## Usage

1. Install a Jukebox upgrade in your backpack.
2. Drop an Immersive Melodies instrument into the jukebox disc slot.
3. Open the picker, choose a melody, press play.
4. Shuffle and repeat work the same way they do for music discs.

## Building

JDK 21 (Loom 1.12 needs it to run, even though the mod targets Java 17).

```
java -classpath "gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain build
```

Output: `build/libs/bpmelodies-1.0.0.jar`.

## License

MIT — see [LICENSE](LICENSE).
