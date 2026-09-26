package dev.darkness.client.module.modules.visual;

import dev.darkness.client.DarknessClient;
import dev.darkness.client.module.Category;
import dev.darkness.client.module.ColorSetting;
import dev.darkness.client.module.Module;
import dev.darkness.client.module.NumberSetting;
import dev.darkness.client.module.OverlayRenderer;
import dev.darkness.client.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;

/** ESP только по последней атакованной цели — подсвечивает её несколько секунд. */
public class TargetEspModule extends Module implements OverlayRenderer {
	private final ColorSetting color;
	private final NumberSetting duration;

	public TargetEspModule() {
		super("TargetESP", "Подсветка только последней атакованной цели", Category.VISUALS);
		color = new ColorSetting("Цвет", "Цвет подсветки", 0xFFFF5252);
		duration = new NumberSetting("Время, с", "Сколько секунд держать подсветку", 4, 1, 10, 0.5);
		register(color, duration);
	}

	@Override
	public void renderOverlay(GuiGraphics g, int screenW, int screenH, float partialTick) {
		if (mc.level == null || mc.player == null) return;
		var tm = DarknessClient.getTargetManager();
		if (tm == null || !tm.hasTarget()) return;
		long shown = (long) (duration.get() * 1000);
		if (System.currentTimeMillis() - tm.getLastAttackMs() > shown) return;
		Entity e = tm.getTarget();
		if (e == null || !e.isAlive()) return;

		double ex = RenderUtil.lerpX(e, partialTick), ey = RenderUtil.lerpY(e, partialTick), ez = RenderUtil.lerpZ(e, partialTick);
		double hw = e.getBbWidth() / 2, h = e.getBbHeight();

		float[][] c = new float[8][];
		int i = 0;
		boolean ok = true;
		for (double yy : new double[]{ey, ey + h}) {
			for (double xx : new double[]{ex - hw, ex + hw}) {
				for (double zz : new double[]{ez - hw, ez + hw}) {
					float[] s = RenderUtil.worldToScreen(xx, yy, zz);
					if (s == null) { ok = false; break; }
					c[i++] = s;
				}
			}
		}
		if (!ok || i < 8) return;

		float left = Float.MAX_VALUE, right = -Float.MAX_VALUE, top = Float.MAX_VALUE, bottom = -Float.MAX_VALUE;
		for (float[] p : c) {
			left = Math.min(left, p[0]);
			right = Math.max(right, p[0]);
			top = Math.min(top, p[1]);
			bottom = Math.max(bottom, p[1]);
		}
		int col = color.rgb();
		// пульсация для читаемости
		int a = 160 + (int) (60 * Math.sin(System.currentTimeMillis() / 200.0));
		RenderUtil.outline(g, (int) left, (int) top, (int) (right - left), (int) (bottom - top), RenderUtil.withAlpha(col, a));
		RenderUtil.outline(g, (int) left + 1, (int) top + 1, (int) (right - left) - 2, (int) (bottom - top) - 2, RenderUtil.withAlpha(col, a / 2));
		if (e instanceof net.minecraft.world.entity.LivingEntity living) {
			String label = e.getName().getString() + " " + (int) Math.ceil(living.getHealth()) + " HP";
			RenderUtil.textCentered(g, label, (int) ((left + right) / 2), (int) top - 11, col);
		}
	}
}
