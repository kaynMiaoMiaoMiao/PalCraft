package com.bmht.palcraft.registry.tag;

import com.bmht.palcraft.PalCraft;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public final class ModEntityTypeTags {
    public static final TagKey<EntityType<?>> CATCHABLE = TagKey.of(
            RegistryKeys.ENTITY_TYPE,
            new Identifier(PalCraft.MOD_ID, "catchable")
    );

    private ModEntityTypeTags() {
    }
}
