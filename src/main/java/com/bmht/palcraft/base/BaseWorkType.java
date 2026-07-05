package com.bmht.palcraft.base;

import com.bmht.palcraft.partner.PalElementType;
import com.bmht.palcraft.partner.PalInstance;

import java.util.Locale;

public enum BaseWorkType implements BaseWork {
    MINING(4),
    LOGGING(3),
    PLANTING(3),
    HAULING(2),
    MANUFACTURING(5);

    private final int requiredWork;

    BaseWorkType(int requiredWork) {
        this.requiredWork = requiredWork;
    }

    public int requiredWork() {
        return requiredWork;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public int suitability(PalInstance pal) {
        int score = 1;
        if (pal.level() >= 3) {
            score++;
        }
        if (pal.elementType() == PalElementType.THUNDER && (this == MANUFACTURING || this == MINING)) {
            score += 2;
        }
        if (pal.elementType() == PalElementType.FIRE && this == MANUFACTURING) {
            score += 2;
        }
        if (pal.elementType() == PalElementType.WATER && (this == PLANTING || this == HAULING)) {
            score += 2;
        }
        if (pal.elementType() == PalElementType.WIND && this == HAULING) {
            score += 2;
        }
        if (pal.elementType() == PalElementType.WOOD && (this == PLANTING || this == LOGGING)) {
            score += 2;
        }
        if (pal.elementType() == PalElementType.ICE && this == HAULING) {
            score += 1;
        }
        if (pal.elementType() == PalElementType.EARTH && this == MINING) {
            score += 2;
        }
        if (this == HAULING) {
            score++;
        }
        return score;
    }

    public static BaseWorkType bestFor(PalInstance pal) {
        BaseWorkType bestType = HAULING;
        int bestScore = bestType.suitability(pal);
        for (BaseWorkType workType : values()) {
            int score = workType.suitability(pal);
            if (score > bestScore) {
                bestType = workType;
                bestScore = score;
            }
        }
        return bestType;
    }

    public static BaseWorkType fromId(String id) {
        for (BaseWorkType workType : values()) {
            if (workType.id().equals(id)) {
                return workType;
            }
        }
        return HAULING;
    }
}
