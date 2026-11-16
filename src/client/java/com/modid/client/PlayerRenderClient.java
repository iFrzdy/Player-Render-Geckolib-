package com.modid.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.world.entity.EntityType;

public class PlayerRenderClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Registramos nuestro renderer bajo EntityType.PLAYER. Vanilla no lo usa para los jugadores
        // (usa su propio mapa), pero queda creado y guardado; el mixin lo busca de ahí.
        EntityRendererRegistry.register(EntityType.PLAYER, context -> new PlayerGeoRenderer<>(context));
    }
}