package com.modid.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;

/**
 * Renderer GeckoLib que reemplaza al renderer vanilla del jugador (en el mundo, no en primera persona).
 * El tipo genérico R es el "truco" que exige GeckoLib 5: el estado de render tiene que ser a la vez
 * un LivingEntityRenderState y un GeoRenderState.
 */
public class PlayerGeoRenderer<R extends LivingEntityRenderState & GeoRenderState>
        extends GeoReplacedEntityRenderer<PlayerGeoAnimatable, Player, R> {

    public PlayerGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerGeoModel(), new PlayerGeoAnimatable());
    }
}