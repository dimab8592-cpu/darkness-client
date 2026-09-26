package dev.darkness.client.util;

import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/** Накопленные позиции щитов за кадр; рисуются одним батчем после всего GUI,
 *  чтобы лежать ПОВЕРХ иконок предметов (иначе буфер предметов их перекрывает). */
public final class ShieldOverlay {
	public static final class Entry {
		public final int x, y;
		public final boolean disabled;

		Entry(int x, int y, boolean disabled) {
			this.x = x;
			this.y = y;
			this.disabled = disabled;
		}
	}

	private static final List<Entry> pending = new ArrayList<>();

	private ShieldOverlay() {
	}

	public static void add(int x, int y, boolean disabled) {
		pending.add(new Entry(x, y, disabled));
	}

	public static void flush(GuiGraphics g) {
		if (pending.isEmpty()) return;
		List<Entry> draw = new ArrayList<>(pending);
		pending.clear();
		for (Entry e : draw) {
			int tint = dev.darkness.client.module.modules.visual.ShieldTintModule.tintFor(e.disabled);
			int border = dev.darkness.client.module.modules.visual.ShieldTintModule.borderFor(e.disabled);
			g.fill(e.x, e.y, e.x + 16, e.y + 16, tint);
			g.fill(e.x, e.y, e.x + 16, e.y + 1, border);
			g.fill(e.x, e.y + 15, e.x + 16, e.y + 16, border);
			g.fill(e.x, e.y, e.x + 1, e.y + 16, border);
			g.fill(e.x + 15, e.y, e.x + 16, e.y + 16, border);
		}
	}
}
