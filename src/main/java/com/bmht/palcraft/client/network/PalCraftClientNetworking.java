package com.bmht.palcraft.client.network;

import com.bmht.palcraft.client.PalCraftClientState;
import com.bmht.palcraft.client.screen.PalCraftManagementScreen;
import com.bmht.palcraft.network.PalCraftNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;

public final class PalCraftClientNetworking {
    public static final int ACTION_SUMMON = 1;
    public static final int ACTION_RECALL = 2;
    public static final int ACTION_ASSIGN_AUTO = 3;
    public static final int ACTION_RENAME = 4;
    public static final int ACTION_ASSIGN_BASE = 5;
    public static final int ACTION_UNASSIGN_BASE = 6;
    public static final int ACTION_DEPLOY_BASE = 7;
    public static final int ACTION_RECALL_BASE = 8;

    private PalCraftClientNetworking() {
    }

    public static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(PalCraftNetworking.UI_OPEN, (client, handler, buf, responseSender) -> {
            String mode = buf.readString();
            String baseUuid = buf.readString();
            client.execute(() -> {
                if (client.player != null) {
                    if ("base".equals(mode)) {
                        client.setScreen(PalCraftManagementScreen.base(baseUuid));
                    } else if (!(client.currentScreen instanceof PalCraftManagementScreen)) {
                        client.setScreen(PalCraftManagementScreen.player());
                    }
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(PalCraftNetworking.UI_STATE, (client, handler, buf, responseSender) -> {
            var nbt = buf.readNbt();
            client.execute(() -> {
                if (nbt != null) {
                    PalCraftClientState.update(nbt);
                    if (client.currentScreen instanceof PalCraftManagementScreen screen) {
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
        buffer.writeString("");
        ClientPlayNetworking.send(PalCraftNetworking.UI_ACTION, buffer);
    }

    public static void sendRename(int slot, String name) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeVarInt(ACTION_RENAME);
        buffer.writeVarInt(slot);
        buffer.writeString(name, 32);
        ClientPlayNetworking.send(PalCraftNetworking.UI_ACTION, buffer);
    }

    public static void sendBaseAssign(String baseUuid, int storageSlot, String workType) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeVarInt(ACTION_ASSIGN_BASE);
        buffer.writeVarInt(storageSlot);
        buffer.writeString(baseUuid + "|" + workType, 128);
        ClientPlayNetworking.send(PalCraftNetworking.UI_ACTION, buffer);
    }

    public static void sendBaseUnassign(String baseUuid, String palUuid) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeVarInt(ACTION_UNASSIGN_BASE);
        buffer.writeVarInt(-1);
        buffer.writeString(baseUuid + "|" + palUuid, 128);
        ClientPlayNetworking.send(PalCraftNetworking.UI_ACTION, buffer);
    }

    public static void sendBaseDeploy(String baseUuid, int storageSlot) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeVarInt(ACTION_DEPLOY_BASE);
        buffer.writeVarInt(storageSlot);
        buffer.writeString(baseUuid, 128);
        ClientPlayNetworking.send(PalCraftNetworking.UI_ACTION, buffer);
    }

    public static void sendBaseRecall(String baseUuid, String palUuid) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeVarInt(ACTION_RECALL_BASE);
        buffer.writeVarInt(-1);
        buffer.writeString(baseUuid + "|" + palUuid, 128);
        ClientPlayNetworking.send(PalCraftNetworking.UI_ACTION, buffer);
    }
}
