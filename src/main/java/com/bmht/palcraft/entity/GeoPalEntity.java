package com.bmht.palcraft.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public abstract class GeoPalEntity extends SparkitEntity implements GeoEntity {
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    protected GeoPalEntity(EntityType<? extends GeoPalEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "Move/Idle", 0, state ->
                state.setAndContinue(state.isMoving() ? movingAnimation() : idleAnimation())
        ));
        controllers.add(DefaultAnimations.genericDeathController(this));
    }

    protected RawAnimation idleAnimation() {
        return DefaultAnimations.IDLE;
    }

    protected RawAnimation movingAnimation() {
        return DefaultAnimations.WALK;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
