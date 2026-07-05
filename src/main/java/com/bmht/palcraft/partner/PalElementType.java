package com.bmht.palcraft.partner;

import java.util.Locale;
import java.util.Set;

public enum PalElementType {
    FIRE,
    WATER,
    THUNDER,
    WIND,
    WOOD,
    ICE,
    EARTH,
    DRAGON,
    NEUTRAL;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public float damageMultiplierAgainst(PalElementType defender) {
        if (this == NEUTRAL || defender == NEUTRAL) {
            return 1.0F;
        }
        if (strongAgainst().contains(defender)) {
            return 1.5F;
        }
        if (resistedBy().contains(defender)) {
            return 0.5F;
        }
        return 1.0F;
    }

    private Set<PalElementType> strongAgainst() {
        return switch (this) {
            case FIRE -> Set.of(WOOD, ICE);
            case WATER -> Set.of(FIRE, EARTH);
            case THUNDER -> Set.of(WATER, WIND);
            case WIND -> Set.of(WOOD, EARTH);
            case WOOD -> Set.of(WATER, EARTH);
            case ICE -> Set.of(WIND, DRAGON);
            case EARTH -> Set.of(FIRE, THUNDER);
            case DRAGON -> Set.of(FIRE, WATER, THUNDER, WIND, WOOD);
            case NEUTRAL -> Set.of();
        };
    }

    private Set<PalElementType> resistedBy() {
        return switch (this) {
            case FIRE -> Set.of(WATER, EARTH, DRAGON);
            case WATER -> Set.of(WOOD, THUNDER, DRAGON);
            case THUNDER -> Set.of(EARTH, WOOD, DRAGON);
            case WIND -> Set.of(THUNDER, ICE, DRAGON);
            case WOOD -> Set.of(FIRE, ICE, DRAGON);
            case ICE -> Set.of(FIRE, WATER, EARTH);
            case EARTH -> Set.of(WATER, WOOD, WIND);
            case DRAGON -> Set.of(ICE, EARTH, DRAGON);
            case NEUTRAL -> Set.of();
        };
    }

    public static PalElementType fromId(String id) {
        if ("electric".equals(id)) {
            return THUNDER;
        }
        for (PalElementType elementType : values()) {
            if (elementType.id().equals(id)) {
                return elementType;
            }
        }
        return NEUTRAL;
    }
}
