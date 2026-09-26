package dev.darkness.client.mixin;

import dev.darkness.client.DarknessClient;
import dev.darkness.client.util.TargetManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
	@Inject(method = "attack", at = @At("HEAD"))
	private void darkness$onAttack(Player player, Entity target, CallbackInfo ci) {
		TargetManager tm = DarknessClient.getTargetManager();
		if (tm != null) {
			tm.onAttack(target);
		}
		// крит: падение + не на земле + не на лестнице + не в воде + не слепота + не пассажир
		if (player == Minecraft.getInstance().player
			&& player.fallDistance > 0.0F && !player.onGround()
			&& !player.onClimbable() && !player.isInWater()
			&& !player.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)
			&& !player.isPassenger()) {
			dev.darkness.client.util.SoundManager.playCrit();
		}
	}
}
