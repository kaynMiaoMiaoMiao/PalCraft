package com.bmht.palcraft.client;

import com.bmht.palcraft.PalCraft;
import com.bmht.palcraft.client.network.PalCraftClientNetworking;
import com.bmht.palcraft.client.render.entity.SparkitEntityRenderer;
import com.bmht.palcraft.client.render.entity.model.SparkitEntityModel;
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
import org.lwjgl.glfw.GLFW;

public class PalCraftClient implements ClientModInitializer {
    private static KeyBinding openManagementKey;

    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(SparkitEntityModel.MODEL_LAYER, SparkitEntityModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.CAPTURE_ORB, FlyingItemEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.SPARKIT, SparkitEntityRenderer::new);
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
}
