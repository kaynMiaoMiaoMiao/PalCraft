# PalCraft 2.0 Plan

Status: planning
Base version: 1.11.0
Primary goal: integrate the purchased partner asset batch, then improve gameplay depth and player experience.

## Asset Batch

Source directory:

```text
/Users/bmht/Downloads/111
```

Detected pack:

```text
Nocsy Mount Pack Vol.1
```

Available model groups:

- Dodo: ground companion, early-game mount candidate.
- Leopard: combat companion candidate.
- Lizard: earth, fire, poison, or desert companion candidate.
- Moth: flying companion candidate.
- Ram: charge, mining, hauling, or mountain companion candidate.

Each group includes adult, baby, and egg `.bbmodel` files. The `.bbmodel` files include embedded textures and animation data.

## Integration Direction

The pack is built for MythicMobs, ModelEngine, and MCPets. It is not a Fabric entity renderer format. PalCraft 2.0 should not try to manually copy those models into the existing simple `ModelPart` renderer.

Preferred path:

1. Add GeckoLib for animated entity rendering.
2. Convert one `.bbmodel` group to GeckoLib-compatible geometry, animation, and texture resources.
3. Integrate one pilot companion end to end.
4. Generalize the registry and species data so more model-backed companions can be added with less code churn.
5. Convert the remaining selected companions.

Pilot recommendation:

```text
Ram or Dodo first.
```

Reason: both are ground companions, so movement, collision, and AI are simpler than a flying Moth. Ram has a clear gameplay identity for charge, mining, and base work. Dodo has a clear identity for early-game mount and utility.

## Legal And Packaging Check

Before any purchased asset is committed into the repository or shipped in a jar:

- Confirm the purchase license allows redistribution inside a Minecraft mod jar.
- Confirm whether redistribution is allowed under this project's GPL-3.0-only license.
- If license terms are incompatible, keep assets local-only and do not publish them with the GPL repository.

## 2.0 Milestones

### Milestone 2.0.1: Rendering Pipeline

- Add GeckoLib dependency.
- Add one converted model-backed companion.
- Register renderer, model, texture, and animations.
- Keep the old simple companions working during migration.
- `./gradlew build` must pass.

Acceptance:

- The pilot companion spawns in game with the purchased model.
- Idle and walk animations play.
- No missing texture or missing model warnings for the pilot companion.

### Milestone 2.0.2: Species System Cleanup

- Split species metadata from hardcoded entity registration where practical.
- Prepare stable identifiers for the new companion batch.
- Define element, stats, skills, spawn rules, work suitability, and display names.

Acceptance:

- Adding a new companion requires minimal repeated registry code.
- Existing 1.11 saved companion data remains readable.

### Milestone 2.0.3: New Companion Batch

- Add selected adult companions from the purchased pack.
- Add spawn eggs or PalCraft-specific acquisition items.
- Add loot tables, language entries, catchable tags, and base work suitability.
- Add companion-specific skills where they have a clear gameplay purpose.

Acceptance:

- Each new companion can spawn, be captured, be stored, be summoned, fight, and work at a base.

### Milestone 2.0.4: Gameplay Polish

- Improve capture pacing and feedback.
- Add clearer companion roles.
- Add better base work feedback and UI status.
- Reduce command dependency further.
- Improve balance for stats, talent, skills, and work speed.

Acceptance:

- A normal survival player can understand what to do without reading commands.
- Base companions visibly look busy and useful.
- Companion role differences are obvious in combat or base work.

### Milestone 2.0.5: Experience Optimization

- Profile entity counts, base scanning, and animation cost.
- Limit expensive scans and unnecessary network sync.
- Improve UI layout for larger companion collections.
- Add save-data migration notes if any persistent structure changes.

Acceptance:

- `./gradlew build` passes.
- Single-player testing remains smooth with multiple deployed companions.
- Dedicated-server behavior remains server-authoritative.

## Out Of Scope For Early 2.0

- Full breeding system.
- Large tech tree.
- Boss progression.
- Complex item production chains.
- Flying mount gameplay, unless the Moth is explicitly selected for a later 2.0 milestone.

## Immediate Next Task

Start with a technical spike:

1. Add GeckoLib.
2. Convert and integrate either Ram or Dodo as the first model-backed companion.
3. Keep the scope narrow: spawn egg, renderer, idle/walk animation, capture, summon, and recall.
