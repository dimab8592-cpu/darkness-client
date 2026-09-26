package dev.darkness.client.mixin;

import dev.darkness.client.DarknessClient;
import dev.darkness.client.module.Module;
import dev.darkness.client.module.modules.visual.ShieldTintModule;
import dev.darkness.client.util.ShieldOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Запоминаем позиции отрисованных щитов; сам оверлей (зелёный/красный)
 * рисуется позже — из GuiMixin в конце GUI, поверх иконок предметов.
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
	@Inject(method = "renderItem(Lnet/minecraft/world/item/ItemStack;II)V", at = @At("TAIL"))
	private void darkness$shieldTint(ItemStack stack, int x, int y, CallbackInfo ci) {
		var mm = DarknessClient.getModuleManager();
		if (mm == null) return;
		Module m = mm.byName("ShieldIndicator");
		if (!(m instanceof ShieldTintModule) || !m.isEnabled()) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || !stack.is(Items.SHIELD)) return;
		boolean disabled = mc.player.getCooldowns().getCooldownPercent(stack, 0f) > 0f;
		ShieldOverlay.add(x, y, disabled);
	}
}
