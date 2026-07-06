package com.bmht.palcraft.client;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.List;

public final class PalCraftClientState {
    private static UiState latestState = UiState.empty();

    private PalCraftClientState() {
    }

    public static UiState latestState() {
        return latestState;
    }

    public static void update(NbtCompound nbt) {
        List<PalSummary> pals = new ArrayList<>();
        NbtList palList = nbt.getList("Pals", NbtElement.COMPOUND_TYPE);
        for (NbtElement palElement : palList) {
            NbtCompound palNbt = (NbtCompound) palElement;
            pals.add(new PalSummary(
                    palNbt.getInt("Slot"),
                    palNbt.getString("InstanceUuid"),
                    palNbt.getString("SpeciesName"),
                    palNbt.getString("SpeciesTranslationKey"),
                    palNbt.getString("SpeciesId"),
                    palNbt.getString("RoleTranslationKey"),
                    palNbt.getString("WorkSuitability"),
                    palNbt.getString("CustomName"),
                    palNbt.getDouble("Talent"),
                    palNbt.getInt("Level"),
                    palNbt.getLong("Experience"),
                    palNbt.getLong("NextExperience"),
                    palNbt.getFloat("Health"),
                    palNbt.getFloat("MaxHealth"),
                    palNbt.getFloat("Attack"),
                    palNbt.getFloat("Defense"),
                    palNbt.getString("Element"),
                    palNbt.getBoolean("Active"),
                    palNbt.getBoolean("Fainted"),
                    palNbt.getString("Skills")
            ));
        }

        List<BaseSummary> bases = new ArrayList<>();
        NbtList baseList = nbt.getList("Bases", NbtElement.COMPOUND_TYPE);
        for (NbtElement baseElement : baseList) {
            NbtCompound baseNbt = (NbtCompound) baseElement;
            List<BasePalSummary> storedPals = new ArrayList<>();
            NbtList storedPalList = baseNbt.getList("StoredPals", NbtElement.COMPOUND_TYPE);
            for (NbtElement storedPalElement : storedPalList) {
                NbtCompound palNbt = (NbtCompound) storedPalElement;
                storedPals.add(new BasePalSummary(
                        palNbt.getInt("Slot"),
                        palNbt.getString("InstanceUuid"),
                        palNbt.getString("SpeciesName"),
                        palNbt.getString("SpeciesTranslationKey"),
                        palNbt.getString("RoleTranslationKey"),
                        palNbt.getString("WorkSuitability"),
                        palNbt.getString("CustomName"),
                        palNbt.getDouble("Talent"),
                        palNbt.getInt("Level"),
                        palNbt.getFloat("Health"),
                        palNbt.getFloat("MaxHealth"),
                        palNbt.getString("Element"),
                        palNbt.getBoolean("Deployed"),
                        palNbt.getBoolean("Assigned"),
                        palNbt.getString("WorkType"),
                        palNbt.getString("WorkProgress")
                ));
            }
            bases.add(new BaseSummary(
                    baseNbt.getString("BaseUuid"),
                    baseNbt.getString("Position"),
                    baseNbt.getInt("Radius"),
                    baseNbt.getInt("AssignedCount"),
                    baseNbt.getInt("StoredCount"),
                    baseNbt.getInt("StorageBlockCount"),
                    baseNbt.getLong("TotalStock"),
                    baseNbt.getInt("QueuedTasks"),
                    baseNbt.getString("Assignments"),
                    baseNbt.getString("Stock"),
                    baseNbt.getString("TaskProgress"),
                    List.copyOf(storedPals)
            ));
        }
        latestState = new UiState(List.copyOf(pals), List.copyOf(bases), nbt.getInt("CarryLimit"));
    }

    public record UiState(List<PalSummary> pals, List<BaseSummary> bases, int carryLimit) {
        public static UiState empty() {
            return new UiState(List.of(), List.of(), 4);
        }
    }

    public record PalSummary(
            int slot,
            String instanceUuid,
            String speciesName,
            String speciesTranslationKey,
            String speciesId,
            String roleTranslationKey,
            String workSuitability,
            String customName,
            double talent,
            int level,
            long experience,
            long nextExperience,
            float health,
            float maxHealth,
            float attack,
            float defense,
            String element,
            boolean active,
            boolean fainted,
            String skills
    ) {
        public String displayName() {
            return customName.isBlank() ? speciesName : customName;
        }
    }

    public record BaseSummary(
            String baseUuid,
            String position,
            int radius,
            int assignedCount,
            int storedCount,
            int storageBlockCount,
            long totalStock,
            int queuedTasks,
            String assignments,
            String stock,
            String taskProgress,
            List<BasePalSummary> storedPals
    ) {
    }

    public record BasePalSummary(
            int slot,
            String instanceUuid,
            String speciesName,
            String speciesTranslationKey,
            String roleTranslationKey,
            String workSuitability,
            String customName,
            double talent,
            int level,
            float health,
            float maxHealth,
            String element,
            boolean deployed,
            boolean assigned,
            String workType,
            String workProgress
    ) {
        public String displayName() {
            return customName.isBlank() ? speciesName : customName;
        }
    }
}
