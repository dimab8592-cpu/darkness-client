package dev.darkness.client.ui;

import dev.darkness.client.DarknessClient;
import dev.darkness.client.module.Category;
import dev.darkness.client.module.Module;
import dev.darkness.client.module.NumberSetting;
import dev.darkness.client.util.KeyManager;
import dev.darkness.client.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Меню функций в стиле Pulse Visual: центральное модальное окно, хлебные крошки
 * категорий, поиск справа, сетка карточек 2 колонки с пилюлями-тумблерами,
 * фиолетовый акцент.
 */
public class ClickGuiScreen extends Screen {
	private static final int ACCENT = 0xFF8B5CF6;
	private static final int ACCENT_DIM = 0x668B5CF6;
	private static final int PANEL_BG = 0xF2141418;
	private static final int HEADER_BG = 0xF21B1B22;
	private static final int CARD_BG = 0xF0222228;
	private static final int CARD_HOVER = 0xF02A2A33;
	private static final int CARD_ON = 0xE0252133;
	private static final int TEXT_MAIN = 0xFFF2F2F2;
	private static final int TEXT_DIM = 0xFF9A9AA5;
	private static final int[] PALETTE = {
		0xFF7C4DFF, 0xFF3D5AFE, 0xFF00B0FF, 0xFF00E676, 0xFFFFEB3B,
		0xFFFF9100, 0xFFFF5252, 0xFFFF4081, 0xFFE040FB, 0xFFB388FF,
		0xFFFFFFFF, 0xFF212121
	};

	private Category current = Category.COMBAT;
	private Module expanded;
	private Module listeningBind;
	private double scroll;
	private EditBox searchBox;

	public boolean isListeningBind() {
		return listeningBind != null;
	}

	public boolean isSearchFocused() {
		return searchBox != null && searchBox.isFocused();
	}

	private record Area(int x, int y, int w, int h, Runnable click, BiConsumer<Double, Double> drag) {
	}

	private final List<Area> areas = new ArrayList<>();
	private Area dragArea = null;

	private static final int CARD_H = 22;          // высота карточки модуля
	private static final int SET_ROW = 13;         // высота строки настройки
	private static final int HEADER1 = 20;         // строка логотипа + поиск
	private static final int HEADER2 = 17;         // строка хлебных крошек
	private static final int FOOTER = 18;
	private static final int GAP = 4;

	// геометрия окна (считается в render)
	private int mx0, my0, mw, mh;
	private int contentY0, contentY1;

	public ClickGuiScreen() {
		super(Component.literal("Darkness Client"));
	}

	@Override
	protected void init() {
		super.init();
		calcGeometry();
		searchBox = new EditBox(this.font, mx0 + mw - 96, my0 + 4, 90, 11, Component.literal("Поиск"));
		searchBox.setMaxLength(24);
		searchBox.setHint(Component.literal("Поиск..."));
		searchBox.setCanLoseFocus(true);
	}

