package dev.darkness.client.module.modules.visual;

import dev.darkness.client.module.Category;
import dev.darkness.client.module.Module;
import net.minecraft.client.OptionInstance;

public class TransparentChatModule extends Module {
	private Double previous = null;

	public TransparentChatModule() {
		super("TransparentChat", "Полупрозрачный фон чата (текст остаётся полностью видимым)", Category.VISUALS);
	}

	@Override
	protected void onEnable() {
		// chatOpacity гасит и текст (его альфа = opacity * 0.9 + 0.1), поэтому
		// обнуляем именно прозрачность фона текста — текст остаётся ярким.
		OptionInstance<Double> op = mc.options.textBackgroundOpacity();
		previous = op.get();
		op.set(0.0);
	}

	@Override
	protected void onDisable() {
		if (previous == null) return;
		mc.options.textBackgroundOpacity().set(previous);
		previous = null;
	}
}
