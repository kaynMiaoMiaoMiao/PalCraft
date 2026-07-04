package com.bmht.palcraft.block;

import com.bmht.palcraft.base.BaseData;
import com.bmht.palcraft.network.PalCraftNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BaseCoreBlock extends Block {
    public BaseCoreBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (world instanceof ServerWorld serverWorld && placer instanceof ServerPlayerEntity player) {
            BaseData.BaseView base = BaseData.get(serverWorld.getServer()).createBase(player, serverWorld, pos);
            player.sendMessage(Text.translatable("message.palcraft.base.created", base.corePositionText(), base.radius()), false);
        }
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        super.onBreak(world, pos, state, player);
        if (world instanceof ServerWorld serverWorld && player instanceof ServerPlayerEntity serverPlayer) {
            boolean removed = BaseData.get(serverWorld.getServer()).removeBaseAt(serverWorld, pos, serverPlayer.getUuid());
            if (removed) {
                serverPlayer.sendMessage(Text.translatable("message.palcraft.base.removed", BaseData.formatPos(pos)), false);
            }
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            PalCraftNetworking.openBaseManagementUi(serverPlayer, pos);
        }
        return ActionResult.SUCCESS;
    }
}
