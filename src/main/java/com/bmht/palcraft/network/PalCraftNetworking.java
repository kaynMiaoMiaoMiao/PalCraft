package com.bmht.palcraft.network;

import com.bmht.palcraft.PalCraft;
import com.bmht.palcraft.base.BaseData;
import com.bmht.palcraft.base.BaseWorkType;
import com.bmht.palcraft.partner.PalInstance;
import com.bmht.palcraft.partner.PlayerPalData;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;

public final class PalCraftNetworking {
    public static final Identifier UI_REQUEST = new Identifier(PalCraft.MOD_ID, "ui_request");
    public static final Identifier UI_STATE = new Identifier(PalCraft.MOD_ID, "ui_state");
    public static final Identifier UI_ACTION = new Identifier(PalCraft.MOD_ID, "ui_action");

    private static final int ACTION_SUMMON = 1;
    private static final int ACTION_RECALL = 2;
    private static final int ACTION_ASSIGN_AUTO = 3;

    private PalCraftNetworking() {
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(UI_REQUEST, (server, player, handler, buf, responseSender) ->
                server.execute(() -> sendUiState(player)));

        ServerPlayNetworking.registerGlobalReceiver(UI_ACTION, (server, player, handler, buf, responseSender) -> {
            int action = buf.readVarInt();
            int slot = buf.readVarInt();
            server.execute(() -> {
                handleUiAction(player, action, slot);
                sendUiState(player);
            });
        });
    }

    public static void sendUiState(ServerPlayerEntity player) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeNbt(createUiState(player));
        ServerPlayNetworking.send(player, UI_STATE, buffer);
    }

    private static void handleUiAction(ServerPlayerEntity player, int action, int slot) {
        PlayerPalData palData = PlayerPalData.get(player.getServer());
        if (action == ACTION_SUMMON) {
            palData.summon(player, slot);
            return;
        }
        if (action == ACTION_RECALL) {
            palData.recall(player);
            return;
        }
        if (action == ACTION_ASSIGN_AUTO) {
            List<PalInstance> pals = palData.getStoredPals(player.getUuid());
            if (slot >= 0 && slot < pals.size()) {
                BaseData.get(player.getServer()).assignPalToNearestBase(player, pals.get(slot), null);
            }
        }
    }

    private static NbtCompound createUiState(ServerPlayerEntity player) {
        NbtCompound state = new NbtCompound();
        PlayerPalData palData = PlayerPalData.get(player.getServer());
        List<PalInstance> pals = palData.getStoredPals(player.getUuid());
        int activeSlot = palData.getActiveSlot(player.getUuid());
        boolean hasActive = palData.getActiveEntityUuid(player.getUuid()).isPresent();

        NbtList palList = new NbtList();
        for (int i = 0; i < pals.size(); i++) {
            PalInstance pal = pals.get(i);
            NbtCompound palNbt = new NbtCompound();
            palNbt.putInt("Slot", i);
            palNbt.putString("SpeciesName", Registries.ENTITY_TYPE.get(pal.speciesId()).getName().getString());
            palNbt.putString("SpeciesId", pal.speciesId().toString());
            palNbt.putString("CustomName", pal.customName());
            palNbt.putInt("Level", pal.level());
            palNbt.putLong("Experience", pal.experience());
            palNbt.putLong("NextExperience", PalInstance.experienceToNextLevel(pal.level()));
            palNbt.putFloat("Health", pal.health());
            palNbt.putFloat("MaxHealth", pal.maxHealth());
            palNbt.putFloat("Attack", pal.attack());
            palNbt.putFloat("Defense", pal.defense());
            palNbt.putString("Element", pal.elementType().id());
            palNbt.putBoolean("Active", hasActive && i == activeSlot);
            palNbt.putBoolean("Fainted", pal.health() <= 0.0F);
            palNbt.putString("Skills", pal.skills().stream().map(skill -> skill.id()).reduce((left, right) -> left + ", " + right).orElse("none"));
            palList.add(palNbt);
        }
        state.put("Pals", palList);

        NbtList baseList = new NbtList();
        for (BaseData.BaseView base : BaseData.get(player.getServer()).getBases(player.getUuid())) {
            NbtCompound baseNbt = new NbtCompound();
            baseNbt.putString("Position", base.corePositionText());
            baseNbt.putInt("Radius", base.radius());
            baseNbt.putInt("AssignedCount", base.assignedCount());
            baseNbt.putLong("TotalStock", base.gatheredMaterials());
            baseNbt.putInt("QueuedTasks", base.queuedTasks());
            baseNbt.putString("Assignments", base.assignmentSummary());
            baseNbt.putString("Stock", base.stockSummary());
            baseList.add(baseNbt);
        }
        state.put("Bases", baseList);

        NbtList workTypes = new NbtList();
        for (BaseWorkType workType : BaseWorkType.values()) {
            NbtCompound workTypeNbt = new NbtCompound();
            workTypeNbt.putString("Id", workType.id());
            workTypes.add(workTypeNbt);
        }
        state.put("WorkTypes", workTypes);
        return state;
    }
}
