package dev.darkness.client.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LogoRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Логотип уже вшит в фон меню — ванильный MINECRAFT убираем. */
@Mixin(LogoRenderer.class)
public abstract class LogoRendererMixin {
	@Inject(method = "renderLogo(Lnet/minecraft/client/gui/GuiGraphics;IF)V", at = @At("HEAD"), cancellable = true)
	private void darkness$hideVanillaLogo(GuiGraphics g, int screenWidth, float partialTick, CallbackInfo ci) {
		ci.cancel();
	}
}
