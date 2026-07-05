package com.bmht.palcraft.partner;

import java.util.Locale;

public enum PalSkill {
    TACKLE(60, PalElementType.NEUTRAL, false),
    FIRE_SPARK(90, PalElementType.FIRE, true),
    WATER_BOLT(100, PalElementType.WATER, true),
    ENERGY_BOLT(80, PalElementType.THUNDER, true),
    WIND_CUTTER(85, PalElementType.WIND, true),
    LEAF_SHOT(95, PalElementType.WOOD, true),
    ICE_SHARD(105, PalElementType.ICE, true),
    MUD_SLAP(100, PalElementType.EARTH, true),
    DRAGON_BREATH(140, PalElementType.DRAGON, true),
    SELF_REPAIR(220, PalElementType.WOOD, false);

    private final int cooldownTicks;
    private final PalElementType elementType;
    private final boolean rangedAttack;

    PalSkill(int cooldownTicks, PalElementType elementType, boolean rangedAttack) {
        this.cooldownTicks = cooldownTicks;
        this.elementType = elementType;
        this.rangedAttack = rangedAttack;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public PalElementType elementType() {
        return elementType;
    }

    public boolean isRangedAttack() {
        return rangedAttack;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static PalSkill fromId(String id) {
        for (PalSkill skill : values()) {
            if (skill.id().equals(id)) {
                return skill;
            }
        }
        return TACKLE;
    }
}
