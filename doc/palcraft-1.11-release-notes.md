# PalCraft 1.11 Release Notes

Version: 1.11.0
Status: sealed
Target Minecraft: 1.20.1
Loader: Fabric

## Scope

PalCraft 1.11 is the first sealed gameplay foundation. This version closes the first playable loop before the project moves into the 2.0 content and polish cycle.

## Completed Foundation

- Fabric 1.20.1 project setup and Gradle build pipeline.
- PalCraft creative mode item group.
- Capture orb item and throwable capture orb entity.
- Capture flow for PalCraft companion entities.
- Player companion storage through server-side persistent state.
- Companion summon, recall, death state, health persistence, and rename flow.
- Companion follow behavior and combat assistance.
- Level, experience, talent, element type, combat stats, and skill data.
- First batch of 7 base companions:
  - Flameling
  - Water Sprite
  - Sparkit
  - Wind Drake
  - Treelet
  - Icelime
  - Mudloba
- Base core block and owned base records.
- Base companion storage, deployment, recall, and assignment.
- Base work queue for mining, logging, and planting.
- Real world interaction for mining, logging, mature crop harvesting, and crop replanting.
- Visible base work behavior:
  - deployed companions receive concrete work targets;
  - companions path near the target before progress is applied;
  - work particles and work sounds play while working;
  - client model plays a generic work animation.
- Player and base management UI with server-synced state.
- Chinese and English language resources for the core feature set.

## Validation

- `./gradlew compileJava` passed.
- `./gradlew build` passed.

## Known 1.11 Limits

- Existing companion models are still simple first-pass models.
- Base work has visible action, but not yet rich job-specific animations.
- Flying, riding, breeding, advanced production chains, and boss encounters are not in 1.11.
- Purchased Nocsy Mount Pack assets are not included in 1.11.

## 2.0 Handoff

2.0 starts from the 1.11 gameplay foundation. The main goal is to replace or extend the temporary companion presentation with the purchased partner model batch, then improve gameplay pacing, UI ergonomics, base work feedback, and content depth.
