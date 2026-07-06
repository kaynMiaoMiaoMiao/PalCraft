package com.bmht.palcraft.client.render.entity.model;

import com.bmht.palcraft.PalCraft;
import com.bmht.palcraft.entity.DodoEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class DodoGeoModel extends GeoModel<DodoEntity> {
    private static final Identifier MODEL = new Identifier(PalCraft.MOD_ID, "geo/entity/dodo.geo.json");
    private static final Identifier TEXTURE = new Identifier(PalCraft.MOD_ID, "textures/entity/dodo.png");
    private static final Identifier ANIMATIONS = new Identifier(PalCraft.MOD_ID, "animations/entity/dodo.animation.json");

    @Override
    public Identifier getModelResource(DodoEntity animatable) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(DodoEntity animatable) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(DodoEntity animatable) {
        return ANIMATIONS;
    }
}
