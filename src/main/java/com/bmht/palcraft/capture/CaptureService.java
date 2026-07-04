package com.bmht.palcraft.capture;

import com.bmht.palcraft.PalCraft;
import com.bmht.palcraft.partner.PalInstance;
import com.bmht.palcraft.partner.PlayerPalData;
import com.bmht.palcraft.registry.tag.ModEntityTypeTags;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public final class CaptureService {
    private static final double BASE_CAPTURE_CHANCE = 0.35D;
    private static final double LOW_HEALTH_BONUS = 0.45D;
    private static final double MIN_CAPTURE_CHANCE = 0.05D;
    private static final double MAX_CAPTURE_CHANCE = 0.90D;

    private CaptureService() {
    }

    public static CaptureResult tryCapture(Entity hitEntity, Entity owner) {
        if (!(hitEntity instanceof LivingEntity target) || !(owner instanceof PlayerEntity player)) {
            return CaptureResult.notAttempted();
        }

        if (!isCatchable(target)) {
            return CaptureResult.notAttempted();
        }

        double chance = calculateCaptureChance(target);
        boolean success = target.getRandom().nextDouble() < chance;

        if (!success) {
            playFailureFeedback(target.getWorld(), target);
            PalCraft.LOGGER.info("Capture failed for {} with chance {}", target.getType(), chance);
            return CaptureResult.failed(chance);
        }

        PalInstance palInstance = PalInstance.fromCapturedEntity(target, player);
        if (player instanceof ServerPlayerEntity serverPlayer) {
            PlayerPalData.get(serverPlayer.getServer()).addCapturedPal(serverPlayer, palInstance);
        }
        player.sendMessage(Text.translatable("message.palcraft.capture_success", target.getDisplayName()), false);
        target.discard();
        playSuccessFeedback(target.getWorld(), target);
        PalCraft.LOGGER.info("Captured {} as {} for {}", palInstance.speciesId(), palInstance.instanceUuid(), player.getGameProfile().getName());
        return CaptureResult.succeeded(chance, palInstance);
    }

    private static boolean isCatchable(LivingEntity target) {
        return target.getType().isIn(ModEntityTypeTags.CATCHABLE);
    }

    private static double calculateCaptureChance(LivingEntity target) {
        double healthFactor = 1.0D - (target.getHealth() / target.getMaxHealth());
        return MathHelper.clamp(
                BASE_CAPTURE_CHANCE + healthFactor * LOW_HEALTH_BONUS,
                MIN_CAPTURE_CHANCE,
                MAX_CAPTURE_CHANCE
        );
    }

    private static void playSuccessFeedback(World world, LivingEntity target) {
        world.playSound(
                null,
                target.getBlockPos(),
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                SoundCategory.PLAYERS,
                0.8F,
                1.4F
        );
    }

    private static void playFailureFeedback(World world, LivingEntity target) {
        world.playSound(
                null,
                target.getBlockPos(),
                SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(),
                SoundCategory.PLAYERS,
                0.8F,
                0.7F
        );
    }
}
