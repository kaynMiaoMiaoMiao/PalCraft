package com.bmht.palcraft.client.network;

import com.bmht.palcraft.client.PalCraftClientState;
import com.bmht.palcraft.network.PalCraftNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;

public final class PalCraftClientNetworking {
    public static final int ACTION_SUMMON = 1;
    public static final int ACTION_RECALL = 2;
    public static final int ACTION_ASSIGN_AUTO = 3;

    private PalCraftClientNetworking() {
    }

    public static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(PalCraftNetworking.UI_STATE, (client, handler, buf, responseSender) -> {
            var nbt = buf.readNbt();
            client.execute(() -> {
                if (nbt != null) {
                    PalCraftClientState.update(nbt);
                    if (client.currentScreen instanceof com.bmht.palcraft.client.screen.PalCraftManagementScreen screen) {
                        screen.refreshFromState();
                    }
                }
            });
        });
    }

    public static void requestState() {
        ClientPlayNetworking.send(PalCraftNetworking.UI_REQUEST, PacketByteBufs.empty());
    }

    public static void sendAction(int action, int slot) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeVarInt(action);
        buffer.writeVarInt(slot);
        ClientPlayNetworking.send(PalCraftNetworking.UI_ACTION, buffer);
    }
}
