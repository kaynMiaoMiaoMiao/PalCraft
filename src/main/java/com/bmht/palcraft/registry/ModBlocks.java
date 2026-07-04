package com.bmht.palcraft.registry;

import com.bmht.palcraft.PalCraft;
import com.bmht.palcraft.block.BaseCoreBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public final class ModBlocks {
    public static final Block BASE_CORE = registerBlock(
            "base_core",
            new BaseCoreBlock(AbstractBlock.Settings.create()
                    .mapColor(MapColor.DIAMOND_BLUE)
                    .strength(3.0F, 6.0F)
                    .sounds(BlockSoundGroup.METAL)
                    .luminance(state -> 6))
    );

    private ModBlocks() {
    }

    public static void registerModBlocks() {
        PalCraft.LOGGER.info("Registering PalCraft blocks");
    }

    private static Block registerBlock(String name, Block block) {
        return Registry.register(Registries.BLOCK, new Identifier(PalCraft.MOD_ID, name), block);
    }
}
