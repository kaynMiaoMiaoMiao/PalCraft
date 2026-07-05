package com.bmht.palcraft.partner;

import com.bmht.palcraft.PalCraft;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.List;

public enum PalSpecies {
    FLAMELING("flameling", PalElementType.FIRE, 14.0F, 3.4F, 0.1F, 1.7F, 0.65F, 0.15F, List.of(PalSkill.TACKLE, PalSkill.FIRE_SPARK)),
    WATER_SPRITE("water_sprite", PalElementType.WATER, 15.0F, 2.7F, 0.2F, 1.9F, 0.45F, 0.18F, List.of(PalSkill.TACKLE, PalSkill.WATER_BOLT, PalSkill.SELF_REPAIR)),
    SPARKIT("sparkit", PalElementType.THUNDER, 16.0F, 3.0F, 0.0F, 2.0F, 0.6F, 0.25F, List.of(PalSkill.TACKLE, PalSkill.ENERGY_BOLT, PalSkill.SELF_REPAIR)),
    WIND_DRAKE("wind_drake", PalElementType.WIND, 13.0F, 3.1F, 0.0F, 1.6F, 0.7F, 0.12F, List.of(PalSkill.TACKLE, PalSkill.WIND_CUTTER)),
    TREELET("treelet", PalElementType.WOOD, 17.0F, 2.5F, 0.3F, 2.2F, 0.4F, 0.22F, List.of(PalSkill.TACKLE, PalSkill.LEAF_SHOT, PalSkill.SELF_REPAIR)),
    ICELIME("icelime", PalElementType.ICE, 16.0F, 2.8F, 0.4F, 1.9F, 0.45F, 0.28F, List.of(PalSkill.TACKLE, PalSkill.ICE_SHARD)),
    MUDLOBA("mudloba", PalElementType.EARTH, 20.0F, 2.9F, 0.8F, 2.4F, 0.35F, 0.35F, List.of(PalSkill.TACKLE, PalSkill.MUD_SLAP));

    private final String path;
    private final PalElementType elementType;
    private final float baseHealth;
    private final float baseAttack;
    private final float baseDefense;
    private final float healthGrowth;
    private final float attackGrowth;
    private final float defenseGrowth;
    private final List<PalSkill> defaultSkills;

    PalSpecies(
            String path,
            PalElementType elementType,
            float baseHealth,
            float baseAttack,
            float baseDefense,
            float healthGrowth,
            float attackGrowth,
            float defenseGrowth,
            List<PalSkill> defaultSkills
    ) {
        this.path = path;
        this.elementType = elementType;
        this.baseHealth = baseHealth;
        this.baseAttack = baseAttack;
        this.baseDefense = baseDefense;
        this.healthGrowth = healthGrowth;
        this.attackGrowth = attackGrowth;
        this.defenseGrowth = defenseGrowth;
        this.defaultSkills = List.copyOf(defaultSkills);
    }

    public Identifier id() {
        return new Identifier(PalCraft.MOD_ID, path);
    }

    public String path() {
        return path;
    }

    public PalElementType elementType() {
        return elementType;
    }

    public List<PalSkill> defaultSkills() {
        return defaultSkills;
    }

    public float maxHealthForLevel(int level, double talent) {
        return scaledStat(baseHealth, healthGrowth, level, talent);
    }

    public float attackForLevel(int level, double talent) {
        return scaledStat(baseAttack, attackGrowth, level, talent);
    }

    public float defenseForLevel(int level, double talent) {
        return scaledStat(baseDefense, defenseGrowth, level, talent);
    }

    private static float scaledStat(float baseValue, float growthValue, int level, double talent) {
        int safeLevel = Math.max(1, level);
        double safeTalent = Math.max(0.0D, Math.min(100.0D, talent));
        double baseMultiplier = 0.9D + safeTalent * 0.0025D;
        double growthMultiplier = 0.85D + safeTalent * 0.0035D;
        return (float) (baseValue * baseMultiplier + (safeLevel - 1) * growthValue * growthMultiplier);
    }

    public static PalSpecies fromId(Identifier speciesId) {
        return Arrays.stream(values())
                .filter(species -> species.id().equals(speciesId))
                .findFirst()
                .orElse(SPARKIT);
    }

    public static boolean isKnownPal(Identifier speciesId) {
        return Arrays.stream(values()).anyMatch(species -> species.id().equals(speciesId));
    }
}
