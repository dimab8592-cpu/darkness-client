package dev.darkness.client.module.modules.util;

import dev.darkness.client.module.Category;
import dev.darkness.client.module.Module;

public class NoPauseOnLostFocusModule extends Module {
	private boolean previous = true;
	private boolean applied = false;

	public NoPauseOnLostFocusModule() {
		super("NoPauseOnTab", "Игра не ставится на паузу при переключении окна", Category.UTILITY);
	}

	@Override
	protected void onEnable() {
		apply();
	}

	@Override
	public void onTick() {
		// при загрузке конфига options ещё не создан — применяем чуть позже
		if (!applied) apply();
	}

	private void apply() {
		if (mc.options == null) return;
		previous = mc.options.pauseOnLostFocus;
		mc.options.pauseOnLostFocus = false;
		applied = true;
	}

	@Override
	protected void onDisable() {
		if (!applied) return;
		applied = false;
		if (mc.options != null) mc.options.pauseOnLostFocus = previous;
	}
}
