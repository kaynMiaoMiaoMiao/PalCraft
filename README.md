# PalCraft

PalCraft is a Minecraft 1.20.1 Fabric mod project focused on original companion capture, companion growth, combat assistance, and later base automation gameplay.

The project is in early development. Current features include a custom creative mode tab, a capture orb item, throwable capture orb behavior, and the early capture-flow foundation that will be connected to original companion entities in later phases.

## AI Authorship Notice

Most of the code in this project is written with AI assistance. The project owner provides the design direction, reviews implementation choices, tests behavior in game, and guides follow-up development.

AI-generated code should be reviewed before release, especially for gameplay correctness, performance, multiplayer behavior, licensing, and compatibility with Minecraft/Fabric updates.

## Project Direction

- Original Minecraft companion-capture gameplay.
- Fabric mod for Minecraft 1.20.1.
- Server-authoritative logic for gameplay systems.
- Incremental development through documented milestones.
- Original assets, names, creatures, UI, and mechanics expression.

## Requirements

- Java 17 or newer
- Use the included Gradle Wrapper: `./gradlew`

## Common Commands

```sh
./gradlew build
./gradlew runClient
./gradlew runServer
```

The built mod jar will be generated under `build/libs/`.

## Documentation

The development plan is tracked in:

[doc/palcraft-development-plan.md](doc/palcraft-development-plan.md)

## License

PalCraft is licensed under the GNU General Public License v3.0 only. See [LICENSE](LICENSE).
