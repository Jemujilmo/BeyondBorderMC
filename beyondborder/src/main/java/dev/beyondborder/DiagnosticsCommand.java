package dev.beyondborder;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

/**
 * {@code /beyondborder} prints where the executor sits in the numeric regimes this experiment is about:
 * distance past vanilla's limit, whether BlockPos packing has wrapped, and how coarse float and double
 * arithmetic have become at that X. It also reports the world border's bound, which proves the border mixin applied.
 */
public final class DiagnosticsCommand {
	private static final double VANILLA_LIMIT = 3.0E7;

	private DiagnosticsCommand() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager.literal("beyondborder")
				.requires(source -> source.hasPermissionLevel(2))
				.executes(context -> report(context.getSource()))
		);
	}

	private static int report(ServerCommandSource source) {
		Vec3d pos = source.getPosition();
		double x = pos.x;
		double z = pos.z;

		int blockX = MathHelper.floor(x);
		int blockY = MathHelper.floor(pos.y);
		int blockZ = MathHelper.floor(z);
		long packed = BlockPos.asLong(blockX, blockY, blockZ);
		int packedX = BlockPos.unpackLongX(packed);
		int packedZ = BlockPos.unpackLongZ(packed);

		double borderEast = source.getWorld().getWorldBorder().getBoundEast();
		double farthest = Math.max(Math.abs(x), Math.abs(z));

		StringBuilder out = new StringBuilder();
		out.append("Beyond the Border  |  limit = +/-").append(sci(Limits.limit())).append('\n');
		out.append(String.format(Locale.ROOT, "Position: x=%.3f  z=%.3f  (block %d, %d)%n", x, z, blockX, blockZ));
		out.append("World border east bound: ").append(sci(borderEast))
			.append(borderEast == Limits.limit() ? "  [patched]" : "  [NOT patched: vanilla is 2.9999984E7]").append('\n');
		out.append("Past vanilla's +/-3.0E7: ")
			.append(farthest > VANILLA_LIMIT ? "yes, by " + sci(farthest - VANILLA_LIMIT) + " blocks" : "no").append('\n');
		out.append("BlockPos.asLong round trip: x ").append(blockX).append(" -> ").append(packedX)
			.append(", z ").append(blockZ).append(" -> ").append(packedZ)
			.append(blockX == packedX && blockZ == packedZ ? "  [intact]" : "  [WRAPPED: block keys now alias another place]").append('\n');
		out.append("Double precision here: one step = ").append(sci(Math.ulp(farthest))).append(" blocks\n");
		out.append("Float precision here: one step = ").append(sci(Math.ulp((float) farthest))).append(" blocks");

		source.sendFeedback(() -> Text.literal(out.toString()), false);
		return 1;
	}

	private static String sci(double value) {
		return String.format(Locale.ROOT, "%.4E", value);
	}
}
