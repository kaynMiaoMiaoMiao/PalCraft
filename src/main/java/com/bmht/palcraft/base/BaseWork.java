package com.bmht.palcraft.base;

import com.bmht.palcraft.partner.PalInstance;

public interface BaseWork {
    String id();

    int requiredWork();

    int suitability(PalInstance pal);
}
