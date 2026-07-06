package com.bmht.palcraft.client.render.entity;

import com.bmht.palcraft.client.render.entity.model.GeoPalModel;
import com.bmht.palcraft.entity.GeoPalEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GeoPalEntityRenderer<T extends GeoPalEntity> extends GeoEntityRenderer<T> {
    public GeoPalEntityRenderer(EntityRendererFactory.Context renderManager, String entityName, float shadowRadius) {
        super(renderManager, new GeoPalModel<>(entityName));
        this.shadowRadius = shadowRadius;
    }
}
