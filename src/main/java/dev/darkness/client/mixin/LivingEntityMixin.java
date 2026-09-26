package dev.darkness.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.darkness.client.DarknessClient;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@ModifyReturnValue(method = "getEffectBlendFactor", at = @At("RETURN"))
	private float darkness$antiNausea(float original, Holder<MobEffect> effect, float partialTick) {
		var m = DarknessClient.getModuleManager();
		if (m == null) return original;
		var mod = m.byName("AntiNausea");
		if (mod == null || !mod.isEnabled()) return original;
		if (effect != null && effect.value() == MobEffects.NAUSEA.value()) {
			return 0f;
		}
		return original;
	}

	/** Событие 35 — срабатывание тотема у нашего игрока: играем кастомный mp3. */
	@Inject(method = "handleEntityEvent", at = @At("HEAD"))
	private void darkness$onTotem(byte event, CallbackInfo ci) {
		if (event != 35) return;
		if ((Object) this != Minecraft.getInstance().player) return;
		dev.darkness.client.util.SoundManager.playTotem();
	}
}
