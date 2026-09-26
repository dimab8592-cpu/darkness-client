package dev.darkness.client.mixin;

import dev.darkness.client.util.CustomizeManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** TitleScreen перекрывает renderBackground, поэтому общий ScreenBackgroundMixin
 *  сюда не попадает — нужен отдельный хук для главного меню. */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {
	private static boolean darkness$logged = false;

	@Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
	private void darkness$customTitleBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		dev.darkness.client.util.FontPackManager.ensureEnabled();
		if (net.minecraft.client.Minecraft.getInstance().player != null) return;
		if (!CustomizeManager.isReady()) {
			CustomizeManager.ensureTextureLoaded(); // со следующего кадра фон будет готов
			return;
		}
		CustomizeManager.renderTitleBackground(guiGraphics, guiGraphics.guiWidth(), guiGraphics.guiHeight());
		ci.cancel();
	}
}
