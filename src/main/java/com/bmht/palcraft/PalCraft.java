package com.bmht.palcraft;

import com.bmht.palcraft.command.PalCraftCommands;
import com.bmht.palcraft.registry.ModItemGroups;
import com.bmht.palcraft.registry.ModItems;
import com.bmht.palcraft.registry.ModEntities;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PalCraft implements ModInitializer {
    public static final String MOD_ID = "palcraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItems.registerModItems();
        ModEntities.registerModEntities();
        ModItemGroups.registerModItemGroups();
        PalCraftCommands.register();
        LOGGER.info("Initializing PalCraft");
    }
}
