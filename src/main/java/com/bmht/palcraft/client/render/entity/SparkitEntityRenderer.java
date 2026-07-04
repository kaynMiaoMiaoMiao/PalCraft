package com.bmht.palcraft.client.render.entity;

import com.bmht.palcraft.PalCraft;
import com.bmht.palcraft.client.render.entity.model.SparkitEntityModel;
import com.bmht.palcraft.entity.SparkitEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class SparkitEntityRenderer extends MobEntityRenderer<SparkitEntity, SparkitEntityModel> {
    private static final Identifier TEXTURE = new Identifier(PalCraft.MOD_ID, "textures/entity/sparkit.png");

    public SparkitEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new SparkitEntityModel(context.getPart(SparkitEntityModel.MODEL_LAYER)), 0.35F);
    }

    @Override
    public Identifier getTexture(SparkitEntity entity) {
        return TEXTURE;
    }
}
