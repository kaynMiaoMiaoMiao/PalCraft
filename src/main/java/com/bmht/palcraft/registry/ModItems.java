package com.bmht.palcraft.registry;

import com.bmht.palcraft.PalCraft;
import com.bmht.palcraft.item.CaptureOrbItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModItems {
    public static final Item CAPTURE_ORB = registerItem(
            "capture_orb",
            new CaptureOrbItem(new FabricItemSettings().maxCount(16))
    );
    public static final Item FLAMELING_SPAWN_EGG = registerItem(
            "flameling_spawn_egg",
            new SpawnEggItem(ModEntities.FLAMELING, 0xFF6A2A, 0xFFD45A, new FabricItemSettings())
    );
    public static final Item WATER_SPRITE_SPAWN_EGG = registerItem(
            "water_sprite_spawn_egg",
            new SpawnEggItem(ModEntities.WATER_SPRITE, 0x3BA7FF, 0xB8F4FF, new FabricItemSettings())
    );
    public static final Item SPARKIT_SPAWN_EGG = registerItem(
            "sparkit_spawn_egg",
            new SpawnEggItem(ModEntities.SPARKIT, 0xF2C84B, 0x2FE5FF, new FabricItemSettings())
    );
    public static final Item WIND_DRAKE_SPAWN_EGG = registerItem(
            "wind_drake_spawn_egg",
            new SpawnEggItem(ModEntities.WIND_DRAKE, 0xD8F7E4, 0x72C7A5, new FabricItemSettings())
    );
    public static final Item TREELET_SPAWN_EGG = registerItem(
            "treelet_spawn_egg",
            new SpawnEggItem(ModEntities.TREELET, 0x4EA348, 0xD8B36A, new FabricItemSettings())
    );
    public static final Item ICELIME_SPAWN_EGG = registerItem(
            "icelime_spawn_egg",
            new SpawnEggItem(ModEntities.ICELIME, 0xC8F7FF, 0x5CA7E8, new FabricItemSettings())
    );
    public static final Item MUDLOBA_SPAWN_EGG = registerItem(
            "mudloba_spawn_egg",
            new SpawnEggItem(ModEntities.MUDLOBA, 0x8A6145, 0xC7A26A, new FabricItemSettings())
    );
    public static final Item DODO_SPAWN_EGG = registerItem(
            "dodo_spawn_egg",
            new SpawnEggItem(ModEntities.DODO, 0xB98B54, 0x3F2A1B, new FabricItemSettings())
    );
    public static final Item BASE_CORE = registerItem(
            "base_core",
            new BlockItem(ModBlocks.BASE_CORE, new FabricItemSettings())
    );

    private ModItems() {
    }

    public static void registerModItems() {
        PalCraft.LOGGER.info("Registering PalCraft items");
    }

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(PalCraft.MOD_ID, name), item);
    }
}
