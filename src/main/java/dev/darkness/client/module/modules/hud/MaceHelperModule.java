package dev.darkness.client.module.modules.hud;

import dev.darkness.client.module.BooleanSetting;
import dev.darkness.client.module.ColorSetting;
import dev.darkness.client.module.HudModule;
import dev.darkness.client.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Items;

/**
 * Помощник булавы: состояние кулдауна и расчётный урон смэша
 * от текущей высоты падения.
 */
public class MaceHelperModule extends HudModule {
	private final BooleanSetting showDamage;
	private final BooleanSetting bar;
	private final ColorSetting readyColor;
	private final ColorSetting cdColor;

	public MaceHelperModule() {
		super("MaceHelper", "Кулдаун булавы и урон смэша при падении");
		showDamage = new BooleanSetting("Урон смэша", "Показывать расчётный урон от падения", true);
		bar = new BooleanSetting("Полоса", "Полоса восстановления атаки", true);
		readyColor = new ColorSetting("Цвет готовности", "Когда булава готова", 0xFF30E860);
		cdColor = new ColorSetting("Цвет кулдауна", "Когда атака ещё слабая", 0xFFFFC107);
		register(showDamage, bar, readyColor, cdColor);
	}

	@Override
	public void render(GuiGraphics g, int screenW, int screenH, float partialTick) {
		if (mc.player == null) return;
		boolean holding = mc.player.getMainHandItem().is(Items.MACE)
			|| mc.player.getOffhandItem().is(Items.MACE);
		if (!holding) {
			setSize(1, 1);
			return;
		}

		int x = getX(screenW), y = getY(screenH);
		float strength = mc.player.getCurrentItemAttackStrengthDelay() <= 0
			? 1f : mc.player.getAttackStrengthScale(partialTick);
		boolean ready = strength >= 0.999f;
		double fall = mc.player.fallDistance;

		String line1 = ready ? "MACE ГОТОВА" : "MACE " + Math.round(strength * 100) + "%";
		String line2 = null;
		if (showDamage.get() && fall >= 1.5f) {
			line2 = "СМЭШ: " + String.format("%.1f", smashDamage(fall)) + " урона";
		}

		int accent = ready ? readyColor.rgb() : cdColor.rgb();
		RenderUtil.text(g, line1, x, y, accent);
		int w = mc.font.width(line1);
		int yy = y + 11;
		if (line2 != null) {
			RenderUtil.text(g, line2, x, yy, 0xFFFFFFFF);
			w = Math.max(w, mc.font.width(line2));
			yy += 11;
		}
		if (bar.get()) {
			RenderUtil.rect(g, x, yy, w, 3, 0x66000000);
			RenderUtil.rect(g, x, yy, (int) (w * Math.max(0, Math.min(1, strength))), 3, accent);
			yy += 5;
		}
		setSize(w + 2, yy - y);
	}

	/** Приближение формулы смэша булавы (1.21): 6 база, +4/блок до 8, дальше +2/блок. */
	static double smashDamage(double fall) {
		if (fall <= 1.5) return 6;
		double bonus = 4.0 * (Math.min(fall, 8.0) - 1.5);
		if (fall > 8.0) bonus += 2.0 * (fall - 8.0);
		return 6.0 + bonus;
	}
}
