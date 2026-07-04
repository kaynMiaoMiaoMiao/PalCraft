package com.bmht.palcraft.partner;

import com.bmht.palcraft.entity.SparkitEntity;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;

public final class PalProgressionEvents {
    private PalProgressionEvents() {
    }

    public static void register() {
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity) -> {
            if (entity instanceof SparkitEntity sparkitEntity && sparkitEntity.isSummonedPal()) {
                PlayerPalData.get(world.getServer()).grantExperienceForKill(world, sparkitEntity, killedEntity);
            }
        });
    }
}
