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
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

public final class PalCraftNetworking {
    public static final Identifier UI_REQUEST = new Identifier(PalCraft.MOD_ID, "ui_request");
    public static final Identifier UI_STATE = new Identifier(PalCraft.MOD_ID, "ui_state");
    public static final Identifier UI_ACTION = new Identifier(PalCraft.MOD_ID, "ui_action");
    public static final Identifier UI_OPEN = new Identifier(PalCraft.MOD_ID, "ui_open");

    private static final int ACTION_SUMMON = 1;
    private static final int ACTION_RECALL = 2;
    private static final int ACTION_ASSIGN_AUTO = 3;
    private static final int ACTION_RENAME = 4;
    private static final int ACTION_ASSIGN_BASE = 5;
    private static final int ACTION_UNASSIGN_BASE = 6;
    private static final int ACTION_DEPLOY_BASE = 7;
    private static final int ACTION_RECALL_BASE = 8;
    private static final int ACTION_STORE_PLAYER_PAL_BASE = 9;
    private static final int ACTION_TAKE_BASE_PAL = 10;

    private PalCraftNetworking() {
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(UI_REQUEST, (server, player, handler, buf, responseSender) ->
                server.execute(() -> sendUiState(player)));

        ServerPlayNetworking.registerGlobalReceiver(UI_ACTION, (server, player, handler, buf, responseSender) -> {
            int action = buf.readVarInt();
            int slot = buf.readVarInt();
            String value = buf.readableBytes() > 0 ? buf.readString(128) : "";
            server.execute(() -> {
                handleUiAction(player, action, slot, value);
                sendUiState(player);
            });
        });
    }

    public static void openManagementUi(ServerPlayerEntity player) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeString("player");
        buffer.writeString("");
        ServerPlayNetworking.send(player, UI_OPEN, buffer);
        sendUiState(player);
    }

    public static void openBaseManagementUi(ServerPlayerEntity player, BlockPos corePos) {
        BaseData.get(player.getServer()).getBaseAt(player.getServerWorld(), corePos, player.getUuid()).ifPresent(base -> {
            PacketByteBuf buffer = PacketByteBufs.create();
            buffer.writeString("base");
            buffer.writeString(base.baseUuid().toString());
            ServerPlayNetworking.send(player, UI_OPEN, buffer);
            sendUiState(player);
        });
    }

    public static void sendUiState(ServerPlayerEntity player) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeNbt(createUiState(player));
        ServerPlayNetworking.send(player, UI_STATE, buffer);
    }

    private static void handleUiAction(ServerPlayerEntity player, int action, int slot, String value) {
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
        if (action == ACTION_RENAME) {
            palData.renamePal(player, slot, value);
        }
        if (action == ACTION_ASSIGN_BASE) {
            String[] parts = value.split("\\|", 2);
            if (parts.length == 2) {
                UUID baseUuid = UUID.fromString(parts[0]);
                BaseWorkType workType = BaseWorkType.fromId(parts[1]);
                BaseData.get(player.getServer()).assignStoredPal(player.getUuid(), baseUuid, slot, workType);
            }
        }
        if (action == ACTION_UNASSIGN_BASE) {
            String[] parts = value.split("\\|", 2);
            if (parts.length == 2) {
                UUID baseUuid = UUID.fromString(parts[0]);
                UUID palUuid = UUID.fromString(parts[1]);
                BaseData.get(player.getServer()).unassignStoredPal(player.getServer(), player.getUuid(), baseUuid, palUuid);
            }
        }
        if (action == ACTION_DEPLOY_BASE) {
            UUID baseUuid = UUID.fromString(value);
            BaseData.get(player.getServer()).deployStoredPal(player.getServer(), player.getUuid(), baseUuid, slot);
        }
        if (action == ACTION_RECALL_BASE) {
            String[] parts = value.split("\\|", 2);
            if (parts.length == 2) {
                UUID baseUuid = UUID.fromString(parts[0]);
                UUID palUuid = UUID.fromString(parts[1]);
                BaseData.get(player.getServer()).recallDeployedPal(player.getServer(), player.getUuid(), baseUuid, palUuid);
            }
        }
        if (action == ACTION_STORE_PLAYER_PAL_BASE) {
            UUID baseUuid = UUID.fromString(value);
            BaseData.get(player.getServer()).storePlayerPal(player, baseUuid, slot);
        }
        if (action == ACTION_TAKE_BASE_PAL) {
            UUID baseUuid = UUID.fromString(value);
            BaseData.get(player.getServer()).takeStoredPalToPlayer(player, baseUuid, slot);
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
            palNbt.putString("InstanceUuid", pal.instanceUuid().toString());
            palNbt.putString("SpeciesName", Registries.ENTITY_TYPE.get(pal.speciesId()).getName().getString());
            palNbt.putString("SpeciesTranslationKey", Registries.ENTITY_TYPE.get(pal.speciesId()).getTranslationKey());
            palNbt.putString("SpeciesId", pal.speciesId().toString());
            palNbt.putString("CustomName", pal.customName());
            palNbt.putDouble("Talent", pal.talent());
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
        state.putInt("CarryLimit", PlayerPalData.MAX_CARRIED_PALS);

        NbtList baseList = new NbtList();
        BaseData baseData = BaseData.get(player.getServer());
        for (BaseData.BaseView base : baseData.getBases(player.getUuid())) {
            NbtCompound baseNbt = new NbtCompound();
            baseNbt.putString("BaseUuid", base.baseUuid().toString());
            baseNbt.putString("Position", base.corePositionText());
            baseNbt.putInt("Radius", base.radius());
            baseNbt.putInt("AssignedCount", base.assignedCount());
            baseNbt.putInt("StoredCount", base.storedPals().size());
            baseNbt.putInt("StorageBlockCount", baseData.countStorageBlocks(player.getServer(), base.baseUuid()));
            baseNbt.putLong("TotalStock", base.gatheredMaterials());
            baseNbt.putInt("QueuedTasks", base.queuedTasks());
            baseNbt.putString("Assignments", base.assignmentSummary());
            baseNbt.putString("Stock", base.stockSummary());

            NbtList storedPals = new NbtList();
            for (BaseData.BasePalView storedPal : base.storedPals()) {
                PalInstance pal = storedPal.pal();
                NbtCompound storedPalNbt = new NbtCompound();
                storedPalNbt.putInt("Slot", storedPal.slot());
                storedPalNbt.putString("InstanceUuid", pal.instanceUuid().toString());
                storedPalNbt.putString("SpeciesName", Registries.ENTITY_TYPE.get(pal.speciesId()).getName().getString());
                storedPalNbt.putString("SpeciesTranslationKey", Registries.ENTITY_TYPE.get(pal.speciesId()).getTranslationKey());
                storedPalNbt.putString("CustomName", pal.customName());
                storedPalNbt.putDouble("Talent", pal.talent());
                storedPalNbt.putInt("Level", pal.level());
                storedPalNbt.putFloat("Health", pal.health());
                storedPalNbt.putFloat("MaxHealth", pal.maxHealth());
                storedPalNbt.putString("Element", pal.elementType().id());
                storedPalNbt.putBoolean("Deployed", storedPal.deployed());
                storedPalNbt.putBoolean("Assigned", storedPal.assigned());
                storedPalNbt.putString("WorkType", storedPal.workType() == null ? "" : storedPal.workType().id());
                storedPals.add(storedPalNbt);
            }
            baseNbt.put("StoredPals", storedPals);
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