	private void calcGeometry() {
		mw = Math.min(this.width - 16, 440);
		mh = Math.min(this.height - 16, 250);
		mx0 = (this.width - mw) / 2;
		my0 = (this.height - mh) / 2;
		contentY0 = my0 + HEADER1 + HEADER2;
		contentY1 = my0 + mh - FOOTER;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		areas.clear();
		calcGeometry();
		searchBox.setX(mx0 + mw - 96);
		searchBox.setY(my0 + 4);
		int w = this.width, h = this.height;

		// затемнение фона под модальным окном
		RenderUtil.rect(g, 0, 0, w, h, 0x88040408);

		// --- модальное окно ---
		RenderUtil.rect(g, mx0, my0, mw, mh, PANEL_BG);
		RenderUtil.outline(g, mx0, my0, mw, mh, 0x30FFFFFF);
		// «скруглённые» углы: тёмные пиксели по краям поверх рамки
		for (int[] c : new int[][]{{mx0, my0}, {mx0 + mw - 1, my0}, {mx0, my0 + mh - 1}, {mx0 + mw - 1, my0 + mh - 1}}) {
			RenderUtil.rect(g, c[0], c[1], 1, 1, PANEL_BG);
		}

		// --- строка 1: логотип + версия + поиск ---
		RenderUtil.rect(g, mx0 + 1, my0 + 1, mw - 2, HEADER1, HEADER_BG);
		RenderUtil.text(g, "DARKNESS", mx0 + 8, my0 + 6, ACCENT);
		RenderUtil.text(g, "client", mx0 + 8 + this.font.width("DARKNESS") + 2, my0 + 6, TEXT_DIM);
		RenderUtil.textRight(g, "v" + DarknessClient.VERSION, mx0 + mw - 102, my0 + 6, 0xFF6A6A75);
		searchBox.render(g, mouseX, mouseY, partialTick);

		// --- строка 2: хлебные крошки категорий ---
		int cy = my0 + HEADER1;
		RenderUtil.rect(g, mx0 + 1, cy, mw - 2, HEADER2 - 1, 0x40000000);
		int tx = mx0 + 8;
		boolean searching = !searchBox.getValue().isBlank();
		for (Category c : Category.values()) {
			String label = c.getDisplayName();
			int lw = this.font.width(label);
			boolean active = c == current && !searching;
			RenderUtil.text(g, label, tx, cy + 5, active ? 0xFFFFFFFF : TEXT_DIM);
			if (active) {
				RenderUtil.rect(g, tx, cy + HEADER2 - 3, lw, 2, ACCENT);
			}
			int cx0 = tx;
			registerArea(cx0, cy, lw, HEADER2 - 1, () -> {
				current = c;
				expanded = null;
				scroll = 0;
				searchBox.setValue("");
			}, null);
			tx += lw + 4;
			RenderUtil.text(g, "/", tx, cy + 5, 0xFF4A4A55);
			tx += this.font.width("/") + 4;
		}
		if (searching) {
			String q = "поиск: " + searchBox.getValue();
			RenderUtil.textRight(g, q, mx0 + mw - 8, cy + 5, ACCENT);
		}

		// --- контент: карточки в 2 колонки ---
		renderCards(g, mouseX, mouseY);

		// --- подвал: кнопки ---
		int fy = my0 + mh - FOOTER + 1;
		RenderUtil.rect(g, mx0 + 1, fy - 1, mw - 2, 1, 0x22FFFFFF);
		int bx = mx0 + 8;
		bx += footerButton(g, bx, fy + 2, 74, 11, "Аккаунты", () -> minecraft.setScreen(new AccountsScreen(this))) + GAP;
		bx += footerButton(g, bx, fy + 2, 88, 11, "HUD-редактор", () -> minecraft.setScreen(new HudEditorScreen())) + GAP;
		bx += footerButton(g, bx, fy + 2, 80, 11, "Customize", () -> {
			dev.darkness.client.util.CustomizeManager.openFolder();
			DarknessClient.getNotifications().show("Положите title.png в папку customize");
		}) + GAP;
		footerButton(g, bx, fy + 2, 92, 11, "Сохр. конфиг", () -> {
			DarknessClient.getConfigManager().save();
			DarknessClient.getNotifications().show("§aКонфиг сохранён");
		});

		if (listeningBind != null) {
			String hint = "Нажмите клавишу для " + listeningBind.getName() + " (Esc — убрать)";
			int hw = this.font.width(hint) + 16;
			RenderUtil.rect(g, w / 2 - hw / 2, my0 - 16, hw, 13, 0xE6101014);
			RenderUtil.outline(g, w / 2 - hw / 2, my0 - 16, hw, 13, ACCENT_DIM);
			RenderUtil.textCentered(g, hint, w / 2, my0 - 12, ACCENT);
		}
	}

