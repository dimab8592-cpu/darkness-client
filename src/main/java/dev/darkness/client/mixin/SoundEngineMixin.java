package dev.darkness.client.mixin;

import dev.darkness.client.util.SoundManager;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Если есть свой totem.mp3 — ванильный звук тотема глушим. */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
	@Inject(method = "play", at = @At("HEAD"), cancellable = true)
	private void darkness$silenceVanillaTotem(SoundInstance sound, CallbackInfoReturnable<Object> cir) {
		if (cir.isCancelled()) return;
		if (SoundManager.hasCustom("totem")
			&& sound != null
			&& "entity.totem.use".equals(sound.getIdentifier().toString())) {
			cir.cancel();
		}
	}
}
