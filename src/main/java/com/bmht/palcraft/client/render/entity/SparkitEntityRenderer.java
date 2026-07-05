package com.bmht.palcraft.client.render.entity;

import com.bmht.palcraft.client.render.entity.model.BasicPalEntityModel;
import com.bmht.palcraft.entity.SparkitEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public class SparkitEntityRenderer extends MobEntityRenderer<SparkitEntity, BasicPalEntityModel> {
    private final Identifier texture;

    public SparkitEntityRenderer(EntityRendererFactory.Context context, EntityModelLayer layer, Identifier texture, float shadowRadius) {
        super(context, new BasicPalEntityModel(context.getPart(layer)), shadowRadius);
        this.texture = texture;
    }

    @Override
    public Identifier getTexture(SparkitEntity entity) {
        return texture;
    }
}
