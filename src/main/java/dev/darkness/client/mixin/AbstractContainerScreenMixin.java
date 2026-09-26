package dev.darkness.client.mixin;

import dev.darkness.client.DarknessClient;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
	@Shadow
	protected AbstractContainerMenu menu;

	@Shadow
	protected Slot hoveredSlot;

	@Inject(method = "mouseScrolled(DDDD)Z", at = @At("HEAD"), cancellable = true)
	private void darkness$itemScroller(double mx, double my, double sx, double sy, CallbackInfoReturnable<Boolean> cir) {
		var m = DarknessClient.getModuleManager();
		if (m == null) return;
		var mod = m.byName("ItemScroller");
		if (mod == null || !mod.isEnabled()) return;
		var mc = net.minecraft.client.Minecraft.getInstance();
		if (mc.player == null || mc.gameMode == null || hoveredSlot == null) return;
		if (!hoveredSlot.hasItem()) return;

		if (sy > 0) {
			// скролл вверх — быстрый перенос стопки (как shift-клик)
			mc.gameMode.handleInventoryMouseClick(menu.containerId, hoveredSlot.index, 0,
				ClickType.QUICK_MOVE, mc.player);
			cir.setReturnValue(true);
		} else if (sy < 0) {
			// скролл вниз — поменять предмет с предметом в выбранном слоте хотбара
			mc.gameMode.handleInventoryMouseClick(menu.containerId, hoveredSlot.index,
				mc.player.getInventory().getSelectedSlot(), ClickType.SWAP, mc.player);
			cir.setReturnValue(true);
		}
	}

	/** ShulkerPreview: сетка содержимого шалкера рядом с курсором. */
	@Inject(method = "renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;II)V", at = @At("TAIL"))
	private void darkness$shulkerPreview(net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, CallbackInfo ci) {
		var m = DarknessClient.getModuleManager();
		if (m == null) return;
		var mod = m.byName("ShulkerPreview");
		if (mod == null || !mod.isEnabled() || hoveredSlot == null) return;
		var stack = hoveredSlot.getItem();
		if (!dev.darkness.client.module.modules.util.ShulkerPreviewModule.isShulker(stack)) return;
		var items = dev.darkness.client.module.modules.util.ShulkerPreviewModule.contents(stack);
		if (items == null) return;

		AbstractContainerScreen self = (AbstractContainerScreen) (Object) this;
		int scrW = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth();
		int scrH = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight();
		int pw = 9 * 18 + 8, ph = 3 * 18 + 8;
		int px = mouseX + 14, py = mouseY - 12;
		if (px + pw > scrW - 4) px = mouseX - pw - 14;
		if (py + ph > scrH - 4) py = scrH - ph - 4;
		if (py < 4) py = 4;

		g.fill(px, py, px + pw, py + ph, 0xF0101014);
		g.renderOutline(px, py, pw, ph, 0xFF8B5CF6);
		for (int i = 0; i < 27; i++) {
			int sx = px + 4 + (i % 9) * 18;
			int sy = py + 4 + (i / 9) * 18;
			g.fill(sx, sy, sx + 16, sy + 16, 0x30FFFFFF);
			if (items[i] != null && !items[i].isEmpty()) {
				g.renderItem(items[i], sx, sy);
			}
		}
	}
}
