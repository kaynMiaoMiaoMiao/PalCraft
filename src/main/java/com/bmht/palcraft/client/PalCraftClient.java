package com.bmht.palcraft.client;

import com.bmht.palcraft.PalCraft;
import com.bmht.palcraft.client.network.PalCraftClientNetworking;
import com.bmht.palcraft.client.render.entity.DodoEntityRenderer;
import com.bmht.palcraft.client.render.entity.SparkitEntityRenderer;
import com.bmht.palcraft.client.render.entity.model.BasicPalEntityModel;
import com.bmht.palcraft.client.screen.PalCraftManagementScreen;
import com.bmht.palcraft.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class PalCraftClient implements ClientModInitializer {
    private static KeyBinding openManagementKey;

    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(BasicPalEntityModel.FLAMELING_LAYER, BasicPalEntityModel::flamelingModel);
        EntityModelLayerRegistry.registerModelLayer(BasicPalEntityModel.WATER_SPRITE_LAYER, BasicPalEntityModel::waterSpriteModel);
        EntityModelLayerRegistry.registerModelLayer(BasicPalEntityModel.SPARKIT_LAYER, BasicPalEntityModel::sparkitModel);
        EntityModelLayerRegistry.registerModelLayer(BasicPalEntityModel.WIND_DRAKE_LAYER, BasicPalEntityModel::windDrakeModel);
        EntityModelLayerRegistry.registerModelLayer(BasicPalEntityModel.TREELET_LAYER, BasicPalEntityModel::treeletModel);
        EntityModelLayerRegistry.registerModelLayer(BasicPalEntityModel.ICELIME_LAYER, BasicPalEntityModel::icelimeModel);
        EntityModelLayerRegistry.registerModelLayer(BasicPalEntityModel.MUDLOBA_LAYER, BasicPalEntityModel::mudlobaModel);
        EntityRendererRegistry.register(ModEntities.CAPTURE_ORB, FlyingItemEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.FLAMELING, context -> new SparkitEntityRenderer(context, BasicPalEntityModel.FLAMELING_LAYER, texture("flameling"), 0.35F));
        EntityRendererRegistry.register(ModEntities.WATER_SPRITE, context -> new SparkitEntityRenderer(context, BasicPalEntityModel.WATER_SPRITE_LAYER, texture("water_sprite"), 0.35F));
        EntityRendererRegistry.register(ModEntities.SPARKIT, context -> new SparkitEntityRenderer(context, BasicPalEntityModel.SPARKIT_LAYER, texture("sparkit"), 0.35F));
        EntityRendererRegistry.register(ModEntities.WIND_DRAKE, context -> new SparkitEntityRenderer(context, BasicPalEntityModel.WIND_DRAKE_LAYER, texture("wind_drake"), 0.45F));
        EntityRendererRegistry.register(ModEntities.TREELET, context -> new SparkitEntityRenderer(context, BasicPalEntityModel.TREELET_LAYER, texture("treelet"), 0.4F));
        EntityRendererRegistry.register(ModEntities.ICELIME, context -> new SparkitEntityRenderer(context, BasicPalEntityModel.ICELIME_LAYER, texture("icelime"), 0.4F));
        EntityRendererRegistry.register(ModEntities.MUDLOBA, context -> new SparkitEntityRenderer(context, BasicPalEntityModel.MUDLOBA_LAYER, texture("mudloba"), 0.45F));
        EntityRendererRegistry.register(ModEntities.DODO, DodoEntityRenderer::new);
        PalCraftClientNetworking.registerReceivers();
        openManagementKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.palcraft.management",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "key.category.palcraft"
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openManagementKey.wasPressed()) {
                if (client.player != null) {
                    client.setScreen(PalCraftManagementScreen.player());
                }
            }
        });
        PalCraft.LOGGER.info("Initializing PalCraft client");
    }

    private static Identifier texture(String name) {
        return new Identifier(PalCraft.MOD_ID, "textures/entity/" + name + ".png");
    }
}
