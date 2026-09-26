package dev.darkness.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Тёмный стиль обычных кнопок с текстом: своя подложка, рамка и подпись. */
@Mixin(AbstractButton.class)
public abstract class AbstractButtonMixin {
	@Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
	private void darkness$darkButton(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (!((Object) this instanceof Button btn)) return;      // чекбоксы/слайдеры — ваниль
		if (btn.getMessage() == null || btn.getMessage().getString().isEmpty()) return; // иконки — ваниль
		int x = btn.getX(), y = btn.getY(), w = btn.getWidth(), h = btn.getHeight();
		int fill, border, textColor;
		if (!btn.active) {
			fill = 0xFF1C1C24;
			border = 0xFF3A3A46;
			textColor = 0xFFA0A0A0;
		} else if (btn.isHovered()) {
			fill = 0xFF44326A;
			border = 0xFF8A6EC8;
			textColor = 0xFFFFFFFF;
		} else {
			fill = 0xE62A2A38;
			border = 0xFF565666;
			textColor = 0xFFFFFFFF;
		}
		g.fill(x, y, x + w, y + h, fill);
		g.renderOutline(x, y, w, h, border);
		g.drawCenteredString(Minecraft.getInstance().font, btn.getMessage(),
			x + w / 2, y + (h - 8) / 2, textColor);
		ci.cancel();
	}
}
