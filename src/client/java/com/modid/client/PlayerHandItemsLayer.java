package com.modid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.renderer.base.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * Dibuja los ítems que el jugador tiene en las manos.
 *
 * Es una copia adaptada de ItemInHandGeoLayer de GeckoLib: la original exige que el animatable sea una
 * entidad, y GeckoLib 5 no lo permite en renderers de reemplazo. Acá leemos el equipo desde el Player.
 *
 * Tu modelo necesita dos huesos SIN cubos, hijos de cada brazo, con el pivote en la mano:
 *   right_item  (hijo de right_arm, pivote [-6, 12, 0])
 *   left_item   (hijo de left_arm,  pivote [ 6, 12, 0])
 * Si un hueso no existe, simplemente no se dibuja el ítem de esa mano.
 */
public class PlayerHandItemsLayer<R extends GeoRenderState> extends BlockAndItemGeoLayer<PlayerGeoAnimatable, Player, R> {
    private static final String RIGHT_HAND_BONE = "right_item";
    private static final String LEFT_HAND_BONE = "left_item";

    /** Ajuste fino de posición del ítem (en bloques; 1/16 = 1 píxel). Si el ítem no te queda justo en la mano, tocá esto. */
    private static final float EXTRA_X = 0f;
    private static final float EXTRA_Y = 0f;
    private static final float EXTRA_Z = 0f;

    public PlayerHandItemsLayer(GeoRenderer<PlayerGeoAnimatable, Player, R> renderer) {
        super(renderer);
    }

    @Override
    protected List<RenderData<R>> getRelevantBones(R renderState, BakedGeoModel model) {
        boolean isLeftHanded = renderState.getOrDefaultGeckolibData(DataTickets.IS_LEFT_HANDED, false);
        List<RenderData<R>> bones = new ArrayList<>(2);

        if (model.getBone(RIGHT_HAND_BONE).isPresent()) {
            bones.add(renderDataForHand(RIGHT_HAND_BONE, HumanoidArm.RIGHT, isLeftHanded));
        }
        if (model.getBone(LEFT_HAND_BONE).isPresent()) {
            bones.add(renderDataForHand(LEFT_HAND_BONE, HumanoidArm.LEFT, isLeftHanded));
        }

        return bones;
    }

    private static <R extends GeoRenderState> RenderData<R> renderDataForHand(String boneName, HumanoidArm arm, boolean isLeftHanded) {
        HumanoidArm mainHandArm = isLeftHanded ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
        EquipmentSlot slot = arm == mainHandArm ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;

        ItemDisplayContext context = switch (slot) {
            case MAINHAND -> mainHandArm == HumanoidArm.RIGHT ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
            case OFFHAND -> mainHandArm == HumanoidArm.RIGHT ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
            default -> ItemDisplayContext.NONE;
        };

        return new RenderData<>(boneName, context, (bone, renderState) ->
                Either.left((ItemStack) renderState.getGeckolibData(DataTickets.EQUIPMENT_BY_SLOT).get(slot)));
    }

    /**
     * Guarda en el estado de render lo que el jugador tiene en las manos. Después del cuadro no se vuelve a leer la entidad.
     */
    @Override
    public void addRenderData(PlayerGeoAnimatable animatable, Player player, R renderState) {
        EnumMap<EquipmentSlot, ItemStack> equipment = renderState.getOrDefaultGeckolibData(DataTickets.EQUIPMENT_BY_SLOT, new EnumMap<>(EquipmentSlot.class));

        equipment.put(EquipmentSlot.MAINHAND, player.getMainHandItem());
        equipment.put(EquipmentSlot.OFFHAND, player.getOffhandItem());

        renderState.addGeckolibData(DataTickets.EQUIPMENT_BY_SLOT, equipment);
        renderState.addGeckolibData(DataTickets.IS_LEFT_HANDED, player.getMainArm() == HumanoidArm.LEFT);
    }

    @Override
    protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, ItemDisplayContext displayContext, R renderState,
                                      MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            poseStack.mulPose(Axis.XN.rotationDegrees(90f));
            poseStack.translate(0, 0.125f, -0.0625f);

            if (stack.getItem() instanceof ShieldItem) {
                poseStack.translate(0, 0.125, -0.25);
            }
        } else if (displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-90f));
            poseStack.translate(0, 0.125f, -0.0625f);

            if (stack.getItem() instanceof ShieldItem) {
                poseStack.translate(0, 0.125, 0.25);
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
            }
        }

        poseStack.translate(EXTRA_X, EXTRA_Y, EXTRA_Z);

        super.renderStackForBone(poseStack, bone, stack, displayContext, renderState, bufferSource, packedLight, packedOverlay);
    }
}