package dev.beyondborder.mixin;

import dev.beyondborder.Limits;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The server clamps every X/Z a client reports to +/-3.0E7 before accepting a movement packet, which would
 * snap a player back at the old limit. Vertical clamping is left alone.
 */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
	@Inject(method = "clampHorizontal", at = @At("HEAD"), cancellable = true)
	private static void beyondborder$clampHorizontal(double value, CallbackInfoReturnable<Double> cir) {
		cir.setReturnValue(Limits.clampHorizontal(value));
	}
}
