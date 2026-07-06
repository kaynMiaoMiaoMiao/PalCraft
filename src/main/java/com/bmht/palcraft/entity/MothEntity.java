package com.bmht.palcraft.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.world.World;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.core.animation.RawAnimation;

public class MothEntity extends GeoPalEntity {
    public MothEntity(EntityType<? extends MothEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected RawAnimation movingAnimation() {
        return DefaultAnimations.FLY;
    }
}
