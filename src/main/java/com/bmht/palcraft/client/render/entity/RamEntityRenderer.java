package com.bmht.palcraft.client.render.entity;

import com.bmht.palcraft.client.render.entity.model.RamGeoModel;
import com.bmht.palcraft.entity.RamEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class RamEntityRenderer extends GeoEntityRenderer<RamEntity> {
    public RamEntityRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new RamGeoModel());
        this.shadowRadius = 0.5F;
    }
}
