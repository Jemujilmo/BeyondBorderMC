package dev.beyondborder.mixin;

import dev.beyondborder.Limits;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla treats every block position beyond +/-30,000,000 on X or Z as invalid, which stops setBlockState and
 * makes /tp refuse. This swaps that hard-coded check for the configured limit.
 */
@Mixin(World.class)
public abstract class WorldMixin {
	@Inject(method = "isValidHorizontally", at = @At("HEAD"), cancellable = true)
	private static void beyondborder$validHorizontally(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(Limits.isWithinLimit(pos.getX()) && Limits.isWithinLimit(pos.getZ()));
	}
}
