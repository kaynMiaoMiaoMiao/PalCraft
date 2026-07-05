package com.bmht.palcraft.partner;

import com.bmht.palcraft.PalCraft;
import com.bmht.palcraft.entity.SparkitEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PlayerPalData extends PersistentState {
    private static final String STATE_ID = PalCraft.MOD_ID + "_player_pals";
    public static final int MAX_CARRIED_PALS = 4;

    private final Map<UUID, PlayerRecord> records = new HashMap<>();

    public static PlayerPalData get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(
                PlayerPalData::fromNbt,
                PlayerPalData::new,
                STATE_ID
        );
    }

    public AddResult addCapturedPal(ServerPlayerEntity player, PalInstance palInstance) {
        PlayerRecord record = getRecord(player.getUuid());
        if (record.storedPals.size() >= MAX_CARRIED_PALS) {
            return AddResult.FULL;
        }

        record.storedPals.add(palInstance);
        markDirty();
        return AddResult.CARRIED;
    }

    public List<PalInstance> getStoredPals(UUID playerUuid) {
        return List.copyOf(getRecord(playerUuid).storedPals);
    }

    public boolean hasCarrySpace(UUID playerUuid) {
        return getRecord(playerUuid).storedPals.size() < MAX_CARRIED_PALS;
    }

    public boolean addStoredPal(UUID playerUuid, PalInstance palInstance) {
        PlayerRecord record = getRecord(playerUuid);
        if (record.storedPals.size() >= MAX_CARRIED_PALS) {
            return false;
        }

        record.storedPals.add(palInstance);
        markDirty();
        return true;
    }

    public Optional<PalInstance> takeStoredPal(ServerPlayerEntity player, int slot) {
        PlayerRecord record = getRecord(player.getUuid());
        if (slot < 0 || slot >= record.storedPals.size()) {
            return Optional.empty();
        }

        if (slot == record.activeSlot) {
            if (record.activeEntityUuid != null) {
                findEntity(player.getServer(), record.activeEntityUuid).ifPresent(entity -> {
                    if (entity instanceof LivingEntity livingEntity) {
                        PalInstance current = record.storedPals.get(slot);
                        record.storedPals.set(slot, current.withHealth(livingEntity.getHealth()));
                    }
                    entity.discard();
                });
            }
            record.activeSlot = -1;
            record.activeEntityUuid = null;
        } else if (record.activeSlot > slot) {
            record.activeSlot--;
        }

        PalInstance palInstance = record.storedPals.remove(slot);
        markDirty();
        return Optional.of(palInstance);
    }

    public int getActiveSlot(UUID playerUuid) {
        return getRecord(playerUuid).activeSlot;
    }

    public Optional<UUID> getActiveEntityUuid(UUID playerUuid) {
        return Optional.ofNullable(getRecord(playerUuid).activeEntityUuid);
    }

    public Optional<String> summon(ServerPlayerEntity player, int slot) {
        PlayerRecord record = getRecord(player.getUuid());
        if (slot < 0 || slot >= record.storedPals.size()) {
            return Optional.of("message.palcraft.command.invalid_slot");
        }
        if (record.activeEntityUuid != null && findEntity(player.getServer(), record.activeEntityUuid).isPresent()) {
            return Optional.of("message.palcraft.command.already_active");
        }

        PalInstance palInstance = record.storedPals.get(slot);
        if (palInstance.health() <= 0.0F) {
            return Optional.of("message.palcraft.command.cannot_summon_fainted");
        }

        EntityType<?> entityType = Registries.ENTITY_TYPE.get(palInstance.speciesId());
        Entity entity = entityType.create(player.getWorld());
        if (!(entity instanceof LivingEntity livingEntity)) {
            return Optional.of("message.palcraft.command.unsupported_species");
        }

        livingEntity.refreshPositionAndAngles(
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYaw(),
                0.0F
        );
        if (!palInstance.customName().isBlank()) {
            livingEntity.setCustomName(Text.literal(palInstance.customName()));
        }
        if (livingEntity instanceof MobEntity mobEntity) {
            mobEntity.setPersistent();
        }
        if (livingEntity instanceof SparkitEntity sparkitEntity) {
            sparkitEntity.setSummonedPalData(player.getUuid(), palInstance);
        }
        livingEntity.setHealth(MathHelper.clamp(palInstance.health(), 1.0F, livingEntity.getMaxHealth()));

        if (!player.getWorld().spawnEntity(livingEntity)) {
            return Optional.of("message.palcraft.command.summon_failed");
        }

        record.activeSlot = slot;
        record.activeEntityUuid = livingEntity.getUuid();
        markDirty();
        return Optional.empty();
    }

    public boolean recall(ServerPlayerEntity player) {
        PlayerRecord record = getRecord(player.getUuid());
        if (record.activeEntityUuid == null) {
            return false;
        }

        findEntity(player.getServer(), record.activeEntityUuid).ifPresent(entity -> {
            if (entity instanceof LivingEntity livingEntity && record.activeSlot >= 0 && record.activeSlot < record.storedPals.size()) {
                PalInstance current = record.storedPals.get(record.activeSlot);
                record.storedPals.set(record.activeSlot, current.withHealth(livingEntity.getHealth()));
            }
            entity.discard();
        });

        record.activeSlot = -1;
        record.activeEntityUuid = null;
        markDirty();
        return true;
    }

    public boolean renamePal(ServerPlayerEntity player, int slot, String name) {
        PlayerRecord record = getRecord(player.getUuid());
        if (slot < 0 || slot >= record.storedPals.size()) {
            return false;
        }

        String sanitizedName = sanitizeName(name);
        PalInstance current = record.storedPals.get(slot);
        record.storedPals.set(slot, current.withCustomName(sanitizedName));
        if (record.activeSlot == slot && record.activeEntityUuid != null) {
            findEntity(player.getServer(), record.activeEntityUuid).ifPresent(entity -> {
                if (sanitizedName.isBlank()) {
                    entity.setCustomName(null);
                } else {
                    entity.setCustomName(Text.literal(sanitizedName));
                }
            });
        }
        markDirty();
        return true;
    }

    public void markActivePalDead(UUID ownerUuid, UUID entityUuid) {
        PlayerRecord record = records.get(ownerUuid);
        if (record == null || record.activeEntityUuid == null || !record.activeEntityUuid.equals(entityUuid)) {
            return;
        }

        if (record.activeSlot >= 0 && record.activeSlot < record.storedPals.size()) {
            PalInstance current = record.storedPals.get(record.activeSlot);
            record.storedPals.set(record.activeSlot, current.withHealth(0.0F));
        }
        record.activeSlot = -1;
        record.activeEntityUuid = null;
        markDirty();
    }

    public void grantExperienceForKill(ServerWorld world, SparkitEntity palEntity, LivingEntity killedEntity) {
        UUID ownerUuid = palEntity.getOwnerUuid();
        UUID instanceUuid = palEntity.getInstanceUuid();
        PlayerRecord record = records.get(ownerUuid);
        if (record == null
                || record.activeEntityUuid == null
                || !record.activeEntityUuid.equals(palEntity.getUuid())
                || record.activeSlot < 0
                || record.activeSlot >= record.storedPals.size()) {
            return;
        }

        PalInstance current = record.storedPals.get(record.activeSlot);
        if (!current.instanceUuid().equals(instanceUuid)) {
            return;
        }

        long gainedExperience = calculateExperienceReward(killedEntity);
        long experience = current.experience() + gainedExperience;
        int level = current.level();
        boolean leveledUp = false;
        while (experience >= PalInstance.experienceToNextLevel(level)) {
            experience -= PalInstance.experienceToNextLevel(level);
            level++;
            leveledUp = true;
        }

        float maxHealth = PalInstance.maxHealthForLevel(current.speciesId(), level, current.talent());
        float attack = PalInstance.attackForLevel(current.speciesId(), level, current.talent());
        float defense = PalInstance.defenseForLevel(current.speciesId(), level, current.talent());
        float health = MathHelper.clamp(palEntity.getHealth(), 0.0F, maxHealth);
        if (leveledUp) {
            health = Math.min(maxHealth, health + Math.max(2.0F, maxHealth - current.maxHealth()));
        }

        PalInstance updated = current.withProgression(level, experience, maxHealth, attack, defense, health);
        record.storedPals.set(record.activeSlot, updated);
        palEntity.applyPalInstanceStats(updated, leveledUp);
        markDirty();

        ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(ownerUuid);
        if (owner != null) {
            owner.sendMessage(Text.translatable("message.palcraft.progress.exp_gained", palEntity.getDisplayName(), gainedExperience), false);
            if (leveledUp) {
                owner.sendMessage(Text.translatable("message.palcraft.progress.level_up", palEntity.getDisplayName(), level), false);
            }
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList players = new NbtList();
        for (Map.Entry<UUID, PlayerRecord> entry : records.entrySet()) {
            NbtCompound playerNbt = new NbtCompound();
            playerNbt.putString("PlayerUuid", entry.getKey().toString());
            playerNbt.putInt("ActiveSlot", entry.getValue().activeSlot);
            if (entry.getValue().activeEntityUuid != null) {
                playerNbt.putString("ActiveEntityUuid", entry.getValue().activeEntityUuid.toString());
            }

            NbtList pals = new NbtList();
            for (PalInstance palInstance : entry.getValue().storedPals) {
                pals.add(palInstance.writeNbt());
            }
            playerNbt.put("Pals", pals);
            players.add(playerNbt);
        }
        nbt.put("Players", players);
        return nbt;
    }

    private static PlayerPalData fromNbt(NbtCompound nbt) {
        PlayerPalData data = new PlayerPalData();
        NbtList players = nbt.getList("Players", NbtElement.COMPOUND_TYPE);
        for (NbtElement playerElement : players) {
            NbtCompound playerNbt = (NbtCompound) playerElement;
            PlayerRecord record = new PlayerRecord();
            record.activeSlot = playerNbt.getInt("ActiveSlot");
            if (playerNbt.contains("ActiveEntityUuid")) {
                record.activeEntityUuid = UUID.fromString(playerNbt.getString("ActiveEntityUuid"));
            }

            NbtList pals = playerNbt.getList("Pals", NbtElement.COMPOUND_TYPE);
            for (NbtElement palElement : pals) {
                record.storedPals.add(PalInstance.fromNbt((NbtCompound) palElement));
            }
            data.records.put(UUID.fromString(playerNbt.getString("PlayerUuid")), record);
        }
        return data;
    }

    private PlayerRecord getRecord(UUID playerUuid) {
        return records.computeIfAbsent(playerUuid, ignored -> new PlayerRecord());
    }

    private static Optional<Entity> findEntity(MinecraftServer server, UUID entityUuid) {
        for (ServerWorld world : server.getWorlds()) {
            Entity entity = world.getEntity(entityUuid);
            if (entity != null) {
                return Optional.of(entity);
            }
        }
        return Optional.empty();
    }

    private static long calculateExperienceReward(LivingEntity killedEntity) {
        return Math.max(5L, Math.round(10.0F + killedEntity.getMaxHealth() * 2.0F));
    }

    private static String sanitizeName(String name) {
        String trimmed = name == null ? "" : name.trim();
        return trimmed.length() > 32 ? trimmed.substring(0, 32) : trimmed;
    }

    private static final class PlayerRecord {
        private final List<PalInstance> storedPals = new ArrayList<>();
        private int activeSlot = -1;
        private UUID activeEntityUuid;
    }

    public enum AddResult {
        CARRIED,
        FULL
    }
}
