package dev.darkness.client.module.modules.visual;

import dev.darkness.client.module.Category;
import dev.darkness.client.module.BooleanSetting;
import dev.darkness.client.module.Module;
import dev.darkness.client.module.NumberSetting;
import dev.darkness.client.util.LiquidsPackManager;

/** Прозрачная вода и лава через автогенерируемый ресурс-пак. */
public class TransparentLiquidsModule extends Module {
	private final BooleanSetting water;
	private final BooleanSetting lava;
	private final NumberSetting transparency;
	private String lastCfg = "";

	public TransparentLiquidsModule() {
		super("TransparentLiquids", "Прозрачная вода и лава (видно дно и мобов)", Category.VISUALS);
		water = new BooleanSetting("Вода", "Прозрачная вода", true);
		lava = new BooleanSetting("Лава", "Полупрозрачная лава (экспериментально)", false);
		transparency = new NumberSetting("Прозрачность", "0 — невидимая, 100 — обычная", 30, 5, 90, 5);
		register(water, lava, transparency);
	}

	@Override
	public void onTick() {
		if (mc.level == null) return;
		String cfg = water.get() + "|" + lava.get() + "|" + (int) Math.round(transparency.get());
		if (cfg.equals(lastCfg)) return;
		lastCfg = cfg;
		apply();
	}

	@Override
	protected void onEnable() {
		lastCfg = "";
	}

	@Override
	protected void onDisable() {
		lastCfg = "";
		LiquidsPackManager.disable();
	}

	private void apply() {
		if (!water.get() && !lava.get()) {
			LiquidsPackManager.disable();
			return;
		}
		// переводим проценты прозрачности в альфу текстуры
		int alpha = (int) Math.round(255 * transparency.get() / 100.0);
		LiquidsPackManager.apply(water.get(), lava.get(), alpha);
	}
}
