package com.modid.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animatable.processing.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;

/**
 * Le dice a GeckoLib dónde están los archivos:
 *  modelo:     assets/player-render/geckolib/models/player_render.geo.json
 *  animación:  assets/player-render/geckolib/animations/player_render.animation.json
 *  textura:    assets/player-render/textures/entity/player_render.png
 *
 * Además mueve los huesos por código (cabeza que mira y balanceo al caminar), como vanilla.
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
     * Se mueven los huesos con estos nombres y también su copia con sufijo "2" (head2, right_arm2...),
     * por si tu modelo tiene un segundo esqueleto. Si un hueso no existe, simplemente se ignora.
     */
    private static final String[] BONE_SUFFIXES = {"", "2"};

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

    @Override
    public void setCustomAnimations(AnimationState<PlayerGeoAnimatable> animationState) {
        // Hacia dónde mira (misma convención y signos que usa GeckoLib en sus propios modelos)
        float pitch = animationState.getData(DataTickets.ENTITY_PITCH);
        float yaw = animationState.getData(DataTickets.ENTITY_YAW);

        // Datos de caminata del estado de render de vanilla
        float walkPos = 0f;
        float walkSpeed = 0f;
        if (animationState.renderState() instanceof LivingEntityRenderState living) {
            walkPos = living.walkAnimationPos;
            walkSpeed = living.walkAnimationSpeed;
        }

        // Mismas fórmulas que el modelo humanoide de vanilla
        float swing = walkPos * 0.6662f;
        float rightArm = Mth.cos(swing + (float) Math.PI) * walkSpeed;
        float leftArm = Mth.cos(swing) * walkSpeed;
        float rightLeg = Mth.cos(swing) * 1.4f * walkSpeed;
        float leftLeg = Mth.cos(swing + (float) Math.PI) * 1.4f * walkSpeed;

        for (String suffix : BONE_SUFFIXES) {
            getBone("head" + suffix).ifPresent(bone -> {
                bone.setRotX(-pitch * Mth.DEG_TO_RAD);
                bone.setRotY(-yaw * Mth.DEG_TO_RAD);
            });
            getBone("right_arm" + suffix).ifPresent(bone -> bone.setRotX(rightArm));
            getBone("left_arm" + suffix).ifPresent(bone -> bone.setRotX(leftArm));
            getBone("right_leg" + suffix).ifPresent(bone -> bone.setRotX(rightLeg));
            getBone("left_leg" + suffix).ifPresent(bone -> bone.setRotX(leftLeg));
        }
    }
}