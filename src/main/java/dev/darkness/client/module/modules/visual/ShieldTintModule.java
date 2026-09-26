package dev.darkness.client.module.modules.visual;

import dev.darkness.client.module.Category;
import dev.darkness.client.module.Module;

/**
 * Подсветка самого щита (в хотбаре, оффхенде и инвентаре):
 * зелёная — щит готов, красная — сбит топором и на перезарядке.
 * Отрисовку делает GuiGraphicsMixin поверх иконки предмета.
 */
public class ShieldTintModule extends Module {
	public ShieldTintModule() {
		super("ShieldIndicator", "Зелёный/красный оттенок прямо на щите в инвентаре и хотбаре", Category.VISUALS);
	}

	/** ARGB-заливка для щита: готов — зелёный, сбит — красный. */
	public static int tintFor(boolean disabled) {
		return disabled ? 0x50FF2020 : 0x3020FF50;
	}

	/** Цвет рамки щита. */
	public static int borderFor(boolean disabled) {
		return disabled ? 0xFFFF3030 : 0xFF30E860;
	}
}