	private void renderCards(GuiGraphics g, int mouseX, int mouseY) {
		List<Module> modules = visibleModules();
		int innerW = mw - 12;                    // поля 6px
		int colW = (innerW - GAP) / 2;
		int viewport = contentY1 - contentY0;

		int totalH = 0;
		for (Module m : modules) totalH += cardHeight(m) + GAP;
		boolean scrollable = totalH - GAP > viewport;
		if (scrollable) {
			scroll = Math.max(0, Math.min(scroll, totalH - viewport));
		} else {
			scroll = 0;
		}

		// сбалансированное разбиение по высоте на две колонки
		int target = totalH / 2;
		int split = 0, acc = 0;
		for (int i = 0; i < modules.size(); i++) {
			int hh = cardHeight(modules.get(i)) + GAP;
			if (acc + hh / 2 > target && i > 0) break;
			acc += hh;
			split = i + 1;
		}

		g.enableScissor(mx0 + 1, contentY0, mx0 + mw - 1, contentY1);
		for (int col = 0; col < 2; col++) {
			int from = col == 0 ? 0 : split;
			int to = col == 0 ? split : modules.size();
			int x = mx0 + 6 + col * (colW + GAP);
			double y = contentY0 - scroll;
			for (int i = from; i < to; i++) {
				Module m = modules.get(i);
				int ch = cardHeight(m);
				int yy = (int) Math.floor(y);
				if (yy + ch > contentY0 && yy < contentY1) {
					drawCard(g, m, x, yy, colW, ch, mouseX, mouseY);
				}
				y += ch + GAP;
			}
		}
		g.disableScissor();

		// скроллбар
		if (scrollable) {
			int trackX = mx0 + mw - 4;
			RenderUtil.rect(g, trackX, contentY0, 2, viewport, 0x22FFFFFF);
			int thumbH = Math.max(14, viewport * viewport / (totalH));
			int thumbY = contentY0 + (int) ((viewport - thumbH) * (scroll / (totalH - viewport)));
			RenderUtil.rect(g, trackX, thumbY, 2, thumbH, ACCENT);
		}
	}

	private void drawCard(GuiGraphics g, Module m, int x, int y, int w, int h, int mouseX, int mouseY) {
		boolean on = m.isEnabled();
		boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
		boolean expandedNow = m == expanded;

		RenderUtil.rect(g, x, y, w, h, on ? CARD_ON : (hovered ? CARD_HOVER : CARD_BG));
		RenderUtil.outline(g, x, y, w, h, expandedNow ? ACCENT_DIM : 0x14FFFFFF);
		if (on) {
			RenderUtil.rect(g, x, y, 2, h, ACCENT); // фиолетовая полоса активного модуля
		}

		RenderUtil.text(g, m.getName(), x + 8, y + (CARD_H - 8) / 2, on ? TEXT_MAIN : TEXT_DIM);
		String info = m.getDisplayInfo();
		if (info != null) {
			RenderUtil.textRight(g, info, x + w - 32, y + (CARD_H - 8) / 2, 0xFF8A8A95);
		}
		if (expandedNow) {
			RenderUtil.text(g, "▾", x + w - 28, y + (CARD_H - 8) / 2, ACCENT);
		}

		// пилюля-тумблер
		int pillX = x + w - 22, pillY = y + (CARD_H - 8) / 2;
		RenderUtil.rect(g, pillX, pillY, 18, 8, on ? ACCENT : 0xFF3A3A44);
		RenderUtil.rect(g, on ? pillX + 12 : pillX + 1, pillY + 1, 5, 6, 0xFFEDEDF5);
		registerArea(pillX - 2, pillY - 2, 22, 12, m::toggle, null);

		// клик по карточке — развернуть настройки
		Module mm = m;
		registerArea(x, y, w - 26, h, () -> {
			expanded = expanded == mm ? null : mm;
			listeningBind = null;
		}, null);

		if (expandedNow) {
			int sy = y + CARD_H + 1;
			for (SettingRow row : settingRows(m, x + 3, sy, w - 6)) {
				if (row.y() > contentY1) break;
				if (row.y() + row.h() >= contentY0) {
					row.renderer().render(g, mouseX);
					registerArea(row.x(), row.y(), row.w(), row.h(), row.click(), row.drag());
				}
			}
		}
	}

