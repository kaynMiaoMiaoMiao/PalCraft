package com.bmht.palcraft.command;

import com.bmht.palcraft.base.BaseData;
import com.bmht.palcraft.base.BaseWorkType;
import com.bmht.palcraft.partner.PalInstance;
import com.bmht.palcraft.partner.PlayerPalData;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class PalCraftCommands {
    private PalCraftCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                literal("palcraft")
                        .then(literal("list").executes(context -> listPals(context.getSource())))
                        .then(literal("summon")
                                .then(argument("slot", IntegerArgumentType.integer(0))
                                        .executes(context -> summonPal(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "slot")
                                        ))))
                        .then(literal("recall").executes(context -> recallPal(context.getSource())))
                        .then(literal("base")
                                .then(literal("list").executes(context -> listBases(context.getSource())))
                                .then(literal("assign")
                                        .then(argument("slot", IntegerArgumentType.integer(0))
                                                .executes(context -> assignPalToBase(
                                                        context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "slot"),
                                                        null
                                                ))
                                                .then(literal("mining").executes(context -> assignPalToBase(context.getSource(), IntegerArgumentType.getInteger(context, "slot"), BaseWorkType.MINING)))
                                                .then(literal("logging").executes(context -> assignPalToBase(context.getSource(), IntegerArgumentType.getInteger(context, "slot"), BaseWorkType.LOGGING)))
                                                .then(literal("planting").executes(context -> assignPalToBase(context.getSource(), IntegerArgumentType.getInteger(context, "slot"), BaseWorkType.PLANTING)))
                                                .then(literal("hauling").executes(context -> assignPalToBase(context.getSource(), IntegerArgumentType.getInteger(context, "slot"), BaseWorkType.HAULING)))
                                                .then(literal("manufacturing").executes(context -> assignPalToBase(context.getSource(), IntegerArgumentType.getInteger(context, "slot"), BaseWorkType.MANUFACTURING))))))
        ));
    }

    private static int listPals(ServerCommandSource source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        PlayerPalData data = PlayerPalData.get(source.getServer());
        List<PalInstance> pals = data.getStoredPals(player.getUuid());
        if (pals.isEmpty()) {
            source.sendFeedback(() -> Text.translatable("message.palcraft.command.list_empty"), false);
            return 0;
        }

        source.sendFeedback(() -> formatPalListHeader(pals.size()), false);
        int activeSlot = data.getActiveSlot(player.getUuid());
        for (int i = 0; i < pals.size(); i++) {
            PalInstance pal = pals.get(i);
            Text speciesName = Text.translatable(Registries.ENTITY_TYPE.get(pal.speciesId()).getTranslationKey());
            boolean active = i == activeSlot && data.getActiveEntityUuid(player.getUuid()).isPresent();
            int slot = i;
            source.sendFeedback(() -> formatPalListEntry(slot, speciesName, pal, active), false);
        }
        return pals.size();
    }

    private static Text formatPalListHeader(int count) {
        return Text.empty()
                .append(Text.literal("PalCraft ").formatted(Formatting.GOLD, Formatting.BOLD))
                .append(Text.literal("Pals").formatted(Formatting.AQUA, Formatting.BOLD))
                .append(Text.literal("  "))
                .append(Text.literal(String.valueOf(count)).formatted(Formatting.YELLOW))
                .append(Text.literal(" captured").formatted(Formatting.GRAY));
    }

    private static Text formatPalListEntry(int slot, Text speciesName, PalInstance pal, boolean active) {
        long nextLevelExperience = PalInstance.experienceToNextLevel(pal.level());
        MutableText entry = Text.empty()
                .append(Text.literal("[").formatted(Formatting.DARK_GRAY))
                .append(Text.literal(String.valueOf(slot)).formatted(Formatting.GOLD))
                .append(Text.literal("] ").formatted(Formatting.DARK_GRAY))
                .append(speciesName.copy().formatted(Formatting.AQUA))
                .append(Text.literal("  Lv.").formatted(Formatting.DARK_GRAY))
                .append(Text.literal(String.valueOf(pal.level())).formatted(Formatting.YELLOW))
                .append(Text.literal("  EXP ").formatted(Formatting.DARK_GRAY))
                .append(Text.literal(pal.experience() + "/" + nextLevelExperience).formatted(Formatting.GREEN))
                .append(Text.literal("  HP ").formatted(Formatting.DARK_GRAY))
                .append(Text.literal(Math.round(pal.health()) + "/" + Math.round(pal.maxHealth())).formatted(healthFormatting(pal)));

        if (active) {
            entry.append(Text.translatable("message.palcraft.command.active_marker").formatted(Formatting.GREEN, Formatting.BOLD));
        }
        return entry;
    }

    private static Formatting healthFormatting(PalInstance pal) {
        if (pal.health() <= 0.0F) {
            return Formatting.RED;
        }
        if (pal.health() <= pal.maxHealth() * 0.35F) {
            return Formatting.YELLOW;
        }
        return Formatting.GREEN;
    }

    private static int listBases(ServerCommandSource source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        List<BaseData.BaseView> bases = BaseData.get(source.getServer()).getBases(player.getUuid());
        if (bases.isEmpty()) {
            source.sendFeedback(() -> Text.translatable("message.palcraft.base.list_empty").formatted(Formatting.GRAY), false);
            return 0;
        }

        source.sendFeedback(() -> Text.translatable("message.palcraft.base.list_header", bases.size()).formatted(Formatting.GOLD, Formatting.BOLD), false);
        for (int i = 0; i < bases.size(); i++) {
            BaseData.BaseView base = bases.get(i);
            int index = i;
            source.sendFeedback(() -> formatBaseListEntry(index, base), false);
        }
        return bases.size();
    }

    private static Text formatBaseListEntry(int index, BaseData.BaseView base) {
        return Text.empty()
                .append(Text.literal("[").formatted(Formatting.DARK_GRAY))
                .append(Text.literal(String.valueOf(index)).formatted(Formatting.GOLD))
                .append(Text.literal("] ").formatted(Formatting.DARK_GRAY))
                .append(Text.literal(base.corePositionText()).formatted(Formatting.AQUA))
                .append(Text.literal("  R ").formatted(Formatting.DARK_GRAY))
                .append(Text.literal(String.valueOf(base.radius())).formatted(Formatting.YELLOW))
                .append(Text.literal("  Pals ").formatted(Formatting.DARK_GRAY))
                .append(Text.literal(String.valueOf(base.assignedCount())).formatted(Formatting.GREEN))
                .append(Text.literal("  Stock ").formatted(Formatting.DARK_GRAY))
                .append(Text.literal(String.valueOf(base.gatheredMaterials())).formatted(Formatting.LIGHT_PURPLE))
                .append(Text.literal("  Queue ").formatted(Formatting.DARK_GRAY))
                .append(Text.literal(String.valueOf(base.queuedTasks())).formatted(Formatting.YELLOW))
                .append(Text.literal("  Work ").formatted(Formatting.DARK_GRAY))
                .append(Text.literal(base.assignmentSummary()).formatted(Formatting.GREEN))
                .append(Text.literal("  StockDetail ").formatted(Formatting.DARK_GRAY))
                .append(Text.literal(base.stockSummary()).formatted(Formatting.LIGHT_PURPLE));
    }

    private static int assignPalToBase(ServerCommandSource source, int slot, BaseWorkType workType) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        PlayerPalData palData = PlayerPalData.get(source.getServer());
        List<PalInstance> pals = palData.getStoredPals(player.getUuid());
        if (slot < 0 || slot >= pals.size()) {
            source.sendError(Text.translatable("message.palcraft.command.invalid_slot"));
            return 0;
        }
        if (slot == palData.getActiveSlot(player.getUuid()) && palData.getActiveEntityUuid(player.getUuid()).isPresent()) {
            source.sendError(Text.translatable("message.palcraft.base.assign_active"));
            return 0;
        }

        PalInstance pal = pals.get(slot);
        BaseData.AssignResult result = BaseData.get(source.getServer()).assignPalToNearestBase(player, pal, workType);
        return switch (result.status()) {
            case ASSIGNED -> {
                Text speciesName = Text.translatable(Registries.ENTITY_TYPE.get(pal.speciesId()).getTranslationKey());
                source.sendFeedback(() -> Text.translatable(
                        "message.palcraft.base.assign_success",
                        speciesName,
                        result.base().corePositionText(),
                        result.workType().id()
                ).formatted(Formatting.GREEN), false);
                yield 1;
            }
            case ALREADY_ASSIGNED -> {
                source.sendError(Text.translatable("message.palcraft.base.assign_duplicate", result.base().corePositionText()));
                yield 0;
            }
            case NO_BASE_NEARBY -> {
                source.sendError(Text.translatable("message.palcraft.base.assign_no_base"));
                yield 0;
            }
            case INVALID_PAL -> {
                source.sendError(Text.translatable("message.palcraft.command.invalid_slot"));
                yield 0;
            }
            case NOT_DEPLOYED -> {
                source.sendError(Text.translatable("message.palcraft.base.assign_not_deployed"));
                yield 0;
            }
        };
    }

    private static int summonPal(ServerCommandSource source, int slot) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        PlayerPalData data = PlayerPalData.get(source.getServer());
        List<PalInstance> pals = data.getStoredPals(player.getUuid());
        if (slot < 0 || slot >= pals.size()) {
            source.sendError(Text.translatable("message.palcraft.command.invalid_slot"));
            return 0;
        }

        PalInstance pal = pals.get(slot);
        return data.summon(player, slot)
                .map(errorKey -> {
                    source.sendError(Text.translatable(errorKey));
                    return 0;
                })
                .orElseGet(() -> {
                    Text speciesName = Text.translatable(Registries.ENTITY_TYPE.get(pal.speciesId()).getTranslationKey());
                    source.sendFeedback(() -> Text.translatable("message.palcraft.command.summon_success", slot, speciesName), false);
                    return 1;
                });
    }

    private static int recallPal(ServerCommandSource source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        PlayerPalData data = PlayerPalData.get(source.getServer());
        if (!data.recall(player)) {
            source.sendError(Text.translatable("message.palcraft.command.no_active_pal"));
            return 0;
        }

        source.sendFeedback(() -> Text.translatable("message.palcraft.command.recall_success"), false);
        return 1;
    }
}
