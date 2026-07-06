package com.bmht.palcraft.client.render.entity.model;

import com.bmht.palcraft.PalCraft;
import com.bmht.palcraft.entity.GeoPalEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class GeoPalModel<T extends GeoPalEntity> extends GeoModel<T> {
    private final Identifier model;
    private final Identifier texture;
    private final Identifier animations;

    public GeoPalModel(String entityName) {
        this.model = new Identifier(PalCraft.MOD_ID, "geo/entity/" + entityName + ".geo.json");
        this.texture = new Identifier(PalCraft.MOD_ID, "textures/entity/" + entityName + ".png");
        this.animations = new Identifier(PalCraft.MOD_ID, "animations/entity/" + entityName + ".animation.json");
    }

    @Override
    public Identifier getModelResource(T animatable) {
        return model;
    }

    @Override
    public Identifier getTextureResource(T animatable) {
        return texture;
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        return animations;
    }
}
