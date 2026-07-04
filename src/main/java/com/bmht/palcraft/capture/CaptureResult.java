package com.bmht.palcraft.capture;

import com.bmht.palcraft.partner.PalInstance;

public record CaptureResult(
        boolean attempted,
        boolean successful,
        double chance,
        PalInstance palInstance
) {
    public static CaptureResult notAttempted() {
        return new CaptureResult(false, false, 0.0D, null);
    }

    public static CaptureResult failed(double chance) {
        return new CaptureResult(true, false, chance, null);
    }

    public static CaptureResult succeeded(double chance, PalInstance palInstance) {
        return new CaptureResult(true, true, chance, palInstance);
    }
}