	private int footerButton(GuiGraphics g, int x, int y, int w, int h, String label, Runnable action) {
		RenderUtil.rect(g, x, y, w, h, 0x661E1E26);
		RenderUtil.outline(g, x, y, w, h, 0x22FFFFFF);
		RenderUtil.textCentered(g, label, x + w / 2, y + 2, 0xFFC8C8D0);
		registerArea(x, y, w, h, action, null);
		return w;
	}

	private List<Module> visibleModules() {
		String q = searchBox.getValue().trim().toLowerCase();
		if (!q.isBlank()) {
			List<Module> out = new ArrayList<>();
			for (Module m : DarknessClient.getModuleManager().getModules()) {
				if (m.getName().toLowerCase().contains(q)) out.add(m);
			}
			return out;
		}
		return DarknessClient.getModuleManager().byCategory(current);
	}

	private int cardHeight(Module m) {
		if (m != expanded) return CARD_H;
		return CARD_H + 1 + m.getSettings().size() * (SET_ROW + 1) + SET_ROW + 2;
	}

	private record SettingRow(int x, int y, int w, int h, Runnable click, BiConsumer<Double, Double> drag,
							  RowRenderer renderer) {
	}

	private interface RowRenderer {
		void render(GuiGraphics g, int mouseX);
	}

	private List<SettingRow> settingRows(Module m, int x, int startY, int w) {
		List<SettingRow> rows = new ArrayList<>();
		int y = startY;
		for (var s : m.getSettings()) {
			int ry = y;
			if (s instanceof dev.darkness.client.module.BooleanSetting bs) {
				rows.add(new SettingRow(x, ry, w, SET_ROW,
					() -> bs.set(!bs.get()), null,
					(g, mm) -> {
						RenderUtil.rect(g, x, ry, w, SET_ROW, 0x66101015);
						RenderUtil.text(g, s.getName(), x + 4, ry + 3, 0xFFC8C8D0);
						// мини-пилюля
						int px = x + w - 20, py = ry + 3;
						RenderUtil.rect(g, px, py, 14, 6, bs.get() ? ACCENT : 0xFF3A3A44);
						RenderUtil.rect(g, bs.get() ? px + 9 : px + 1, py + 1, 4, 4, 0xFFEDEDF5);
					}));
			} else if (s instanceof NumberSetting ns) {
				final int fx = x + 4;
				final int fw = w - 8;
				final int fy = ry;
				BiConsumer<Double, Double> apply = (dmx, dmy) -> {
					double t = Math.max(0, Math.min(1, (dmx - fx) / fw));
					ns.set(ns.getMin() + t * (ns.getMax() - ns.getMin()));
				};
				rows.add(new SettingRow(x, ry, w, SET_ROW, null, apply, (g, mm) -> {
					RenderUtil.rect(g, x, fy, w, SET_ROW, 0x66101015);
					RenderUtil.text(g, s.getName(), x + 4, fy + 2, 0xFFC8C8D0);
					RenderUtil.textRight(g, ns.getDisplayValue(), x + w - 4, fy + 2, ACCENT);
					int barY = fy + SET_ROW - 2;
					double t = (ns.get() - ns.getMin()) / (ns.getMax() - ns.getMin());
					RenderUtil.rect(g, fx, barY, fw, 2, 0x4DFFFFFF);
					RenderUtil.rect(g, fx, barY, (int) (fw * Math.max(0, Math.min(1, t))), 2, ACCENT);
				}));
			} else if (s instanceof dev.darkness.client.module.ModeSetting ms) {
				rows.add(new SettingRow(x, ry, w, SET_ROW, ms::cycle, null, (g, mm) -> {
					RenderUtil.rect(g, x, ry, w, SET_ROW, 0x66101015);
					RenderUtil.text(g, s.getName(), x + 4, ry + 3, 0xFFC8C8D0);
					RenderUtil.textRight(g, ms.get(), x + w - 4, ry + 3, ACCENT);
				}));
			} else if (s instanceof dev.darkness.client.module.ColorSetting cs) {
				rows.add(new SettingRow(x, ry, w, SET_ROW, () -> {
					int idx = 0;
					for (int i = 0; i < PALETTE.length; i++) {
						if ((PALETTE[i] & 0xFFFFFF) == (cs.get() & 0xFFFFFF)) {
							idx = i;
							break;
						}
					}
					cs.set(PALETTE[(idx + 1) % PALETTE.length]);
				}, null, (g, mm) -> {
					RenderUtil.rect(g, x, ry, w, SET_ROW, 0x66101015);
					RenderUtil.text(g, s.getName(), x + 4, ry + 3, 0xFFC8C8D0);
					RenderUtil.rect(g, x + w - 20, ry + 3, 14, 7, cs.rgb());
					RenderUtil.outline(g, x + w - 20, ry + 3, 14, 7, 0x66FFFFFF);
				}));
			} else if (s instanceof dev.darkness.client.module.StringSetting ss) {
				rows.add(new SettingRow(x, ry, w, SET_ROW, () -> {
				}, null, (g, mm) -> {
					RenderUtil.rect(g, x, ry, w, SET_ROW, 0x66101015);
					RenderUtil.text(g, s.getName(), x + 4, ry + 3, 0xFFC8C8D0);
					RenderUtil.textRight(g, ss.get(), x + w - 4, ry + 3, ACCENT);
				}));
			}
			y += SET_ROW + 1;
		}
		// строка бинда
		int by = y;
		String keyLabel = m.getKeybind() > 0 ? KeyManager.keyName(m.getKeybind()) : "—";
		rows.add(new SettingRow(x, by, w, SET_ROW, () -> listeningBind = m, null, (g, mm) -> {
			RenderUtil.rect(g, x, by, w, SET_ROW, 0x66101015);
			RenderUtil.text(g, "Клавиша", x + 4, by + 3, 0xFFC8C8D0);
			RenderUtil.textRight(g, listeningBind == m ? "..." : keyLabel, x + w - 4, by + 3, ACCENT);
		}));
		return rows;
	}

