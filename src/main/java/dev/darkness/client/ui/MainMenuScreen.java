package dev.darkness.client.ui;

import dev.darkness.client.DarknessClient;
import dev.darkness.client.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Главное меню в стиле PulseVisuals: фон-картинка с затемнением, большие
 * кнопки по центру, чип аккаунта слева сверху, версия/часы/тулбар снизу.
 * Вызывается вместо ванильного TitleScreen (см. MinecraftMixin).
 */
public class MainMenuScreen extends Screen {
	private static final int ACCENT = 0xFF8B5CF6;
	private static final int ACCENT_SOFT = 0x668B5CF6;
	private static final int TEXT_MAIN = 0xFFF2F2F2;
	private static final int TEXT_DIM = 0xFF9A9AA5;

	private record Zone(int x, int y, int w, int h, Runnable click) {
	}

	private final List<Zone> zones = new ArrayList<>();
	private long openedAt = 0;

	public MainMenuScreen() {
		super(Component.literal("Darkness Client"));
	}

	@Override
	protected void init() {
		super.init();
		openedAt = System.currentTimeMillis();
		var am = DarknessClient.getAccountManager();
		if (am != null) am.restoreOnce();
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		zones.clear();
		int w = this.width, h = this.height;

		// фон уже нарисован renderBackground-хуком (donk); добавляем затемнение как в Pulse
		RenderUtil.rect(g, 0, 0, w, h, 0x59000000);
		// затемнение под кнопками
		for (int i = 0; i < 12; i++) {
			RenderUtil.rect(g, 0, (int) (h * 0.40f) + i * 2, w, 2, 0x30000000 + i * 0x02000000);
		}

		// ---- центральные большие кнопки ----
		int bw = (int) Math.max(100, Math.min(130, w * 0.24));
		int bh = 14;
		int bx = (w - bw) / 2;
		int y = (int) (h * 0.46f);
		bigButton(g, bx, y, bw, bh, "Одиночная игра", false, mouseX, mouseY,
			() -> minecraft.setScreen(new SelectWorldScreen(this)));
		y += bh + 4;
		bigButton(g, bx, y, bw, bh, "Сетевая игра", false, mouseX, mouseY,
			() -> minecraft.setScreen(new JoinMultiplayerScreen(this)));
		y += bh + 4;
		bigButton(g, bx, y, bw, bh, "Меню функций", true, mouseX, mouseY,
			() -> minecraft.setScreen(new ClickGuiScreen()));
		y += bh + 8;
		RenderUtil.textCentered(g, "RSHIFT — меню функций", w / 2, y, 0x8AFFFFFF);

		// ---- чип аккаунта слева сверху ----
		String nick = "";
		var am = DarknessClient.getAccountManager();
		if (am != null) nick = am.getCurrentName();
		int ax = 8, ay = 8;
		int chipW = 6 + 18 + 4 + Math.max(40, font.width(nick)) + 8;
		RenderUtil.rect(g, ax, ay, chipW, 24, 0xCC14141A);
		RenderUtil.outline(g, ax, ay, chipW, 24, 0x33FFFFFF);
		RenderUtil.rect(g, ax + 4, ay + 4, 16, 16, ACCENT);
		String letter = nick.isEmpty() ? "?" : nick.substring(0, 1).toUpperCase();
		RenderUtil.textCentered(g, letter, ax + 12, ay + 8, 0xFFFFFFFF);
		RenderUtil.text(g, nick, ax + 24, ay + 5, TEXT_MAIN);
		RenderUtil.rect(g, ax + 24, ay + 15, 3, 3, 0xFF30E860); // зелёная точка онлайна
		RenderUtil.text(g, "офлайн аккаунт", ax + 30, ay + 14, TEXT_DIM);
		registerZone(ax, ay, chipW, 24, () -> minecraft.setScreen(new AccountsScreen(this)));

		// ---- пилюля версии + шестерёнка справа сверху ----
		String ver = "DC v" + DarknessClient.VERSION;
		int pw = font.width(ver) + 12;
		int px = w - pw - 8 - 22, py = 8;
		RenderUtil.rect(g, px, py, pw, 13, 0xCC14141A);
		RenderUtil.outline(g, px, py, pw, 13, 0x33FFFFFF);
		RenderUtil.textCentered(g, ver, px + pw / 2, py + 3, ACCENT);
		iconButton(g, w - 8 - 16, py - 1, "O", () ->
			minecraft.setScreen(new OptionsScreen(this, this.minecraft.options)));

		// ---- нижний тулбар по центру ----
		String[] labels = {"Настройки", "Функции", "Аккаунты", "Сервера", "Выход"};
		Runnable[] acts = {
			() -> minecraft.setScreen(new OptionsScreen(this, this.minecraft.options)),
			() -> minecraft.setScreen(new ClickGuiScreen()),
			() -> minecraft.setScreen(new AccountsScreen(this)),
			() -> minecraft.setScreen(new JoinMultiplayerScreen(this)),
			() -> this.minecraft.stop()
		};
		int tw = 0;
		for (String s : labels) tw += font.width(s) + 14;
		int tx = (w - tw) / 2;
		int ty = h - 22;
		for (int i = 0; i < labels.length; i++) {
			int cw = font.width(labels[i]) + 10;
			RenderUtil.rect(g, tx, ty, cw, 14, 0xB016161E);
			RenderUtil.outline(g, tx, ty, cw, 14, 0x2EFFFFFF);
			RenderUtil.textCentered(g, labels[i], tx + cw / 2, ty + 3, 0xFFD8D8DE);
			final Runnable act = acts[i];
			final int fx = tx, fw = cw;
			registerZone(fx, ty, fw, 14, act);
			tx += cw + 4;
		}

		// ---- низ слева: версия клиента ----
		RenderUtil.text(g, "Darkness Client " + DarknessClient.VERSION, 8, h - 34, TEXT_DIM);
		RenderUtil.text(g, "Minecraft 1.21.11", 8, h - 24, 0xFF6A6A75);

		// ---- низ справа: приветствие + часы ----
		String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
		int cw2 = font.width(time) + 12;
		int cx2 = w - 8 - cw2;
		RenderUtil.rect(g, cx2, h - 24, cw2, 14, 0xCC14141A);
		RenderUtil.outline(g, cx2, h - 24, cw2, 14, 0x33FFFFFF);
		RenderUtil.textCentered(g, time, cx2 + cw2 / 2, h - 21, TEXT_MAIN);
		String gift = "С возвращением, " + nick + "!";
		int gw = font.width(gift) + 12;
		int gx = cx2 - gw - 4;
		RenderUtil.rect(g, gx, h - 24, gw, 14, 0xCC14141A);
		RenderUtil.outline(g, gx, h - 24, gw, 14, ACCENT_SOFT);
		RenderUtil.textCentered(g, gift, gx + gw / 2, h - 21, 0xFFE8E0FF);
	}

