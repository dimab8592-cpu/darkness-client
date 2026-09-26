package dev.darkness.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.darkness.client.DarknessClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Inject(method = "tick", at = @At("HEAD"))
	private void darkness$onTick(CallbackInfo ci) {
		DarknessClient.onTick();
	}

	@Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At("HEAD"))
	private void darkness$onDisconnect(net.minecraft.client.gui.screens.Screen screen, boolean isTransferring, CallbackInfo ci) {
		DarknessClient.onDisconnectedFromServer();
	}

	/** Ванильный TitleScreen подменяем своим главным меню — ПОСЛЕ того как
	 *  setScreen отработал (реентерабельный cancel из середины disconnect
	 *  ронял игру в дедлок). */
	@Inject(method = "setScreen", at = @At("TAIL"))
	private void darkness$replaceTitleScreen(net.minecraft.client.gui.screens.Screen screen, CallbackInfo ci) {
		Minecraft self = (Minecraft) (Object) this;
		if (screen != null && screen.getClass() == net.minecraft.client.gui.screens.TitleScreen.class
			&& self.screen == screen) {
			self.setScreen(new dev.darkness.client.ui.MainMenuScreen());
		}
	}

	/** Смена аккаунта без записи final-поля user: подменяем результат getUser(). */
	@ModifyReturnValue(method = "getUser", at = @At("RETURN"))
	private net.minecraft.client.User darkness$overrideUser(net.minecraft.client.User original) {
		var am = dev.darkness.client.DarknessClient.getAccountManager();
		if (am == null) return original;
		var override = am.getSessionOverride();
		return override != null ? override : original;
	}
}
