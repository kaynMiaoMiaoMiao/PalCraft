package com.bmht.palcraft.registry;

import com.bmht.palcraft.PalCraft;
import com.bmht.palcraft.item.CaptureOrbItem;
import com.bmht.palcraft.partner.PalSpecies;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ModItems {
    public static final Item CAPTURE_ORB = registerItem(
            "capture_orb",
            new CaptureOrbItem(new FabricItemSettings().maxCount(16))
    );
    private static final Map<PalSpecies, Item> SPAWN_EGGS_BY_SPECIES = registerSpawnEggs();

    public static final Item FLAMELING_SPAWN_EGG = spawnEgg(PalSpecies.FLAMELING);
    public static final Item WATER_SPRITE_SPAWN_EGG = spawnEgg(PalSpecies.WATER_SPRITE);
    public static final Item SPARKIT_SPAWN_EGG = spawnEgg(PalSpecies.SPARKIT);
    public static final Item WIND_DRAKE_SPAWN_EGG = spawnEgg(PalSpecies.WIND_DRAKE);
    public static final Item TREELET_SPAWN_EGG = spawnEgg(PalSpecies.TREELET);
    public static final Item ICELIME_SPAWN_EGG = spawnEgg(PalSpecies.ICELIME);
    public static final Item MUDLOBA_SPAWN_EGG = spawnEgg(PalSpecies.MUDLOBA);
    public static final Item DODO_SPAWN_EGG = spawnEgg(PalSpecies.DODO);
    public static final Item BASE_CORE = registerItem(
            "base_core",
            new BlockItem(ModBlocks.BASE_CORE, new FabricItemSettings())
    );

    private ModItems() {
    }

    public static void registerModItems() {
        PalCraft.LOGGER.info("Registering PalCraft items");
    }

    public static List<Item> spawnEggs() {
        return List.copyOf(SPAWN_EGGS_BY_SPECIES.values());
    }

    private static Map<PalSpecies, Item> registerSpawnEggs() {
        Map<PalSpecies, Item> spawnEggs = new EnumMap<>(PalSpecies.class);
        for (PalSpecies species : PalSpecies.registeredValues()) {
            spawnEggs.put(
                    species,
                    registerItem(
                            species.spawnEggItemPath(),
                            new SpawnEggItem(
                                    ModEntities.getPalEntityType(species),
                                    species.spawnEggPrimaryColor(),
                                    species.spawnEggSecondaryColor(),
                                    new FabricItemSettings()
                            )
                    )
            );
        }
        return Collections.unmodifiableMap(spawnEggs);
    }

    private static Item spawnEgg(PalSpecies species) {
        Item item = SPAWN_EGGS_BY_SPECIES.get(species);
        if (item == null) {
            throw new IllegalArgumentException("Pal species does not have a spawn egg: " + species.id());
        }
        return item;
    }

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(PalCraft.MOD_ID, name), item);
    }
}
