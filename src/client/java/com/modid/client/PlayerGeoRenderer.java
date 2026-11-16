package com.modid.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
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

        addRenderLayer(new PlayerHandItemsLayer<>(this));
    }

    /**
     * Captura datos del jugador que las animaciones necesitan (acá: si está tensando un arco y con qué mano).
     */
    @Override
    public void addRenderData(PlayerGeoAnimatable animatable, Player player, R renderState) {
        boolean usingBow = player.isUsingItem() && player.getUseItem().getItem() instanceof BowItem;
        boolean bowInRightHand = false;
        boolean bowInLeftHand = false;

        if (usingBow) {
            HumanoidArm bowArm = player.getUsedItemHand() == InteractionHand.MAIN_HAND
                    ? player.getMainArm()
                    : player.getMainArm().getOpposite();

            bowInRightHand = bowArm == HumanoidArm.RIGHT;
            bowInLeftHand = !bowInRightHand;
        }

        renderState.addGeckolibData(PlayerDataTickets.BOW_IN_RIGHT_HAND, bowInRightHand);
        renderState.addGeckolibData(PlayerDataTickets.BOW_IN_LEFT_HAND, bowInLeftHand);
    }
}