package com.modid.client.mixin;

import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Da acceso a la lista privada "cubes" de ModelPart (GeckoLib la lee con un access widener propio,
 * que no se aplica a nuestro mod).
 */
@Mixin(ModelPart.class)
public interface ModelPartAccessor {
    @Accessor("cubes")
    List<ModelPart.Cube> playerrender$getCubes();
}