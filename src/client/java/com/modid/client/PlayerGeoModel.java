package com.modid.client;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;

/**
 * Le dice a GeckoLib dónde están los archivos:
 *  modelo:     assets/player-render/geckolib/models/player_render.geo.json
 *  animación:  assets/player-render/geckolib/animations/player_render.animation.json
 *  textura:    assets/player-render/textures/entity/player_render.png
 */
public class PlayerGeoModel extends GeoModel<PlayerGeoAnimatable> {
    private static final String MOD_ID = "player-render";

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "player_render");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "player_render");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/entity/player_render.png");

    @Override
    public ResourceLocation getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(GeoRenderState renderState) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(PlayerGeoAnimatable animatable) {
        return ANIMATION;
    }
}