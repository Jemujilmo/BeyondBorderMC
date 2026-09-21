package dev.beyondborder.mixin;

import dev.beyondborder.AquiferMath;
import net.minecraft.world.gen.chunk.AquiferSampler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fixes "ArrayIndexOutOfBoundsException: Index -16776903 out of bounds for length 315" while generating a chunk
 * past 2^25 blocks (seen at X/Z about 34,000,000): the aquifer keeps cell centres as packed BlockPos longs (26 bits
 * per horizontal axis), so the decoded coordinate is 2^26 blocks off and the cache lookup falls off the array.
 * The exception left the chunk permanently ungenerated, which froze teleports and Save and Quit.
 */
@Mixin(AquiferSampler.Impl.class)
public abstract class AquiferSamplerImplMixin {
	@Shadow @Final private int startX;
	@Shadow @Final private int startY;
	@Shadow @Final private int startZ;
	@Shadow @Final private int sizeX;
	@Shadow @Final private int sizeZ;
	/** Same length as the water level cache, so it stands in for it. */
	@Shadow @Final private long[] blockPositions;

	@Inject(method = "index(III)I", at = @At("HEAD"), cancellable = true)
	private void beyondborder$unwrapIndex(int x, int y, int z, CallbackInfoReturnable<Integer> cir) {
		cir.setReturnValue(AquiferMath.index(x, y, z, startX, startY, startZ, sizeX, sizeZ, blockPositions.length));
	}
}
