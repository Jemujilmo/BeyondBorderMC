package dev.beyondborder.mixin;

import dev.beyondborder.Limits;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * PlayerEntity#tick clamps the player to +/-2.9999999E7 every tick and moves them back if they are outside.
 * That is the one that makes walking across the old limit fail even when everything else is patched.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
	@Redirect(
		method = "tick",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;clamp(DDD)D")
	)
	private double beyondborder$clampPlayer(double value, double min, double max) {
		return Limits.clampCoordinate(value, min, max);
	}
}
