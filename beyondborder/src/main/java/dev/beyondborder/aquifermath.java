package dev.beyondborder;

/**
 * Index maths for the aquifer sampler's per-chunk cache. Deliberately free of Minecraft classes.
 *
 * <p>The aquifer stores each cell's centre as a packed {@code BlockPos} long, which keeps only 26 bits of X and Z.
 * Past 33,554,432 blocks the unpacked coordinate comes back exactly 2^26 blocks (2^22 sixteen-block cells) off, the
 * cache index lands millions of slots outside the array and the chunk fails to generate. This puts the coordinate
 * back where it belongs.
 */
public final class AquiferMath {
	/** 2^26 blocks / 16 blocks per cell: how far a wrapped horizontal cell coordinate is off. */
	public static final int WRAP_CELLS = 1 << 22;

	private AquiferMath() {
	}

	/**
	 * Same result as vanilla's {@code index(x, y, z)} whenever the cell is inside the cache; otherwise the horizontal
	 * offsets are un-wrapped, and as a last resort the result is clamped so it can never throw.
	 *
	 * @param length length of the cache array
	 */
	public static int index(int x, int y, int z, int startX, int startY, int startZ, int sizeX, int sizeZ, int length) {
		int i = x - startX;
		int j = y - startY;
		int k = z - startZ;
		if (i < 0 || i >= sizeX) {
			i = Math.floorMod(i, WRAP_CELLS);
		}
		if (k < 0 || k >= sizeZ) {
			k = Math.floorMod(k, WRAP_CELLS);
		}
		int index = (j * sizeZ + k) * sizeX + i;
		if (index < 0) {
			return 0;
		}
		return Math.min(index, length - 1);
	}
}
