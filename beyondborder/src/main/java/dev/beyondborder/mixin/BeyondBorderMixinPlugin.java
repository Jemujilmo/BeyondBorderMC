package dev.beyondborder.mixin;

import dev.beyondborder.BeyondBorderConfig;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Lets a single mixin be switched off from {@code config/beyondborder.json} ("disabledMixins"), so a target that
 * does not match your exact Minecraft build can be bypassed without waiting for a new jar.
 */
public final class BeyondBorderMixinPlugin implements IMixinConfigPlugin {
	private static final Logger LOGGER = LoggerFactory.getLogger("BeyondBorder");

	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		String simpleName = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
		if (BeyondBorderConfig.get().isMixinDisabled(simpleName)) {
			LOGGER.warn("{} is disabled in beyondborder.json; skipping it", simpleName);
			return false;
		}
		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
