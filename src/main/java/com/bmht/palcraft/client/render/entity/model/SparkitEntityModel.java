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
import net.minecraft.util.math.MathHelper;

public class SparkitEntityModel extends SinglePartEntityModel<SparkitEntity> {
    public static final EntityModelLayer MODEL_LAYER = new EntityModelLayer(new Identifier(PalCraft.MOD_ID, "sparkit"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftBackLeg;
    private final ModelPart rightBackLeg;
    private final ModelPart tail;

    public SparkitEntityModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftBackLeg = root.getChild("left_back_leg");
        this.rightBackLeg = root.getChild("right_back_leg");
        this.tail = root.getChild("tail");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        root.addChild(
                "body",
                ModelPartBuilder.create()
                        .uv(0, 0)
                        .cuboid(-4.0F, -4.0F, -5.0F, 8.0F, 7.0F, 10.0F, Dilation.NONE),
                ModelTransform.pivot(0.0F, 17.0F, 0.0F)
        );
        ModelPartData head = root.addChild(
                "head",
                ModelPartBuilder.create()
                        .uv(0, 17)
                        .cuboid(-3.5F, -3.5F, -5.0F, 7.0F, 6.0F, 5.0F, Dilation.NONE),
                ModelTransform.pivot(0.0F, 14.5F, -4.5F)
        );
        head.addChild(
                "left_ear",
                ModelPartBuilder.create()
                        .uv(32, 0)
                        .cuboid(0.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F, Dilation.NONE),
                ModelTransform.pivot(1.5F, -3.0F, -2.5F)
        );
        head.addChild(
                "right_ear",
                ModelPartBuilder.create()
                        .uv(32, 0)
                        .cuboid(-2.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F, Dilation.NONE),
                ModelTransform.pivot(-1.5F, -3.0F, -2.5F)
        );
        root.addChild(
                "left_front_leg",
                ModelPartBuilder.create().uv(28, 16).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, Dilation.NONE),
                ModelTransform.pivot(2.5F, 19.0F, -3.0F)
        );
        root.addChild(
                "right_front_leg",
                ModelPartBuilder.create().uv(28, 16).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, Dilation.NONE),
                ModelTransform.pivot(-2.5F, 19.0F, -3.0F)
        );
        root.addChild(
                "left_back_leg",
                ModelPartBuilder.create().uv(28, 16).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, Dilation.NONE),
                ModelTransform.pivot(2.5F, 19.0F, 3.0F)
        );
        root.addChild(
                "right_back_leg",
                ModelPartBuilder.create().uv(28, 16).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, Dilation.NONE),
                ModelTransform.pivot(-2.5F, 19.0F, 3.0F)
        );
        root.addChild(
                "tail",
                ModelPartBuilder.create().uv(40, 12).cuboid(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 6.0F, Dilation.NONE),
                ModelTransform.pivot(0.0F, 16.0F, 4.0F)
        );

        return TexturedModelData.of(modelData, 64, 32);
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }

    @Override
    public void setAngles(SparkitEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
        this.head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;
        this.tail.yaw = MathHelper.cos(animationProgress * 0.18F) * 0.25F;

        float legSwing = limbDistance * 0.8F;
        this.leftFrontLeg.pitch = MathHelper.cos(limbAngle * 0.6662F) * legSwing;
        this.rightFrontLeg.pitch = MathHelper.cos(limbAngle * 0.6662F + MathHelper.PI) * legSwing;
        this.leftBackLeg.pitch = MathHelper.cos(limbAngle * 0.6662F + MathHelper.PI) * legSwing;
        this.rightBackLeg.pitch = MathHelper.cos(limbAngle * 0.6662F) * legSwing;
    }
}
