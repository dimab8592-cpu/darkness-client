package dev.darkness.client.module.modules.util;

import dev.darkness.client.DarknessClient;
import dev.darkness.client.module.Category;
import dev.darkness.client.module.Module;
import dev.darkness.client.module.NumberSetting;
import dev.darkness.client.util.KeyManager;
import org.lwjgl.glfw.GLFW;

/** Двойное нажатие Shift — включает/выключает постоянный спринт. */
public class ShiftTapModule extends Module {
	private final NumberSetting interval;
	private boolean sprintLock = false;
	private boolean shiftWasDown = false;
	private long lastShiftMs = 0;

	public ShiftTapModule() {
		super("ShiftTap", "Двойной тап Shift переключает постоянный спринт", Category.UTILITY);
		interval = new NumberSetting("Интервал, мс", "Окно между нажатиями Shift", 350, 150, 600, 50);
		register(interval);
	}

	@Override
	public void onTick() {
		if (mc.player == null) return;
		boolean down = KeyManager.isDown(GLFW.GLFW_KEY_LEFT_SHIFT);
		long now = System.currentTimeMillis();
		if (down && !shiftWasDown) {
			if (now - lastShiftMs < Math.round(interval.get())) {
				sprintLock = !sprintLock;
				DarknessClient.getNotifications().show(
					sprintLock ? "§aСпринт закреплён" : "§cСпринт откреплён");
				lastShiftMs = 0;
			} else {
				lastShiftMs = now;
			}
		}
		shiftWasDown = down;

		if (sprintLock && !mc.player.isSprinting()
			&& !mc.player.isShiftKeyDown()
			&& mc.player.input != null && mc.player.input.keyPresses.forward()
			&& !mc.player.isUsingItem()) {
			mc.player.setSprinting(true);
		}
	}

	@Override
	protected void onDisable() {
		sprintLock = false;
	}
}
