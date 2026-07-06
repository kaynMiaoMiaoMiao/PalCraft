package com.bmht.palcraft.partner;

import com.bmht.palcraft.PalCraft;
import com.bmht.palcraft.base.BaseWorkType;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public enum PalSpecies {
    FLAMELING(
            "flameling",
            true,
            PalElementType.FIRE,
            14.0F,
            3.4F,
            0.1F,
            1.7F,
            0.65F,
            0.15F,
            0.65F,
            0.8F,
            12,
            1,
            2,
            0xFF6A2A,
            0xFFD45A,
            List.of(PalSkill.TACKLE, PalSkill.FIRE_SPARK),
            Map.of(BaseWorkType.MANUFACTURING, 2)
    ),
    WATER_SPRITE(
            "water_sprite",
            true,
            PalElementType.WATER,
            15.0F,
            2.7F,
            0.2F,
            1.9F,
            0.45F,
            0.18F,
            0.65F,
            0.8F,
            12,
            1,
            2,
            0x3BA7FF,
            0xB8F4FF,
            List.of(PalSkill.TACKLE, PalSkill.WATER_BOLT, PalSkill.SELF_REPAIR),
            Map.of(BaseWorkType.PLANTING, 3)
    ),
    SPARKIT(
            "sparkit",
            true,
            PalElementType.THUNDER,
            16.0F,
            3.0F,
            0.0F,
            2.0F,
            0.6F,
            0.25F,
            0.65F,
            0.8F,
            12,
            1,
            2,
            0xF2C84B,
            0x2FE5FF,
            List.of(PalSkill.TACKLE, PalSkill.ENERGY_BOLT, PalSkill.SELF_REPAIR),
            Map.of(BaseWorkType.MINING, 3, BaseWorkType.MANUFACTURING, 2)
    ),
    WIND_DRAKE(
            "wind_drake",
            true,
            PalElementType.WIND,
            13.0F,
            3.1F,
            0.0F,
            1.6F,
            0.7F,
            0.12F,
            0.65F,
            0.8F,
            12,
            1,
            2,
            0xD8F7E4,
            0x72C7A5,
            List.of(PalSkill.TACKLE, PalSkill.WIND_CUTTER),
            Map.of(BaseWorkType.HAULING, 2)
    ),
    TREELET(
            "treelet",
            true,
            PalElementType.WOOD,
            17.0F,
            2.5F,
            0.3F,
            2.2F,
            0.4F,
            0.22F,
            0.65F,
            0.8F,
            12,
            1,
            2,
            0x4EA348,
            0xD8B36A,
            List.of(PalSkill.TACKLE, PalSkill.LEAF_SHOT, PalSkill.SELF_REPAIR),
            Map.of(BaseWorkType.LOGGING, 3, BaseWorkType.PLANTING, 3)
    ),
    ICELIME(
            "icelime",
            true,
            PalElementType.ICE,
            16.0F,
            2.8F,
            0.4F,
            1.9F,
            0.45F,
            0.28F,
            0.65F,
            0.8F,
            12,
            1,
            2,
            0xC8F7FF,
            0x5CA7E8,
            List.of(PalSkill.TACKLE, PalSkill.ICE_SHARD),
            Map.of(BaseWorkType.HAULING, 2)
    ),
    MUDLOBA(
            "mudloba",
            true,
            PalElementType.EARTH,
            20.0F,
            2.9F,
            0.8F,
            2.4F,
            0.35F,
            0.35F,
            0.65F,
            0.8F,
            12,
            1,
            2,
            0x8A6145,
            0xC7A26A,
            List.of(PalSkill.TACKLE, PalSkill.MUD_SLAP),
            Map.of(BaseWorkType.MINING, 3)
    ),
    DODO(
            "dodo",
            true,
            PalElementType.NEUTRAL,
            18.0F,
            2.6F,
            0.4F,
            2.0F,
            0.35F,
            0.18F,
            0.9F,
            1.4F,
            12,
            1,
            2,
            0xB98B54,
            0x3F2A1B,
            List.of(PalSkill.TACKLE),
            Map.of(BaseWorkType.HAULING, 3)
    ),
    RAM(
            "ram",
            true,
            PalElementType.EARTH,
            22.0F,
            3.2F,
            0.9F,
            2.6F,
            0.45F,
            0.4F,
            0.9F,
            1.2F,
            10,
            1,
            2,
            0x8D7C66,
            0xE6D7B8,
            List.of(PalSkill.TACKLE, PalSkill.MUD_SLAP),
            Map.of(BaseWorkType.MINING, 3, BaseWorkType.HAULING, 3)
    ),
    LEOPARD(
            "leopard",
            false,
            PalElementType.NEUTRAL,
            18.0F,
            4.0F,
            0.2F,
            2.0F,
            0.8F,
            0.18F,
            0.8F,
            0.9F,
            8,
            1,
            1,
            0xD8A24C,
            0x2E261F,
            List.of(PalSkill.TACKLE),
            Map.of(BaseWorkType.HAULING, 2)
    ),
    LIZARD(
            "lizard",
            false,
            PalElementType.FIRE,
            19.0F,
            3.4F,
            0.5F,
            2.1F,
            0.55F,
            0.28F,
            0.75F,
            0.75F,
            10,
            1,
            2,
            0x5C8A3A,
            0xC05A2A,
            List.of(PalSkill.TACKLE, PalSkill.FIRE_SPARK),
            Map.of(BaseWorkType.MINING, 2, BaseWorkType.MANUFACTURING, 2)
    ),
    MOTH(
            "moth",
            false,
            PalElementType.WIND,
            15.0F,
            3.1F,
            0.1F,
            1.8F,
            0.65F,
            0.16F,
            0.9F,
            0.7F,
            6,
            1,
            1,
            0xDCCBC2,
            0x7B8EB8,
            List.of(PalSkill.TACKLE, PalSkill.WIND_CUTTER),
            Map.of(BaseWorkType.PLANTING, 2, BaseWorkType.HAULING, 2)
    );

    private final String path;
    private final boolean registered;
    private final PalElementType elementType;
    private final float baseHealth;
    private final float baseAttack;
    private final float baseDefense;
    private final float healthGrowth;
    private final float attackGrowth;
    private final float defenseGrowth;
    private final float width;
    private final float height;
    private final int spawnWeight;
    private final int minGroupSize;
    private final int maxGroupSize;
    private final int spawnEggPrimaryColor;
    private final int spawnEggSecondaryColor;
    private final List<PalSkill> defaultSkills;
    private final Map<BaseWorkType, Integer> workSuitability;

    PalSpecies(
            String path,
            boolean registered,
            PalElementType elementType,
            float baseHealth,
            float baseAttack,
            float baseDefense,
            float healthGrowth,
            float attackGrowth,
            float defenseGrowth,
            float width,
            float height,
            int spawnWeight,
            int minGroupSize,
            int maxGroupSize,
            int spawnEggPrimaryColor,
            int spawnEggSecondaryColor,
            List<PalSkill> defaultSkills,
            Map<BaseWorkType, Integer> workSuitability
    ) {
        this.path = path;
        this.registered = registered;
        this.elementType = elementType;
        this.baseHealth = baseHealth;
        this.baseAttack = baseAttack;
        this.baseDefense = baseDefense;
        this.healthGrowth = healthGrowth;
        this.attackGrowth = attackGrowth;
        this.defenseGrowth = defenseGrowth;
        this.width = width;
        this.height = height;
        this.spawnWeight = spawnWeight;
        this.minGroupSize = minGroupSize;
        this.maxGroupSize = maxGroupSize;
        this.spawnEggPrimaryColor = spawnEggPrimaryColor;
        this.spawnEggSecondaryColor = spawnEggSecondaryColor;
        this.defaultSkills = List.copyOf(defaultSkills);
        this.workSuitability = new EnumMap<>(BaseWorkType.class);
        this.workSuitability.putAll(workSuitability);
    }

    public Identifier id() {
        return new Identifier(PalCraft.MOD_ID, path);
    }

    public String path() {
        return path;
    }

    public boolean registered() {
        return registered;
    }

    public PalElementType elementType() {
        return elementType;
    }

    public EntityDimensions dimensions() {
        return EntityDimensions.fixed(width, height);
    }

    public int spawnWeight() {
        return spawnWeight;
    }

    public int minGroupSize() {
        return minGroupSize;
    }

    public int maxGroupSize() {
        return maxGroupSize;
    }

    public int spawnEggPrimaryColor() {
        return spawnEggPrimaryColor;
    }

    public int spawnEggSecondaryColor() {
        return spawnEggSecondaryColor;
    }

    public String translationKey() {
        return "entity." + PalCraft.MOD_ID + "." + path;
    }

    public String roleTranslationKey() {
        return "role." + PalCraft.MOD_ID + "." + path;
    }

    public String spawnEggItemPath() {
        return path + "_spawn_egg";
    }

    public List<PalSkill> defaultSkills() {
        return defaultSkills;
    }

    public int workSuitability(BaseWorkType workType) {
        return workSuitability.getOrDefault(workType, 1);
    }

    public String workSuitabilitySummary() {
        List<String> entries = Arrays.stream(BaseWorkType.assignableValues())
                .map(workType -> workType.id() + ":" + workSuitability(workType))
                .toList();
        return String.join(", ", entries);
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

    public static List<PalSpecies> registeredValues() {
        return Arrays.stream(values())
                .filter(PalSpecies::registered)
                .toList();
    }
}
