# PalCraft

PalCraft is a Minecraft 1.20.1 Fabric mod project focused on original companion capture, companion growth, combat assistance, companion management, and base automation gameplay.

Version 1.11.0 is the first sealed gameplay foundation. Current features include capture orbs, multiple companion species, player companion storage, summon and recall, combat assistance, progression, element skills, base cores, base companion storage, base deployment, base work assignment, and visible base work actions.

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

Development and version planning are tracked in:

[doc/palcraft-development-plan.md](doc/palcraft-development-plan.md)

[doc/palcraft-1.11-release-notes.md](doc/palcraft-1.11-release-notes.md)

[doc/palcraft-2.0-plan.md](doc/palcraft-2.0-plan.md)

## License

PalCraft is licensed under the GNU General Public License v3.0 only. See [LICENSE](LICENSE).
