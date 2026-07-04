package com.bmht.palcraft.command;

import com.bmht.palcraft.partner.PalInstance;
import com.bmht.palcraft.partner.PlayerPalData;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

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

        source.sendFeedback(() -> Text.translatable("message.palcraft.command.list_header", pals.size()), false);
        int activeSlot = data.getActiveSlot(player.getUuid());
        for (int i = 0; i < pals.size(); i++) {
            PalInstance pal = pals.get(i);
            Text speciesName = Text.translatable(Registries.ENTITY_TYPE.get(pal.speciesId()).getTranslationKey());
            Text activeMarker = i == activeSlot && data.getActiveEntityUuid(player.getUuid()).isPresent()
                    ? Text.translatable("message.palcraft.command.active_marker")
                    : Text.empty();
            int slot = i;
            source.sendFeedback(
                    () -> Text.translatable("message.palcraft.command.list_entry", slot, speciesName, pal.level(), Math.round(pal.health()), Math.round(pal.maxHealth()), activeMarker),
                    false
            );
        }
        return pals.size();
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
