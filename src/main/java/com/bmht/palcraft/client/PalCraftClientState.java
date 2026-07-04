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
                    palNbt.getString("SpeciesName"),
                    palNbt.getString("SpeciesTranslationKey"),
                    palNbt.getString("SpeciesId"),
                    palNbt.getString("CustomName"),
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
            bases.add(new BaseSummary(
                    baseNbt.getString("Position"),
                    baseNbt.getInt("Radius"),
                    baseNbt.getInt("AssignedCount"),
                    baseNbt.getLong("TotalStock"),
                    baseNbt.getInt("QueuedTasks"),
                    baseNbt.getString("Assignments"),
                    baseNbt.getString("Stock")
            ));
        }
        latestState = new UiState(List.copyOf(pals), List.copyOf(bases));
    }

    public record UiState(List<PalSummary> pals, List<BaseSummary> bases) {
        public static UiState empty() {
            return new UiState(List.of(), List.of());
        }
    }

    public record PalSummary(
            int slot,
            String speciesName,
            String speciesTranslationKey,
            String speciesId,
            String customName,
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
            String position,
            int radius,
            int assignedCount,
            long totalStock,
            int queuedTasks,
            String assignments,
            String stock
    ) {
    }
}
