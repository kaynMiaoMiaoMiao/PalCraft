package com.bmht.palcraft.client.render.entity.model;

import com.bmht.palcraft.PalCraft;
import com.bmht.palcraft.entity.SparkitEntity;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.Identifier;

public class BasicPalEntityModel extends SinglePartEntityModel<SparkitEntity> {
    public static final EntityModelLayer FLAMELING_LAYER = layer("flameling");
    public static final EntityModelLayer WATER_SPRITE_LAYER = layer("water_sprite");
    public static final EntityModelLayer SPARKIT_LAYER = layer("sparkit_basic");
    public static final EntityModelLayer WIND_DRAKE_LAYER = layer("wind_drake");
    public static final EntityModelLayer TREELET_LAYER = layer("treelet");
    public static final EntityModelLayer ICELIME_LAYER = layer("icelime");
    public static final EntityModelLayer MUDLOBA_LAYER = layer("mudloba");

    private final ModelPart root;

    public BasicPalEntityModel(ModelPart root) {
        this.root = root;
    }

    public static TexturedModelData flamelingModel() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        root.addChild("body", ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -5.0F, -3.0F, 8.0F, 8.0F, 6.0F, Dilation.NONE), ModelTransform.pivot(0.0F, 19.0F, 0.0F));
        root.addChild("flame", ModelPartBuilder.create().uv(28, 0).cuboid(-2.0F, -8.0F, -1.0F, 4.0F, 8.0F, 2.0F, Dilation.NONE), ModelTransform.pivot(0.0F, 14.0F, 0.0F));
        return TexturedModelData.of(data, 64, 32);
    }

    public static TexturedModelData waterSpriteModel() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        root.addChild("body", ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F, Dilation.NONE), ModelTransform.pivot(0.0F, 18.0F, 0.0F));
        root.addChild("crest", ModelPartBuilder.create().uv(32, 0).cuboid(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F, Dilation.NONE), ModelTransform.pivot(0.0F, 14.0F, -1.0F));
        root.addChild("tail", ModelPartBuilder.create().uv(32, 10).cuboid(-1.0F, -2.0F, 0.0F, 2.0F, 4.0F, 5.0F, Dilation.NONE), ModelTransform.pivot(0.0F, 18.0F, 3.0F));
        return TexturedModelData.of(data, 64, 32);
    }

    public static TexturedModelData sparkitModel() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        root.addChild("body", ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -4.0F, -4.0F, 8.0F, 7.0F, 8.0F, Dilation.NONE), ModelTransform.pivot(0.0F, 17.0F, 0.0F));
        root.addChild("left_spike", ModelPartBuilder.create().uv(32, 0).cuboid(0.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F, Dilation.NONE), ModelTransform.pivot(1.5F, 13.0F, -2.0F));
        root.addChild("right_spike", ModelPartBuilder.create().uv(32, 0).cuboid(-2.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F, Dilation.NONE), ModelTransform.pivot(-1.5F, 13.0F, -2.0F));
        root.addChild("tail", ModelPartBuilder.create().uv(40, 10).cuboid(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 6.0F, Dilation.NONE), ModelTransform.pivot(0.0F, 16.0F, 3.0F));
        return TexturedModelData.of(data, 64, 32);
    }

    public static TexturedModelData windDrakeModel() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        root.addChild("body", ModelPartBuilder.create().uv(0, 0).cuboid(-3.0F, -3.0F, -6.0F, 6.0F, 5.0F, 12.0F, Dilation.NONE), ModelTransform.pivot(0.0F, 17.0F, 0.0F));
        root.addChild("left_wing", ModelPartBuilder.create().uv(30, 0).cuboid(0.0F, -1.0F, -4.0F, 8.0F, 1.0F, 7.0F, Dilation.NONE), ModelTransform.pivot(3.0F, 15.5F, -1.0F));
        root.addChild("right_wing", ModelPartBuilder.create().uv(30, 0).cuboid(-8.0F, -1.0F, -4.0F, 8.0F, 1.0F, 7.0F, Dilation.NONE), ModelTransform.pivot(-3.0F, 15.5F, -1.0F));
        root.addChild("tail", ModelPartBuilder.create().uv(0, 20).cuboid(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 8.0F, Dilation.NONE), ModelTransform.pivot(0.0F, 16.0F, 5.0F));
        return TexturedModelData.of(data, 64, 32);
    }

    public static TexturedModelData treeletModel() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        root.addChild("trunk", ModelPartBuilder.create().uv(0, 0).cuboid(-3.0F, -6.0F, -3.0F, 6.0F, 8.0F, 6.0F, Dilation.NONE), ModelTransform.pivot(0.0F, 22.0F, 0.0F));
        root.addChild("leaf_crown", ModelPartBuilder.create().uv(24, 0).cuboid(-5.0F, -5.0F, -5.0F, 10.0F, 5.0F, 10.0F, Dilation.NONE), ModelTransform.pivot(0.0F, 14.0F, 0.0F));
        root.addChild("left_root", ModelPartBuilder.create().uv(0, 18).cuboid(0.0F, -1.0F, -1.0F, 4.0F, 2.0F, 2.0F, Dilation.NONE), ModelTransform.pivot(2.0F, 23.0F, 0.0F));
        root.addChild("right_root", ModelPartBuilder.create().uv(0, 18).cuboid(-4.0F, -1.0F, -1.0F, 4.0F, 2.0F, 2.0F, Dilation.NONE), ModelTransform.pivot(-2.0F, 23.0F, 0.0F));
        return TexturedModelData.of(data, 64, 32);
    }

    public static TexturedModelData icelimeModel() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        root.addChild("body", ModelPartBuilder.create().uv(0, 0).cuboid(-5.0F, -5.0F, -5.0F, 10.0F, 9.0F, 10.0F, new Dilation(0.2F)), ModelTransform.pivot(0.0F, 19.0F, 0.0F));
        root.addChild("top_crystal", ModelPartBuilder.create().uv(40, 0).cuboid(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F, Dilation.NONE), ModelTransform.pivot(0.0F, 14.0F, 0.0F));
        return TexturedModelData.of(data, 64, 32);
    }

    public static TexturedModelData mudlobaModel() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        root.addChild("body", ModelPartBuilder.create().uv(0, 0).cuboid(-5.0F, -5.0F, -4.0F, 10.0F, 8.0F, 8.0F, Dilation.NONE), ModelTransform.pivot(0.0F, 19.0F, 0.0F));
        root.addChild("left_arm", ModelPartBuilder.create().uv(36, 0).cuboid(0.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F, Dilation.NONE), ModelTransform.pivot(4.0F, 18.0F, 0.0F));
        root.addChild("right_arm", ModelPartBuilder.create().uv(36, 0).cuboid(-4.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F, Dilation.NONE), ModelTransform.pivot(-4.0F, 18.0F, 0.0F));
        root.addChild("head_lump", ModelPartBuilder.create().uv(0, 18).cuboid(-3.0F, -3.0F, -3.0F, 6.0F, 3.0F, 6.0F, Dilation.NONE), ModelTransform.pivot(0.0F, 14.0F, 0.0F));
        return TexturedModelData.of(data, 64, 32);
    }

    @Override
    public ModelPart getPart() {
        return root;
    }

    @Override
    public void setAngles(SparkitEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
    }

    private static EntityModelLayer layer(String path) {
        return new EntityModelLayer(new Identifier(PalCraft.MOD_ID, path), "main");
    }
}
