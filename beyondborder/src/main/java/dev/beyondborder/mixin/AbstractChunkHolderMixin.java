package dev.beyondborder.mixin;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.AbstractChunkHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The server refuses to create a chunk more than a fixed number of chunks from the origin
 * ("Trying to create chunk out of reasonable bounds"), which crashes the world the moment a player is
 * teleported, or walks, a little past ~30,001,000 blocks.
 *
 * <p>SPECULATIVE: 1.21.6's mapping doesn't name this check, so the exact shape of the bytecode is a
 * best guess. Two variants are tried, and either one that matches defuses the check. Both use
 * {@code require = 0}, so if neither matches the game still launches (and still crashes at the same
 * spot, which is how you'd know). tools/scan_limits.py prints the real constructor so this can be made exact.
 */
@Mixin(AbstractChunkHolder.class)
public abstract class AbstractChunkHolderMixin {
	/** Variant A: the check is {@code pos.getChebyshevDistance(ChunkPos.ORIGIN) > MAX}. Report distance 0. */
	@Redirect(
		method = "<init>",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/ChunkPos;getChebyshevDistance(Lnet/minecraft/util/math/ChunkPos;)I"),
		require = 0
	)
	private int beyondborder$distanceAlwaysZero(ChunkPos self, ChunkPos other) {
		return 0;
	}

	/** Variant B: the limit is read from {@code ChunkPos.MAX_COORDINATE} (opcode 178 = GETSTATIC). Make it huge. */
	@Redirect(
		method = "<init>",
		at = @At(value = "FIELD", target = "Lnet/minecraft/util/math/ChunkPos;MAX_COORDINATE:I", opcode = 178),
		require = 0
	)
	private int beyondborder$unboundedMaxCoordinate() {
		return Integer.MAX_VALUE;
	}
}
