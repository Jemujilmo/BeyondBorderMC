package dev.beyondborder;

/** Shared maths for the mixins. Deliberately free of Minecraft classes. */
public final class Limits {
	private static final double LIMIT = BeyondBorderConfig.get().limit();

	/**
	 * Vanilla's horizontal clamps are +/-3.0E7 (Entity) and +/-2.9999999E7 (PlayerEntity#tick); its vertical
	 * clamp is +/-2.0E7. Anything above this threshold is treated as a horizontal clamp.
	 */
	private static final double HORIZONTAL_THRESHOLD = 2.5E7;

	private Limits() {
	}

	public static double limit() {
		return LIMIT;
	}

	public static double clampHorizontal(double value) {
		return Math.max(-LIMIT, Math.min(LIMIT, value));
	}

	/**
	 * Drop-in replacement for {@code MathHelper.clamp(value, min, max)} at vanilla's coordinate clamp sites.
	 * Horizontal clamps use the configured limit; anything else (the vertical clamp) behaves exactly as before.
	 */
	public static double clampCoordinate(double value, double min, double max) {
		if (max > HORIZONTAL_THRESHOLD) {
			return clampHorizontal(value);
		}
		return value < min ? min : (value > max ? max : value);
	}

	/** True if a block coordinate is inside the configured limit. */
	public static boolean isWithinLimit(int blockCoordinate) {
		return blockCoordinate >= -LIMIT && blockCoordinate < LIMIT;
	}
}
