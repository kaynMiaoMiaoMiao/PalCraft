package com.bmht.palcraft.registry;

import com.bmht.palcraft.PalCraft;
import com.bmht.palcraft.entity.CaptureOrbEntity;
import com.bmht.palcraft.entity.DodoEntity;
import com.bmht.palcraft.entity.RamEntity;
import com.bmht.palcraft.entity.SparkitEntity;
import com.bmht.palcraft.partner.PalSpecies;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.world.Heightmap;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ModEntities {
    public static final EntityType<CaptureOrbEntity> CAPTURE_ORB = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(PalCraft.MOD_ID, "capture_orb"),
            FabricEntityTypeBuilder.<CaptureOrbEntity>create(SpawnGroup.MISC, CaptureOrbEntity::new)
                    .dimensions(net.minecraft.entity.EntityDimensions.fixed(0.25F, 0.25F))
                    .trackRangeBlocks(4)
                    .trackedUpdateRate(10)
                    .build()
    );

    private static final Map<PalSpecies, EntityType<? extends SparkitEntity>> PALS_BY_SPECIES = registerPalEntities();

    public static final EntityType<SparkitEntity> FLAMELING = basicPal(PalSpecies.FLAMELING);
    public static final EntityType<SparkitEntity> WATER_SPRITE = basicPal(PalSpecies.WATER_SPRITE);
    public static final EntityType<SparkitEntity> SPARKIT = basicPal(PalSpecies.SPARKIT);
    public static final EntityType<SparkitEntity> WIND_DRAKE = basicPal(PalSpecies.WIND_DRAKE);
    public static final EntityType<SparkitEntity> TREELET = basicPal(PalSpecies.TREELET);
    public static final EntityType<SparkitEntity> ICELIME = basicPal(PalSpecies.ICELIME);
    public static final EntityType<SparkitEntity> MUDLOBA = basicPal(PalSpecies.MUDLOBA);
    public static final EntityType<DodoEntity> DODO = dodoPal();
    public static final EntityType<RamEntity> RAM = ramPal();

    public static final List<EntityType<? extends SparkitEntity>> BASIC_PALS = List.copyOf(PALS_BY_SPECIES.values());

    private ModEntities() {
    }

    public static void registerModEntities() {
        for (PalSpecies species : PalSpecies.registeredValues()) {
            registerPalSpawn(species, getPalEntityType(species));
        }
        PalCraft.LOGGER.info("Registering PalCraft entities");
    }

    public static EntityType<? extends SparkitEntity> getPalEntityType(PalSpecies species) {
        EntityType<? extends SparkitEntity> entityType = PALS_BY_SPECIES.get(species);
        if (entityType == null) {
            throw new IllegalArgumentException("Pal species is not registered as an entity: " + species.id());
        }
        return entityType;
    }

    private static <T extends SparkitEntity> void registerPalSpawn(PalSpecies species, EntityType<T> entityType) {
        FabricDefaultAttributeRegistry.register(entityType, SparkitEntity.createSparkitAttributes());
        SpawnRestriction.register(
                entityType,
                SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                SparkitEntity::canSpawn
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.foundInOverworld(),
                SpawnGroup.CREATURE,
                entityType,
                species.spawnWeight(),
                species.minGroupSize(),
                species.maxGroupSize()
        );
    }

    private static Map<PalSpecies, EntityType<? extends SparkitEntity>> registerPalEntities() {
        Map<PalSpecies, EntityType<? extends SparkitEntity>> entities = new EnumMap<>(PalSpecies.class);
        for (PalSpecies species : PalSpecies.registeredValues()) {
            EntityType<? extends SparkitEntity> entityType = switch (species) {
                case DODO -> registerPal(species, DodoEntity::new);
                case RAM -> registerPal(species, RamEntity::new);
                default -> registerPal(species, SparkitEntity::new);
            };
            entities.put(species, entityType);
        }
        return Collections.unmodifiableMap(entities);
    }

    private static <T extends SparkitEntity> EntityType<T> registerPal(PalSpecies species, EntityType.EntityFactory<T> factory) {
        return Registry.register(
                Registries.ENTITY_TYPE,
                species.id(),
                FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, factory)
                        .dimensions(species.dimensions())
                        .trackRangeBlocks(8)
                        .build()
        );
    }

    @SuppressWarnings("unchecked")
    private static EntityType<SparkitEntity> basicPal(PalSpecies species) {
        return (EntityType<SparkitEntity>) getPalEntityType(species);
    }

    @SuppressWarnings("unchecked")
    private static EntityType<DodoEntity> dodoPal() {
        return (EntityType<DodoEntity>) getPalEntityType(PalSpecies.DODO);
    }

    @SuppressWarnings("unchecked")
    private static EntityType<RamEntity> ramPal() {
        return (EntityType<RamEntity>) getPalEntityType(PalSpecies.RAM);
    }
}
