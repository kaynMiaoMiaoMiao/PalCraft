package com.bmht.palcraft.registry;

import com.bmht.palcraft.PalCraft;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ModItemGroups {
    public static final ItemGroup PALCRAFT_ITEM_GROUP = Registry.register(
            Registries.ITEM_GROUP,
            new Identifier(PalCraft.MOD_ID, "item_group"),
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemGroup.palcraft"))
                    .icon(() -> new ItemStack(ModItems.CAPTURE_ORB))
                    .entries((context, entries) -> {
                        entries.add(ModItems.CAPTURE_ORB);
                        entries.add(ModItems.SPARKIT_SPAWN_EGG);
                        entries.add(ModItems.BASE_CORE);
                    })
                    .build()
    );

    private ModItemGroups() {
    }

    public static void registerModItemGroups() {
        PalCraft.LOGGER.info("Registering PalCraft item groups");
    }
}