	private void bigButton(GuiGraphics g, int x, int y, int w, int h, String label, boolean highlight,
						   int mouseX, int mouseY, Runnable action) {
		boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
		if (highlight) {
			// фиолетовая градиентная кнопка как «Магазин» в Pulse
			for (int i = 0; i < h; i++) {
				int c = RenderUtil.lerpColor(0xFF7C3AED, 0xFF9F67FF, i / (float) h);
				RenderUtil.rect(g, x, y + i, w, 1, c);
			}
			RenderUtil.outline(g, x, y, w, h, 0xFFC4B0FF);
		} else {
			RenderUtil.rect(g, x, y, w, h, hovered ? 0xE0252530 : 0xCC1C1C24);
			RenderUtil.outline(g, x, y, w, h, hovered ? ACCENT_SOFT : 0x33FFFFFF);
		}
		RenderUtil.textCentered(g, label, x + w / 2, y + (h - 8) / 2, highlight ? 0xFFFFFFFF : TEXT_MAIN);
		registerZone(x, y, w, h, action);
	}

	private void iconButton(GuiGraphics g, int x, int y, String glyph, Runnable action) {
		RenderUtil.rect(g, x, y, 16, 15, 0xB016161E);
		RenderUtil.outline(g, x, y, 16, 15, 0x2EFFFFFF);
		RenderUtil.textCentered(g, glyph, x + 8, y + 4, 0xFFD8D8DE);
		registerZone(x, y, 16, 15, action);
	}

	private void registerZone(int x, int y, int w, int h, Runnable action) {
		zones.add(new Zone(x, y, w, h, action));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		// защита от «пролётного» клика сразу после смены экрана (например,
		// клик по «Сохранить и выйти» не должен попасть в «Выход» нового меню)
		if (System.currentTimeMillis() - openedAt < 400) {
			return super.mouseClicked(event, doubleClick);
		}
		if (event.button() == 0) {
			double mx = event.x(), my = event.y();
			for (int i = zones.size() - 1; i >= 0; i--) {
				Zone z = zones.get(i);
				if (mx >= z.x() && mx <= z.x() + z.w() && my >= z.y() && my <= z.y() + z.h()) {
					z.click().run();
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
		// ESC на главном меню ничего не закрывает
		if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) return true;
		return super.keyPressed(event);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void onClose() {
		// главное меню нельзя закрыть в пустой экран
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
