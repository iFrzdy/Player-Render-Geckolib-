package com.modid.client.mixin;

import com.modid.client.PlayerGeoRenderer;
import java.util.Map;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Shadow
    private Map<EntityType<?>, EntityRenderer<?, ?>> renderers;

    /**
     * Se engancha SOLO en el método que dibuja entidades en el mundo y cambia el renderer que recibe.
     * No toca getRenderer(), porque la mano en primera persona lo castea a PlayerRenderer y crashearía.
     */
    @ModifyVariable(
            method = "render(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/EntityRenderer;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private EntityRenderer<?, ?> playerrender$useGeoRenderer(EntityRenderer<?, ?> original) {
        if (original instanceof PlayerRenderer) {
            EntityRenderer<?, ?> geo = this.renderers.get(EntityType.PLAYER);
            if (geo instanceof PlayerGeoRenderer<?>) {
                return geo;
            }
        }
        return original;
    }
}