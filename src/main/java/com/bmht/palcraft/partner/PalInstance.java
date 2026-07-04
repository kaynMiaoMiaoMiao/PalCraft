package com.bmht.palcraft.partner;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record PalInstance(
        UUID instanceUuid,
        Identifier speciesId,
        UUID ownerUuid,
        String customName,
        int level,
        long experience,
        float health,
        float maxHealth,
        float attack,
        float defense,
        PalElementType elementType,
        List<PalSkill> skills,
        long capturedGameTime
) {
    private static final float BASE_SPARKIT_HEALTH = 16.0F;
    private static final float BASE_SPARKIT_ATTACK = 3.0F;
    private static final float BASE_SPARKIT_DEFENSE = 0.0F;

    public PalInstance {
        skills = List.copyOf(skills);
    }

    public static PalInstance fromCapturedEntity(LivingEntity entity, PlayerEntity owner) {
        String name = entity.hasCustomName() ? entity.getName().getString() : "";
        float maxHealth = entity.getMaxHealth();
        float attack = (float) entity.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);

        return new PalInstance(
                UUID.randomUUID(),
                Registries.ENTITY_TYPE.getId(entity.getType()),
                owner.getUuid(),
                name,
                1,
                0L,
                entity.getHealth(),
                maxHealth,
                attack,
                BASE_SPARKIT_DEFENSE,
                defaultElementType(Registries.ENTITY_TYPE.getId(entity.getType())),
                defaultSkills(Registries.ENTITY_TYPE.getId(entity.getType())),
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
                experience,
                health,
                maxHealth,
                attack,
                defense,
                elementType,
                skills,
                capturedGameTime
        );
    }

    public PalInstance withCustomName(String customName) {
        return new PalInstance(
                instanceUuid,
                speciesId,
                ownerUuid,
                customName,
                level,
                experience,
                health,
                maxHealth,
                attack,
                defense,
                elementType,
                skills,
                capturedGameTime
        );
    }

    public PalInstance withProgression(int level, long experience, float maxHealth, float attack, float defense, float health) {
        return new PalInstance(
                instanceUuid,
                speciesId,
                ownerUuid,
                customName,
                level,
                experience,
                health,
                maxHealth,
                attack,
                defense,
                elementType,
                skills,
                capturedGameTime
        );
    }

    public static long experienceToNextLevel(int level) {
        return 50L + Math.max(1, level) * 25L;
    }

    public static float maxHealthForLevel(int level) {
        return BASE_SPARKIT_HEALTH + (Math.max(1, level) - 1) * 2.0F;
    }

    public static float attackForLevel(int level) {
        return BASE_SPARKIT_ATTACK + (Math.max(1, level) - 1) * 0.6F;
    }

    public static float defenseForLevel(int level) {
        return BASE_SPARKIT_DEFENSE + (Math.max(1, level) - 1) * 0.25F;
    }

    public NbtCompound writeNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("InstanceUuid", instanceUuid.toString());
        nbt.putString("SpeciesId", speciesId.toString());
        nbt.putString("OwnerUuid", ownerUuid.toString());
        nbt.putString("CustomName", customName);
        nbt.putInt("Level", level);
        nbt.putLong("Experience", experience);
        nbt.putFloat("Health", health);
        nbt.putFloat("MaxHealth", maxHealth);
        nbt.putFloat("Attack", attack);
        nbt.putFloat("Defense", defense);
        nbt.putString("ElementType", elementType.id());
        NbtList skillList = new NbtList();
        for (PalSkill skill : skills) {
            skillList.add(NbtString.of(skill.id()));
        }
        nbt.put("Skills", skillList);
        nbt.putLong("CapturedGameTime", capturedGameTime);
        return nbt;
    }

    public static PalInstance fromNbt(NbtCompound nbt) {
        Identifier speciesId = Identifier.tryParse(nbt.getString("SpeciesId"));
        if (speciesId == null) {
            speciesId = new Identifier("minecraft", "pig");
        }

        int level = Math.max(1, nbt.getInt("Level"));
        long experience = nbt.contains("Experience") ? nbt.getLong("Experience") : 0L;
        float maxHealth = nbt.contains("MaxHealth") ? nbt.getFloat("MaxHealth") : maxHealthForLevel(level);
        float attack = nbt.contains("Attack") ? nbt.getFloat("Attack") : attackForLevel(level);
        float defense = nbt.contains("Defense") ? nbt.getFloat("Defense") : defenseForLevel(level);
        PalElementType elementType = nbt.contains("ElementType")
                ? PalElementType.fromId(nbt.getString("ElementType"))
                : defaultElementType(speciesId);
        List<PalSkill> skills = nbt.contains("Skills", NbtElement.LIST_TYPE)
                ? readSkills(nbt.getList("Skills", NbtElement.STRING_TYPE))
                : defaultSkills(speciesId);

        return new PalInstance(
                UUID.fromString(nbt.getString("InstanceUuid")),
                speciesId,
                UUID.fromString(nbt.getString("OwnerUuid")),
                nbt.getString("CustomName"),
                level,
                experience,
                nbt.getFloat("Health"),
                maxHealth,
                attack,
                defense,
                elementType,
                skills,
                nbt.getLong("CapturedGameTime")
        );
    }

    private static PalElementType defaultElementType(Identifier speciesId) {
        if ("palcraft".equals(speciesId.getNamespace()) && "sparkit".equals(speciesId.getPath())) {
            return PalElementType.ELECTRIC;
        }
        return PalElementType.NEUTRAL;
    }

    private static List<PalSkill> defaultSkills(Identifier speciesId) {
        if ("palcraft".equals(speciesId.getNamespace()) && "sparkit".equals(speciesId.getPath())) {
            return List.of(PalSkill.TACKLE, PalSkill.ENERGY_BOLT, PalSkill.SELF_REPAIR);
        }
        return List.of(PalSkill.TACKLE);
    }

    private static List<PalSkill> readSkills(NbtList skillList) {
        List<PalSkill> skills = new ArrayList<>();
        for (NbtElement skillElement : skillList) {
            skills.add(PalSkill.fromId(skillElement.asString()));
        }
        return skills.isEmpty() ? List.of(PalSkill.TACKLE) : skills;
    }
}
