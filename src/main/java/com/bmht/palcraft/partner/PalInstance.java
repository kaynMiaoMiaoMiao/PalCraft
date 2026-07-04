package com.bmht.palcraft.partner;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record PalInstance(
        UUID instanceUuid,
        Identifier speciesId,
        UUID ownerUuid,
        String customName,
        int level,
        float health,
        float maxHealth,
        long capturedGameTime
) {
    public static PalInstance fromCapturedEntity(LivingEntity entity, PlayerEntity owner) {
        String name = entity.hasCustomName() ? entity.getName().getString() : "";

        return new PalInstance(
                UUID.randomUUID(),
                Registries.ENTITY_TYPE.getId(entity.getType()),
                owner.getUuid(),
                name,
                1,
                entity.getHealth(),
                entity.getMaxHealth(),
                entity.getWorld().getTime()
        );
    }

    public PalInstance withHealth(float health) {
        return new PalInstance(
                instanceUuid,
                speciesId,
                ownerUuid,
                customName,
                level,
                health,
                maxHealth,
                capturedGameTime
        );
    }

    public NbtCompound writeNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("InstanceUuid", instanceUuid.toString());
        nbt.putString("SpeciesId", speciesId.toString());
        nbt.putString("OwnerUuid", ownerUuid.toString());
        nbt.putString("CustomName", customName);
        nbt.putInt("Level", level);
        nbt.putFloat("Health", health);
        nbt.putFloat("MaxHealth", maxHealth);
        nbt.putLong("CapturedGameTime", capturedGameTime);
        return nbt;
    }

    public static PalInstance fromNbt(NbtCompound nbt) {
        Identifier speciesId = Identifier.tryParse(nbt.getString("SpeciesId"));
        if (speciesId == null) {
            speciesId = new Identifier("minecraft", "pig");
        }

        return new PalInstance(
                UUID.fromString(nbt.getString("InstanceUuid")),
                speciesId,
                UUID.fromString(nbt.getString("OwnerUuid")),
                nbt.getString("CustomName"),
                nbt.getInt("Level"),
                nbt.getFloat("Health"),
                nbt.getFloat("MaxHealth"),
                nbt.getLong("CapturedGameTime")
        );
    }
}
