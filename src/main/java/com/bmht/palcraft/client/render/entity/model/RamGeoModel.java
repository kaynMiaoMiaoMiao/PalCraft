package com.bmht.palcraft.client.render.entity.model;

import com.bmht.palcraft.PalCraft;
import com.bmht.palcraft.entity.RamEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class RamGeoModel extends GeoModel<RamEntity> {
    private static final Identifier MODEL = new Identifier(PalCraft.MOD_ID, "geo/entity/ram.geo.json");
    private static final Identifier TEXTURE = new Identifier(PalCraft.MOD_ID, "textures/entity/ram.png");
    private static final Identifier ANIMATIONS = new Identifier(PalCraft.MOD_ID, "animations/entity/ram.animation.json");

    @Override
    public Identifier getModelResource(RamEntity animatable) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(RamEntity animatable) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(RamEntity animatable) {
        return ANIMATIONS;
    }
}
