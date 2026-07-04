package com.bmht.palcraft.partner;

import java.util.Locale;

public enum PalElementType {
    NEUTRAL,
    ELECTRIC;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static PalElementType fromId(String id) {
        for (PalElementType elementType : values()) {
            if (elementType.id().equals(id)) {
                return elementType;
            }
        }
        return NEUTRAL;
    }
}
