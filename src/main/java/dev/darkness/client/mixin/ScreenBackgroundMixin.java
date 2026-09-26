package dev.darkness.client.mixin;

import dev.darkness.client.util.CustomizeManager;
import dev.darkness.client.util.FontPackManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Фон donk во всех экранах меню и загрузки мира (внутри игры — ваниль). */
@Mixin(Screen.class)
public abstract class ScreenBackgroundMixin {
	@Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
	private void darkness$menuBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		FontPackManager.ensureEnabled();
		if (Minecraft.getInstance().player != null) return;   // в игре оставляем ваниль
		if (!CustomizeManager.isReady()) {
			CustomizeManager.ensureTextureLoaded();
			return;
		}
		CustomizeManager.renderTitleBackground(g, g.guiWidth(), g.guiHeight());
		ci.cancel();
	}

	/** Земляной фон экранов загрузки («Загрузка мира», сохранение). */
	@Inject(method = "renderDirtBackground", at = @At("HEAD"), cancellable = true)
	private void darkness$dirtBackground(GuiGraphics g, CallbackInfo ci) {
		if (Minecraft.getInstance().player != null) return;
		if (!CustomizeManager.isReady()) return;
		CustomizeManager.renderTitleBackground(g, g.guiWidth(), g.guiHeight());
		ci.cancel();
	}
}
