package com.modid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import com.modid.client.mixin.ModelPartAccessor;
import net.minecraft.Util;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.GeckoLibConstants;
import software.bernie.geckolib.GeckoLibServices;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.renderer.base.GeoRenderer;
import software.bernie.geckolib.renderer.base.PerBoneRender;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.service.GeckoLibClient;
import software.bernie.geckolib.util.RenderUtil;

/**
 * Dibuja la armadura del jugador sobre el modelo GeckoLib.
 *
 * Es una copia adaptada de ItemArmorGeoLayer (GeckoLib 5.1.0). La original exige que el animatable sea una
 * LivingEntity; el nuestro es un singleton, así que acá leemos el equipo directamente desde el Player.
 *
 * Funciona con armaduras vanilla, de otros mods y con armaduras GeckoLib (GeoArmorRenderer).
 *
 * Huesos que usa (todos tienen que tener al menos un cubo, porque se escala con el primer cubo):
 *   head, torso, right_arm, left_arm, right_leg, left_leg
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class PlayerArmorLayer<R extends LivingEntityRenderState & GeoRenderState> extends GeoRenderLayer<PlayerGeoAnimatable, Player, R> {
    protected final EquipmentLayerRenderer equipmentRenderer;
    protected final EquipmentAssetManager equipmentAssets;
    protected final Function<SkullBlock.Type, SkullModelBase> skullModels;

    /**
     * Cuánto de la rotación del hueso se copia a la pieza de armadura (multiplicador por eje).
     *   0      = no se copia (el PoseStack ya trae la rotación del hueso)   <- valor actual
     *   -1 / 1 = se copia con ese signo
     * Los valores del ItemArmorGeoLayer original son ROT_X = -1, ROT_Y = -1, ROT_Z = 1.
     */
    private static final float ROT_X = 0f;
    private static final float ROT_Y = 0f;
    private static final float ROT_Z = 0f;

    public PlayerArmorLayer(GeoRenderer<PlayerGeoAnimatable, Player, R> geoRenderer, EntityRendererProvider.Context context) {
        super(geoRenderer);

        this.equipmentRenderer = context.getEquipmentRenderer();
        this.equipmentAssets = context.getEquipmentAssets();
        this.skullModels = Util.memoize(type -> SkullBlockRenderer.createModel(Minecraft.getInstance().getEntityModels(), type));
    }

    /**
     * Relación hueso -> pieza de armadura. Si un hueso no existe en el modelo, se ignora con un aviso en el log.
     */
    private static final List<RenderData> BONES = List.of(
            RenderData.head("head"),
            RenderData.body("torso_armor"),
            new RenderData("torso_armor", EquipmentSlot.LEGS, model -> model.body),
            RenderData.rightArm("right_arm"),
            RenderData.leftArm("left_arm"),
            RenderData.rightLeg("right_leg"),
            RenderData.leftLeg("left_leg"),
            RenderData.rightFoot("right_leg"),
            RenderData.leftFoot("left_leg")
    );

    protected List<RenderData> getRelevantBones(R renderState, BakedGeoModel model) {
        return BONES;
    }

    public record RenderData(String boneName, EquipmentSlot slot, Function<HumanoidModel<?>, ModelPart> modelPartFactory) {
        public static RenderData head(String boneName) {
            return new RenderData(boneName, EquipmentSlot.HEAD, model -> model.head);
        }

        public static RenderData body(String boneName) {
            return new RenderData(boneName, EquipmentSlot.CHEST, model -> model.body);
        }

        public static RenderData leftArm(String boneName) {
            return new RenderData(boneName, EquipmentSlot.CHEST, model -> model.leftArm);
        }

        public static RenderData rightArm(String boneName) {
            return new RenderData(boneName, EquipmentSlot.CHEST, model -> model.rightArm);
        }

        public static RenderData leftLeg(String boneName) {
            return new RenderData(boneName, EquipmentSlot.LEGS, model -> model.leftLeg);
        }

        public static RenderData rightLeg(String boneName) {
            return new RenderData(boneName, EquipmentSlot.LEGS, model -> model.rightLeg);
        }

        public static RenderData leftFoot(String boneName) {
            return new RenderData(boneName, EquipmentSlot.FEET, model -> model.leftLeg);
        }

        public static RenderData rightFoot(String boneName) {
            return new RenderData(boneName, EquipmentSlot.FEET, model -> model.rightLeg);
        }
    }

    /**
     * Guarda en el estado de render lo que el jugador lleva puesto. Después del cuadro no se vuelve a leer la entidad.
     * Comparte el ticket EQUIPMENT_BY_SLOT con PlayerHandItemsLayer: cada uno parte del mapa existente y agrega lo suyo.
     */
    @Override
    public void addRenderData(PlayerGeoAnimatable animatable, Player player, R renderState) {
        EnumMap<EquipmentSlot, ItemStack> equipment = renderState.getOrDefaultGeckolibData(DataTickets.EQUIPMENT_BY_SLOT, new EnumMap<>(EquipmentSlot.class));

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            equipment.put(slot, player.getItemBySlot(slot));
        }

        renderState.addGeckolibData(DataTickets.EQUIPMENT_BY_SLOT, equipment);

        if (!equipment.get(EquipmentSlot.CHEST).isEmpty()) {
            float partialTick = renderState.getGeckolibData(DataTickets.PARTIAL_TICK);

            renderState.addGeckolibData(DataTickets.ELYTRA_ROTATION, new Vec3(player.elytraAnimationState.getRotX(partialTick),
                    player.elytraAnimationState.getRotY(partialTick),
                    player.elytraAnimationState.getRotZ(partialTick)));
        }
    }

    @Override
    public void addPerBoneRender(R renderState, BakedGeoModel model, BiConsumer<GeoBone, PerBoneRender<R>> consumer) {
        // Se agrupan las piezas por hueso: un hueso puede llevar varias (ej. pantalón y botas en la misma pierna).
        // Se registra UN solo render por hueso, así las piezas no se pisan entre sí.
        Map<String, List<RenderData>> piecesByBone = new LinkedHashMap<>();

        for (RenderData renderData : getRelevantBones(renderState, model)) {
            piecesByBone.computeIfAbsent(renderData.boneName, name -> new ArrayList<>()).add(renderData);
        }

        piecesByBone.forEach((boneName, pieces) -> model.getBone(boneName).ifPresentOrElse(
                bone -> createPerBoneRender(bone, pieces, consumer, renderState),
                () -> GeckoLibConstants.LOGGER.error("Unable to find bone for PlayerArmorLayer: {}, skipping", boneName)));
    }

    private void createPerBoneRender(GeoBone bone, List<RenderData> pieces, BiConsumer<GeoBone, PerBoneRender<R>> consumer, R renderState) {
        List<RenderData> toRender = new ArrayList<>();
        List<ItemStack> stacks = new ArrayList<>();

        for (RenderData piece : pieces) {
            ItemStack stack = getEquipmentStack(bone, piece.slot, renderState);

            if (!stack.isEmpty()) {
                toRender.add(piece);
                stacks.add(stack);
            }
        }

        if (toRender.isEmpty())
            return;

        consumer.accept(bone, (renderState2, poseStack, bone2, renderType, bufferSource, packedLight, packedOverlay, renderColor) -> {
            for (int i = 0; i < toRender.size(); i++) {
                RenderData piece = toRender.get(i);

                renderForBone(renderState2, piece.slot, piece.modelPartFactory, stacks.get(i), poseStack, bone2, renderType, bufferSource, packedLight, packedOverlay, renderColor);
            }
        });
    }

    protected void renderForBone(R renderState, EquipmentSlot slot, Function<HumanoidModel<?>, ModelPart> modelPartFactory, ItemStack equipmentStack, PoseStack poseStack, GeoBone bone,
                                 RenderType renderType, MultiBufferSource bufferSource, int packedLight, int packedOverlay, int renderColor) {
        if (equipmentStack.isEmpty())
            return;

        if (equipmentStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof AbstractSkullBlock skullBlock) {
            // Las cabezas (skulls) solo se dibujan sobre el hueso de la cabeza
            if (slot == EquipmentSlot.HEAD)
                renderSkullAsArmor(poseStack, bone, equipmentStack, skullBlock, bufferSource, packedLight);
        }
        else {
            HumanoidModel<?> model = getHumanoidModelForRender(bone, slot, equipmentStack, renderState);
            ModelPart modelPart = modelPartFactory.apply(model);

            if (!modelPart.isEmpty()) {
                poseStack.pushPose();
                poseStack.scale(-1, -1, 1);

                if (model instanceof GeoArmorRenderer<?, ?> geoArmorRenderer) {
                    prepHumanoidModelForRender(poseStack, bone, modelPart);
                    geoArmorRenderer.applyBoneVisibilityByPart(slot, modelPart, model);
                    geoArmorRenderer.renderToBuffer(poseStack, null, packedLight, packedOverlay, 0xFFFFFFFF);
                }
                else {
                    Equippable equippable = equipmentStack.get(DataComponents.EQUIPPABLE);

                    if (equippable != null) {
                        equippable.assetId().ifPresent(assetId -> {
                            prepHumanoidModelForRender(poseStack, bone, modelPart);
                            renderVanillaArmorPiece(poseStack, renderState, bone, slot, equipmentStack, equippable, assetId, model, modelPart, bufferSource, packedLight, packedOverlay);
                        });
                    }
                }

                poseStack.popPose();
            }
        }
    }

    protected ItemStack getEquipmentStack(GeoBone bone, EquipmentSlot slot, R renderState) {
        return (ItemStack) renderState.getGeckolibData(DataTickets.EQUIPMENT_BY_SLOT).getOrDefault(slot, ItemStack.EMPTY);
    }

    protected EquipmentClientInfo.LayerType getEquipmentLayerType(R renderState, GeoBone bone, EquipmentSlot slot, ItemStack armorStack, ResourceKey<EquipmentAsset> assetId) {
        if (slot == EquipmentSlot.LEGS)
            return EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS;

        if (slot == EquipmentSlot.CHEST && !this.equipmentAssets.get(assetId).getLayers(EquipmentClientInfo.LayerType.WINGS).isEmpty())
            return EquipmentClientInfo.LayerType.WINGS;

        return EquipmentClientInfo.LayerType.HUMANOID;
    }

    protected void renderVanillaArmorPiece(PoseStack poseStack, R renderState, GeoBone bone, EquipmentSlot slot, ItemStack armorStack,
                                           Equippable equippable, ResourceKey<EquipmentAsset> assetId, HumanoidModel<?> model, ModelPart modelPart,
                                           MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        EquipmentClientInfo.LayerType layerType = getEquipmentLayerType(renderState, bone, slot, armorStack, assetId);
        Model modelToRender = model;

        if (layerType == EquipmentClientInfo.LayerType.WINGS) {
            if (modelPart != model.body)
                return;

            modelToRender = checkForElytraModel(layerType, renderState, bone, poseStack);
        }
        else {
            setVanillaModelPartVisibility(renderState, armorStack, bone, model, modelPart, slot);
        }

        this.equipmentRenderer.renderLayers(layerType, assetId, modelToRender,
                armorStack, poseStack, bufferSource, packedLight);
    }

    protected Model checkForElytraModel(EquipmentClientInfo.LayerType layerType, R renderState, GeoBone bone, PoseStack poseStack) {
        ElytraModel model = GeckoLibClient.GENERIC_ELYTRA_MODEL.get();
        HumanoidRenderState humanoidRenderState = new HumanoidRenderState();
        Vec3 elytraRotation = renderState.getOrDefaultGeckolibData(DataTickets.ELYTRA_ROTATION, Vec3.ZERO);
        humanoidRenderState.isCrouching = renderState.getOrDefaultGeckolibData(DataTickets.IS_CROUCHING, false);
        humanoidRenderState.elytraRotX = (float) elytraRotation.x;
        humanoidRenderState.elytraRotY = (float) elytraRotation.y;
        humanoidRenderState.elytraRotZ = (float) elytraRotation.z;

        model.setupAnim(humanoidRenderState);
        poseStack.translate(0, -1.5f, 0.125f);

        return model;
    }

    protected void setVanillaModelPartVisibility(R renderState, ItemStack armorStack, GeoBone bone, HumanoidModel<?> baseModel, ModelPart modelPart, EquipmentSlot slot) {
        baseModel.setAllVisible(false);
        modelPart.visible = true;


    }

    protected <S extends HumanoidRenderState & GeoRenderState> HumanoidModel<?> getHumanoidModelForRender(GeoBone bone, EquipmentSlot slot, ItemStack stack, R renderState) {
        S humanoidRenderState = renderState instanceof HumanoidRenderState humanoidRenderState1 ? (S) humanoidRenderState1 : (S) new HumanoidRenderState();
        EquipmentClientInfo.LayerType layerType = slot == EquipmentSlot.LEGS ? EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS : EquipmentClientInfo.LayerType.HUMANOID;
        HumanoidModel defaultModel = slot == EquipmentSlot.LEGS ? GeckoLibClient.GENERIC_INNER_ARMOR_MODEL.get() : GeckoLibClient.GENERIC_OUTER_ARMOR_MODEL.get();

        return GeckoLibServices.Client.ITEM_RENDERING.<S>getArmorModelForItem(humanoidRenderState, stack, slot, layerType, defaultModel);
    }

    protected void renderSkullAsArmor(PoseStack poseStack, GeoBone bone, ItemStack stack, AbstractSkullBlock skullBlock, MultiBufferSource bufferSource, int packedLight) {
        SkullBlock.Type type = skullBlock.getType();
        SkullModelBase model = this.skullModels.apply(type);
        RenderType renderType = SkullBlockRenderer.getRenderType(type, stack.get(DataComponents.PROFILE));

        poseStack.pushPose();
        RenderUtil.translateAndRotateMatrixForBone(poseStack, bone);
        poseStack.scale(1.1875f, 1.1875f, 1.1875f);
        poseStack.translate(-0.5f, 0, -0.5f);
        SkullBlockRenderer.renderSkull(null, 0, 0, poseStack, bufferSource, packedLight, model, renderType);
        poseStack.popPose();
    }

    /**
     * Posiciona y escala la pieza de armadura según el hueso. Usa el PRIMER cubo del hueso para calcular la escala.
     */
    protected void prepHumanoidModelForRender(PoseStack poseStack, GeoBone bone, ModelPart sourcePart) {
        float scaleX = 1f;
        float scaleY = 1f;
        float scaleZ = 1f;

        // Si el hueso tiene cubos, se escala al tamaño del primero. Si es un hueso vacío (como torso_armor),
        // la escala queda en 1, que sirve para proporciones de Steve (el modelo vanilla de armadura).
        if (!bone.getCubes().isEmpty()) {
            final GeoCube firstCube = bone.getCubes().getFirst();
            final ModelPart.Cube armorCube = ((ModelPartAccessor) (Object) sourcePart).playerrender$getCubes().getFirst();
            final double armorBoneSizeX = firstCube.size().x();
            final double armorBoneSizeY = firstCube.size().y();
            final double armorBoneSizeZ = firstCube.size().z();
            final double actualArmorSizeX = Math.abs(armorCube.maxX - armorCube.minX);
            final double actualArmorSizeY = Math.abs(armorCube.maxY - armorCube.minY);
            final double actualArmorSizeZ = Math.abs(armorCube.maxZ - armorCube.minZ);
            scaleX = (float) (armorBoneSizeX / actualArmorSizeX);
            scaleY = (float) (armorBoneSizeY / actualArmorSizeY);
            scaleZ = (float) (armorBoneSizeZ / actualArmorSizeZ);
        }

        sourcePart.setPos(-(bone.getPivotX() - ((bone.getPivotX() * scaleX) - bone.getPivotX()) / scaleX),
                -(bone.getPivotY() - ((bone.getPivotY() * scaleY) - bone.getPivotY()) / scaleY),
                (bone.getPivotZ() - ((bone.getPivotZ() * scaleZ) - bone.getPivotZ()) / scaleZ));

        sourcePart.xRot = ROT_X * bone.getRotX();
        sourcePart.yRot = ROT_Y * bone.getRotY();
        sourcePart.zRot = ROT_Z * bone.getRotZ();

        poseStack.scale(scaleX, scaleY, scaleZ);
    }
}