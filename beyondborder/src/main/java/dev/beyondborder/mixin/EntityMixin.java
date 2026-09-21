package dev.beyondborder.mixin;

import dev.beyondborder.Limits;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Entity#updatePosition clamps X/Z to +/-3.0E7 whenever an entity is placed (teleports, respawns, packets), and
 * Entity#readData does the same when an entity is loaded from disk. The second one is why a player who logs out
 * beyond the old limit would otherwise be moved back on rejoin.
 *
 * <p>Both redirect vanilla's MathHelper.clamp(DDD)D calls. Horizontal clamps use the configured limit; the
 * vertical clamp is unchanged (see {@link Limits#clampCoordinate}).
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
	@Redirect(
		method = "updatePosition",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;clamp(DDD)D")
	)
	private double beyondborder$clampOnPlace(double value, double min, double max) {
		return Limits.clampCoordinate(value, min, max);
	}

	/** require = 0: only matters for entities saved beyond the old limit; shouldn't stop the game launching. */
	@Redirect(
		method = "readData",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;clamp(DDD)D"),
		require = 0
	)
	private double beyondborder$clampOnLoad(double value, double min, double max) {
		return Limits.clampCoordinate(value, min, max);
	}
}
