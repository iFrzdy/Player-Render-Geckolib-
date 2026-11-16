package com.modid.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import software.bernie.geckolib.animatable.GeoReplacedEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animatable.processing.AnimationController;
import software.bernie.geckolib.animatable.processing.AnimationTest;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Objeto "animatable" que GeckoLib usa para animar a los jugadores.
 * GeckoLib 5 NO permite usar la entidad directamente, por eso existe este singleton aparte.
 *
 * Nombres de animación que se buscan en player_render.animation.json:
 *   idle, walk, run, sneak, sneak_walk   (controlador "movement")
 *   mine                                 (controlador "action", mientras el jugador golpea/mina)
 * El tipo de loop sale de cada animación en el .json (true = repetir, hold_on_last_frame = mantener la pose final).
 * Las que no existan se reemplazan por la más parecida (ver FALLBACKS) o se ignoran.
 */
public class PlayerGeoAnimatable implements GeoReplacedEntity {
    /** Si falta una animación, se prueba con la siguiente de esta cadena. */
    private static final Map<String, String> FALLBACKS = Map.of(
            "sneak_walk", "sneak",
            "sneak", "idle",
            "run", "walk",
            "walk", "idle"
    );

    private static final Map<String, RawAnimation> PLAYS = new HashMap<>();

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public EntityType<?> getReplacingEntityType() {
        return EntityType.PLAYER;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Orden importa: "action" se registra después, así que pisa a "movement" en los huesos que ambos animan.
        controllers.add(new AnimationController<PlayerGeoAnimatable>("movement", 4, this::movementState));
        controllers.add(new AnimationController<PlayerGeoAnimatable>("action", 2, this::actionState));
    }

    private PlayState movementState(AnimationTest<PlayerGeoAnimatable> test) {
        boolean moving = test.isMoving();
        boolean sprinting = test.getDataOrDefault(DataTickets.SPRINTING, false);
        boolean crouching = test.getDataOrDefault(DataTickets.ENTITY_POSE, Pose.STANDING) == Pose.CROUCHING;

        String name;
        if (crouching) {
            name = moving ? "sneak_walk" : "sneak";
        } else if (moving && sprinting) {
            name = "run";
        } else if (moving) {
            name = "walk";
        } else {
            name = "idle";
        }

        // Si esa animación no existe todavía, bajamos por la cadena de reemplazos
        while (name != null && !PlayerGeoModel.hasAnimation(name)) {
            name = FALLBACKS.get(name);
        }
        if (name == null) {
            return PlayState.STOP;
        }

        return test.setAndContinue(PLAYS.computeIfAbsent(name, n -> RawAnimation.begin().thenPlay(n)));
    }

    private PlayState actionState(AnimationTest<PlayerGeoAnimatable> test) {
        boolean swinging = test.getDataOrDefault(DataTickets.SWINGING_ARM, false);

        if (!swinging || !PlayerGeoModel.hasAnimation("mine")) {
            return PlayState.STOP;
        }

        return test.setAndContinue(PLAYS.computeIfAbsent("mine", n -> RawAnimation.begin().thenPlay(n)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}