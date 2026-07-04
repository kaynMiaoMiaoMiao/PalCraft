package com.bmht.palcraft.client;

import com.bmht.palcraft.PalCraft;
import com.bmht.palcraft.client.render.entity.SparkitEntityRenderer;
import com.bmht.palcraft.client.render.entity.model.SparkitEntityModel;
import com.bmht.palcraft.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;

public class PalCraftClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(SparkitEntityModel.MODEL_LAYER, SparkitEntityModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.CAPTURE_ORB, FlyingItemEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.SPARKIT, SparkitEntityRenderer::new);
        PalCraft.LOGGER.info("Initializing PalCraft client");
    }
}
