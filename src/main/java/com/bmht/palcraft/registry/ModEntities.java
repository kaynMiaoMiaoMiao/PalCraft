package com.bmht.palcraft.registry;

import com.bmht.palcraft.PalCraft;
import com.bmht.palcraft.entity.CaptureOrbEntity;
import com.bmht.palcraft.entity.DodoEntity;
import com.bmht.palcraft.entity.SparkitEntity;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.world.Heightmap;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.List;

public final class ModEntities {
    public static final EntityType<CaptureOrbEntity> CAPTURE_ORB = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(PalCraft.MOD_ID, "capture_orb"),
            FabricEntityTypeBuilder.<CaptureOrbEntity>create(SpawnGroup.MISC, CaptureOrbEntity::new)
                    .dimensions(EntityDimensions.fixed(0.25F, 0.25F))
                    .trackRangeBlocks(4)
                    .trackedUpdateRate(10)
                    .build()
    );

    public static final EntityType<SparkitEntity> FLAMELING = registerBasicPal("flameling");
    public static final EntityType<SparkitEntity> WATER_SPRITE = registerBasicPal("water_sprite");
    public static final EntityType<SparkitEntity> SPARKIT = registerBasicPal("sparkit");
    public static final EntityType<SparkitEntity> WIND_DRAKE = registerBasicPal("wind_drake");
    public static final EntityType<SparkitEntity> TREELET = registerBasicPal("treelet");
    public static final EntityType<SparkitEntity> ICELIME = registerBasicPal("icelime");
    public static final EntityType<SparkitEntity> MUDLOBA = registerBasicPal("mudloba");
    public static final EntityType<DodoEntity> DODO = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(PalCraft.MOD_ID, "dodo"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, DodoEntity::new)
                    .dimensions(EntityDimensions.fixed(0.9F, 1.4F))
                    .trackRangeBlocks(8)
                    .build()
    );

    public static final List<EntityType<? extends SparkitEntity>> BASIC_PALS = List.of(
            FLAMELING,
            WATER_SPRITE,
            SPARKIT,
            WIND_DRAKE,
            TREELET,
            ICELIME,
            MUDLOBA,
            DODO
    );

    private ModEntities() {
    }

    public static void registerModEntities() {
        for (EntityType<? extends SparkitEntity> entityType : BASIC_PALS) {
            registerPalSpawn(entityType);
        }
        PalCraft.LOGGER.info("Registering PalCraft entities");
    }

    private static <T extends SparkitEntity> void registerPalSpawn(EntityType<T> entityType) {
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
                12,
                1,
                2
        );
    }

    private static EntityType<SparkitEntity> registerBasicPal(String name) {
        return Registry.register(
                Registries.ENTITY_TYPE,
                new Identifier(PalCraft.MOD_ID, name),
                FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, SparkitEntity::new)
                        .dimensions(EntityDimensions.fixed(0.65F, 0.8F))
                        .trackRangeBlocks(8)
                        .build()
        );
    }
}
