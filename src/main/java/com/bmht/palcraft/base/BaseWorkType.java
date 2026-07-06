package com.bmht.palcraft.base;

import com.bmht.palcraft.partner.PalInstance;
import com.bmht.palcraft.partner.PalSpecies;

import java.util.Locale;

public enum BaseWorkType implements BaseWork {
    MINING(4),
    LOGGING(3),
    PLANTING(3),
    HAULING(2),
    MANUFACTURING(5);

    private static final BaseWorkType[] ASSIGNABLE_VALUES = {
            MINING,
            LOGGING,
            PLANTING
    };

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

    public boolean isAssignable() {
        return this == MINING || this == LOGGING || this == PLANTING;
    }

    public int suitability(PalInstance pal) {
        int score = PalSpecies.fromId(pal.speciesId()).workSuitability(this);
        if (pal.level() >= 3) {
            score++;
        }
        return score;
    }

    public static BaseWorkType bestFor(PalInstance pal) {
        BaseWorkType bestType = MINING;
        int bestScore = bestType.suitability(pal);
        for (BaseWorkType workType : assignableValues()) {
            int score = workType.suitability(pal);
            if (score > bestScore) {
                bestType = workType;
                bestScore = score;
            }
        }
        return bestType;
    }

    public static BaseWorkType[] assignableValues() {
        return ASSIGNABLE_VALUES.clone();
    }

    public static BaseWorkType fromId(String id) {
        for (BaseWorkType workType : values()) {
            if (workType.id().equals(id)) {
                return workType;
            }
        }
        return MINING;
    }
}
