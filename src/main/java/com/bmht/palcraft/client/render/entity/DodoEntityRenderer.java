package com.bmht.palcraft.client.render.entity;

import com.bmht.palcraft.client.render.entity.model.DodoGeoModel;
import com.bmht.palcraft.entity.DodoEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DodoEntityRenderer extends GeoEntityRenderer<DodoEntity> {
    public DodoEntityRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new DodoGeoModel());
        this.shadowRadius = 0.45F;
    }
}
