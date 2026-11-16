package com.modid.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animatable.processing.AnimationState;
import software.bernie.geckolib.cache.GeckoLibResources;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;

/**
 * Le dice a GeckoLib dónde están los archivos:
 *  modelo:     assets/player-render/geckolib/models/player_render.geo.json
 *  animación:  assets/player-render/geckolib/animations/player_render.animation.json
 *  textura:    assets/player-render/textures/entity/player_render.png
 *
 * La cabeza se mueve por código (sigue a la cámara, como vanilla).
 * Todo lo demás (caminar, agacharse, etc.) sale de las animaciones del archivo .animation.json.
 */
public class PlayerGeoModel extends GeoModel<PlayerGeoAnimatable> {
    private static final String MOD_ID = "player-render";

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "player_render");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "player_render");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/entity/player_render.png");

    /**
     * Se mueven los huesos "head" y también su copia "head2", por si tu modelo tiene un segundo esqueleto.
     * Si un hueso no existe, simplemente se ignora.
     */
    private static final String[] HEAD_BONES = {"head", "head2"};

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

    /**
     * Devuelve true si el archivo .animation.json está cargado y contiene una animación con ese nombre.
     * Sirve para no llenar el log de errores cuando todavía no creaste alguna animación.
     */
    public static boolean hasAnimation(String name) {
        BakedAnimations baked = GeckoLibResources.getBakedAnimations().get(ANIMATION);
        return baked != null && baked.getAnimation(name) != null;
    }

    @Override
    public void setCustomAnimations(AnimationState<PlayerGeoAnimatable> animationState) {
        // Misma convención y signos que usa GeckoLib en sus propios modelos
        float pitch = animationState.getData(DataTickets.ENTITY_PITCH);
        float yaw = animationState.getData(DataTickets.ENTITY_YAW);

        for (String boneName : HEAD_BONES) {
            getBone(boneName).ifPresent(bone -> {
                bone.setRotX(-pitch * Mth.DEG_TO_RAD);
                bone.setRotY(-yaw * Mth.DEG_TO_RAD);
            });
        }
    }
}