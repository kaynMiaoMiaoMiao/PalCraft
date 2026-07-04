package com.bmht.palcraft.item;

import com.bmht.palcraft.entity.CaptureOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class CaptureOrbItem extends Item {
    public CaptureOrbItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        world.playSound(
                null,
                user.getX(),
                user.getY(),
                user.getZ(),
                SoundEvents.ENTITY_SNOWBALL_THROW,
                SoundCategory.NEUTRAL,
                0.5F,
                0.8F + world.getRandom().nextFloat() * 0.4F
        );

        if (!world.isClient) {
            CaptureOrbEntity captureOrbEntity = new CaptureOrbEntity(world, user);
            ItemStack thrownStack = stack.copy();
            thrownStack.setCount(1);
            captureOrbEntity.setItem(thrownStack);
            captureOrbEntity.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F, 1.5F, 1.0F);
            world.spawnEntity(captureOrbEntity);
        }

        user.incrementStat(Stats.USED.getOrCreateStat(this));

        if (!user.getAbilities().creativeMode) {
            stack.decrement(1);
        }

        return TypedActionResult.success(stack, world.isClient());
    }
}
