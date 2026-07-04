package com.bmht.palcraft.base;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public final class BaseWorkEvents {
    private BaseWorkEvents() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 200 == 0) {
                BaseData.get(server).tickWork(server);
            }
        });
    }
}
