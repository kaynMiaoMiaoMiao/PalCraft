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
    public static final Item SPARKIT_SPAWN_EGG = registerItem(
            "sparkit_spawn_egg",
            new SpawnEggItem(ModEntities.SPARKIT, 0xF2C84B, 0x2FE5FF, new FabricItemSettings())
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
