package com.bmht.palcraft.entity;

import com.bmht.palcraft.capture.CaptureResult;
import com.bmht.palcraft.capture.CaptureService;
import com.bmht.palcraft.registry.ModEntities;
import com.bmht.palcraft.registry.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

public class CaptureOrbEntity extends ThrownItemEntity {
    private static final byte GENERIC_HIT_STATUS = 3;
    private static final byte CAPTURE_SUCCESS_STATUS = 4;
    private static final byte CAPTURE_FAILURE_STATUS = 5;

    public CaptureOrbEntity(EntityType<? extends CaptureOrbEntity> entityType, World world) {
        super(entityType, world);
    }

    public CaptureOrbEntity(World world, LivingEntity owner) {
        super(ModEntities.CAPTURE_ORB, owner, world);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.CAPTURE_ORB;
    }

    @Override
    protected float getGravity() {
        return 0.04F;
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);

        if (getWorld().isClient) {
            return;
        }

        CaptureResult result = CaptureService.tryCapture(entityHitResult.getEntity(), getOwner());
        if (!result.attempted()) {
            return;
        }

        getWorld().sendEntityStatus(this, result.successful() ? CAPTURE_SUCCESS_STATUS : CAPTURE_FAILURE_STATUS);
        discard();
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);

        if (!getWorld().isClient && !isRemoved()) {
            getWorld().sendEntityStatus(this, GENERIC_HIT_STATUS);
            discard();
        }
    }

    @Override
    public void handleStatus(byte status) {
        if (status == GENERIC_HIT_STATUS) {
            spawnParticles(ParticleTypes.ELECTRIC_SPARK, 12, 0.35D, 0.25D);
        } else if (status == CAPTURE_SUCCESS_STATUS) {
            spawnParticles(ParticleTypes.HAPPY_VILLAGER, 18, 0.45D, 0.35D);
            spawnParticles(ParticleTypes.ELECTRIC_SPARK, 10, 0.30D, 0.20D);
        } else if (status == CAPTURE_FAILURE_STATUS) {
            spawnParticles(ParticleTypes.SMOKE, 12, 0.35D, 0.18D);
            spawnParticles(ParticleTypes.ELECTRIC_SPARK, 6, 0.25D, 0.15D);
        } else {
            super.handleStatus(status);
        }
    }

    private void spawnParticles(net.minecraft.particle.ParticleEffect particle, int count, double horizontalSpeed, double verticalSpeed) {
        for (int i = 0; i < count; i++) {
            getWorld().addParticle(
                    particle,
                    getX(),
                    getY(),
                    getZ(),
                    (random.nextDouble() - 0.5D) * horizontalSpeed,
                    random.nextDouble() * verticalSpeed,
                    (random.nextDouble() - 0.5D) * horizontalSpeed
            );
        }
    }
}