	private void registerArea(int x, int y, int w, int h, Runnable click, BiConsumer<Double, Double> drag) {
		if (click == null && drag == null) return;
		areas.add(new Area(x, y, w, h, click, drag));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x(), my = event.y();
		if (mx >= searchBox.getX() && mx <= searchBox.getX() + searchBox.getWidth()
			&& my >= searchBox.getY() && my <= searchBox.getY() + searchBox.getHeight()) {
			setFocused(searchBox);
			searchBox.setFocused(true);
			return searchBox.mouseClicked(event, doubleClick);
		}
		if (getFocused() == searchBox && !searchBox.isFocused()) {
			setFocused(null);
		}
		if (event.button() == 0) {
			for (int i = areas.size() - 1; i >= 0; i--) {
				Area a = areas.get(i);
				if (mx >= a.x() && mx <= a.x() + a.w() && my >= a.y() && my <= a.y() + a.h()) {
					if (a.click() != null) a.click().run();
					if (a.drag() != null) {
						a.drag().accept(mx, my);
						dragArea = a;
					}
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		double mx = event.x(), my = event.y();
		if (dragArea != null) {
			dragArea.drag().accept(mx, my);
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		dragArea = null;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		scroll -= scrollY * 24;
		return super.mouseScrolled(x, y, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (listeningBind != null) {
			if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
				listeningBind.setKeybind(-1);
			} else {
				listeningBind.setKeybind(event.key());
			}
			listeningBind = null;
			return true;
		}
		if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		DarknessClient.getConfigManager().save();
		super.onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
