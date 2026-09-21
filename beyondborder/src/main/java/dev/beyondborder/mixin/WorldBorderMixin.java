package dev.beyondborder.mixin;

import dev.beyondborder.Limits;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.world.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Pushes the world border out to +/-limit, turns off its collision wall and its damage.
 *
 * <p>Vanilla's contains/clamp/distance helpers all go through the four getBound* methods, so overriding those
 * moves every border check at once. The vanilla area (capped at 5.9999968E7 wide) is never consulted.
 * This needs to be installed on both the client and the server.
 */
@Mixin(WorldBorder.class)
public abstract class WorldBorderMixin {
	@Inject(method = "getBoundWest", at = @At("HEAD"), cancellable = true)
	private void beyondborder$boundWest(CallbackInfoReturnable<Double> cir) {
		cir.setReturnValue(-Limits.limit());
	}

	@Inject(method = "getBoundEast", at = @At("HEAD"), cancellable = true)
	private void beyondborder$boundEast(CallbackInfoReturnable<Double> cir) {
		cir.setReturnValue(Limits.limit());
	}

	@Inject(method = "getBoundNorth", at = @At("HEAD"), cancellable = true)
	private void beyondborder$boundNorth(CallbackInfoReturnable<Double> cir) {
		cir.setReturnValue(-Limits.limit());
	}

	@Inject(method = "getBoundSouth", at = @At("HEAD"), cancellable = true)
	private void beyondborder$boundSouth(CallbackInfoReturnable<Double> cir) {
		cir.setReturnValue(Limits.limit());
	}

	/** Belt and braces: the vanilla border shape must never block movement. */
	@Inject(method = "canCollide", at = @At("HEAD"), cancellable = true)
	private void beyondborder$noCollision(Entity entity, Box box, CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(false);
	}

	/** Belt and braces: nobody takes border damage, even if some path still reports "outside". */
	@Inject(method = "getDamagePerBlock", at = @At("HEAD"), cancellable = true)
	private void beyondborder$noDamage(CallbackInfoReturnable<Double> cir) {
		cir.setReturnValue(0.0);
	}
}
