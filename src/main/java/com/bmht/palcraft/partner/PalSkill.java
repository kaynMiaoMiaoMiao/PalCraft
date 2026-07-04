package com.bmht.palcraft.partner;

import java.util.Locale;

public enum PalSkill {
    TACKLE(60),
    ENERGY_BOLT(80),
    SELF_REPAIR(220);

    private final int cooldownTicks;

    PalSkill(int cooldownTicks) {
        this.cooldownTicks = cooldownTicks;
    }

    public int cooldownTicks() {
        return cooldownTicks;
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
