package com.bmht.palcraft.registry;

import com.bmht.palcraft.PalCraft;
import com.bmht.palcraft.entity.CaptureOrbEntity;
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

    public static final EntityType<SparkitEntity> SPARKIT = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(PalCraft.MOD_ID, "sparkit"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, SparkitEntity::new)
                    .dimensions(EntityDimensions.fixed(0.65F, 0.8F))
                    .trackRangeBlocks(8)
                    .build()
    );

    private ModEntities() {
    }

    public static void registerModEntities() {
        FabricDefaultAttributeRegistry.register(SPARKIT, SparkitEntity.createSparkitAttributes());
        SpawnRestriction.register(
                SPARKIT,
                SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                SparkitEntity::canSpawn
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.foundInOverworld(),
                SpawnGroup.CREATURE,
                SPARKIT,
                30,
                1,
                2
        );
        PalCraft.LOGGER.info("Registering PalCraft entities");
    }
}
