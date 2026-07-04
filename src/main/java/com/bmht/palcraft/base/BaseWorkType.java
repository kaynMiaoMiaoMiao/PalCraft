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
        if (pal.elementType() == PalElementType.ELECTRIC && (this == MANUFACTURING || this == MINING)) {
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
