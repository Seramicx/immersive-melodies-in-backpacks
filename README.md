# Immersive Melodies in Backpacks

Forge 1.20.1 mod that lets you put [Immersive Melodies](https://www.curseforge.com/minecraft/mc-mods/immersive-melodies) instruments into the Jukebox upgrade slot of [Sophisticated Backpacks](https://www.curseforge.com/minecraft/mc-mods/sophisticated-backpacks) or [Traveler's Backpack](https://www.curseforge.com/minecraft/mc-mods/travelers-backpack), then pick songs through a small in-GUI picker.

## Requirements

- Minecraft 1.20.1
- Forge 47+
- Immersive Melodies 0.6.0+
- At least one of:
  - Sophisticated Backpacks 3.24.0+ (with Sophisticated Core 1.3.0+)
  - Traveler's Backpack 9.1.0+

## Usage

1. Install a Jukebox upgrade in your backpack.
2. Drop an Immersive Melodies instrument into the jukebox disc slot.
3. Open the picker, choose a melody, press play.
4. Shuffle and repeat work the same way they do for music discs.

## Building

```
java -classpath "gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain build
```

Output: `build/libs/bpmelodies-1.0.0.jar`.

## License

MIT — see [LICENSE](LICENSE).
