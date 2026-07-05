package com.bmht.palcraft.partner;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record PalInstance(
        UUID instanceUuid,
        Identifier speciesId,
        UUID ownerUuid,
        String customName,
        double talent,
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
    public PalInstance {
        talent = normalizeTalent(talent);
        skills = List.copyOf(skills);
    }

    public static PalInstance fromCapturedEntity(LivingEntity entity, PlayerEntity owner) {
        Identifier speciesId = Registries.ENTITY_TYPE.getId(entity.getType());
        PalSpecies species = PalSpecies.fromId(speciesId);
        String name = entity.hasCustomName() ? entity.getName().getString() : "";
        double talent = randomTalent(entity.getRandom().nextDouble());
        float maxHealth = maxHealthForLevel(speciesId, 1, talent);
        float healthRatio = entity.getMaxHealth() <= 0.0F ? 1.0F : entity.getHealth() / entity.getMaxHealth();
        float health = MathHelper.clamp(maxHealth * healthRatio, 1.0F, maxHealth);

        return new PalInstance(
                UUID.randomUUID(),
                speciesId,
                owner.getUuid(),
                name,
                talent,
                1,
                0L,
                health,
                maxHealth,
                attackForLevel(speciesId, 1, talent),
                defenseForLevel(speciesId, 1, talent),
                species.elementType(),
                species.defaultSkills(),
                entity.getWorld().getTime()
        );
    }

    public PalInstance withHealth(float health) {
        return new PalInstance(
                instanceUuid,
                speciesId,
                ownerUuid,
                customName,
                talent,
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
                talent,
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
                talent,
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
        return maxHealthForLevel(PalSpecies.SPARKIT.id(), level, 50.0D);
    }

    public static float attackForLevel(int level) {
        return attackForLevel(PalSpecies.SPARKIT.id(), level, 50.0D);
    }

    public static float defenseForLevel(int level) {
        return defenseForLevel(PalSpecies.SPARKIT.id(), level, 50.0D);
    }

    public static float maxHealthForLevel(Identifier speciesId, int level, double talent) {
        return PalSpecies.fromId(speciesId).maxHealthForLevel(level, talent);
    }

    public static float attackForLevel(Identifier speciesId, int level, double talent) {
        return PalSpecies.fromId(speciesId).attackForLevel(level, talent);
    }

    public static float defenseForLevel(Identifier speciesId, int level, double talent) {
        return PalSpecies.fromId(speciesId).defenseForLevel(level, talent);
    }

    public static double normalizeTalent(double talent) {
        return Math.round(MathHelper.clamp(talent, 0.0D, 100.0D) * 100.0D) / 100.0D;
    }

    private static double randomTalent(double randomValue) {
        return normalizeTalent(randomValue * 100.0D);
    }

    public NbtCompound writeNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("InstanceUuid", instanceUuid.toString());
        nbt.putString("SpeciesId", speciesId.toString());
        nbt.putString("OwnerUuid", ownerUuid.toString());
        nbt.putString("CustomName", customName);
        nbt.putDouble("Talent", talent);
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
            speciesId = PalSpecies.SPARKIT.id();
        }

        int level = Math.max(1, nbt.getInt("Level"));
        double talent = nbt.contains("Talent") ? normalizeTalent(nbt.getDouble("Talent")) : 50.0D;
        long experience = nbt.contains("Experience") ? nbt.getLong("Experience") : 0L;
        float maxHealth = nbt.contains("MaxHealth") ? nbt.getFloat("MaxHealth") : maxHealthForLevel(speciesId, level, talent);
        float attack = nbt.contains("Attack") ? nbt.getFloat("Attack") : attackForLevel(speciesId, level, talent);
        float defense = nbt.contains("Defense") ? nbt.getFloat("Defense") : defenseForLevel(speciesId, level, talent);
        PalElementType elementType = nbt.contains("ElementType")
                ? PalElementType.fromId(nbt.getString("ElementType"))
                : PalSpecies.fromId(speciesId).elementType();
        List<PalSkill> skills = nbt.contains("Skills", NbtElement.LIST_TYPE)
                ? readSkills(nbt.getList("Skills", NbtElement.STRING_TYPE))
                : PalSpecies.fromId(speciesId).defaultSkills();

        return new PalInstance(
                UUID.fromString(nbt.getString("InstanceUuid")),
                speciesId,
                UUID.fromString(nbt.getString("OwnerUuid")),
                nbt.getString("CustomName"),
                talent,
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

    private static List<PalSkill> readSkills(NbtList skillList) {
        List<PalSkill> skills = new ArrayList<>();
        for (NbtElement skillElement : skillList) {
            skills.add(PalSkill.fromId(skillElement.asString()));
        }
        return skills.isEmpty() ? List.of(PalSkill.TACKLE) : skills;
    }
}
