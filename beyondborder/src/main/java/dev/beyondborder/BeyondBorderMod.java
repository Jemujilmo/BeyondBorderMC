package dev.beyondborder;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BeyondBorderMod implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("BeyondBorder");

	@Override
	public void onInitialize() {
		LOGGER.info("Beyond the Border active: horizontal limit is +/-{} blocks (vanilla is about 3.0E7)", Limits.limit());
		CommandRegistrationCallback.EVENT.register(
			(dispatcher, registryAccess, environment) -> DiagnosticsCommand.register(dispatcher)
		);
	}
}
