package dev.darkness.client.ui;

import dev.darkness.client.DarknessClient;
import dev.darkness.client.accounts.Account;
import dev.darkness.client.accounts.AccountManager;
import dev.darkness.client.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Смена аккаунтов в стиле главного меню: модальное окно, аватары, выбранный подсвечен. */
public class AccountsScreen extends Screen {
	private static final int ACCENT = 0xFF8B5CF6;
	private static final int ACCENT_SOFT = 0x668B5CF6;

	private EditBox nickBox;
	private final List<ClickZone> zones = new ArrayList<>();
	private final Screen parent;
	private long openedAt = 0;

	private record ClickZone(int x, int y, int w, int h, Runnable action) {
	}

	public AccountsScreen(Screen parent) {
		super(Component.literal("Аккаунты"));
		this.parent = parent;
	}

	public AccountsScreen() {
		this(null);
	}

	@Override
	protected void init() {
		super.init();
		openedAt = System.currentTimeMillis();
		int boxX = this.width / 2 - 70;
		int boxY = this.height / 2 + Math.min(92, this.height / 4) - 20;
		nickBox = new EditBox(this.font, boxX, boxY, 140, 12, Component.literal("Ник"));
		nickBox.setMaxLength(16);
		nickBox.setHint(Component.literal("Ник нового аккаунта"));
		nickBox.setCanLoseFocus(true);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		zones.clear();
		AccountManager am = DarknessClient.getAccountManager();
		int w = this.width, h = this.height;

		RenderUtil.rect(g, 0, 0, w, h, 0x88040408);

		// модальное окно
		int mw = Math.min(240, w - 20), mh = Math.min(190, h - 20);
		int mx0 = (w - mw) / 2, my0 = (h - mh) / 2;
		RenderUtil.rect(g, mx0, my0, mw, mh, 0xF2141418);
		RenderUtil.outline(g, mx0, my0, mw, mh, 0x30FFFFFF);

		// шапка
		RenderUtil.rect(g, mx0 + 1, my0 + 1, mw - 2, 18, 0xF21B1B22);
		RenderUtil.text(g, "СМЕНА АККАУНТА", mx0 + 8, my0 + 6, 0xFFF2F2F2);
		RenderUtil.textRight(g, "сейчас: " + am.getCurrentName(), mx0 + mw - 8, my0 + 6, ACCENT);

		// список аккаунтов
		List<Account> accounts = am.getAccounts();
		int rowH = 22;
		int listY = my0 + 24;
		int listH = mh - 24 - 26;
		int maxRows = Math.max(1, (listH - 2) / (rowH + 2));
		int from = Math.max(0, accounts.size() - maxRows); // показываем последние
		int ry = listY;
		for (int i = from; i < accounts.size(); i++) {
			Account acc = accounts.get(i);
			boolean current = acc.getName().equalsIgnoreCase(am.getCurrentName());
			boolean hovered = mouseX >= mx0 + 6 && mouseX <= mx0 + mw - 6
				&& mouseY >= ry && mouseY <= ry + rowH;

			RenderUtil.rect(g, mx0 + 6, ry, mw - 12, rowH, current ? 0xE0252133 : (hovered ? 0xF02A2A33 : 0xF0222228));
			RenderUtil.outline(g, mx0 + 6, ry, mw - 12, rowH, current ? ACCENT_SOFT : 0x14FFFFFF);
			if (current) {
				RenderUtil.rect(g, mx0 + 6, ry, 2, rowH, ACCENT);
			}
			// аватар
			RenderUtil.rect(g, mx0 + 10, ry + 3, 16, 16, current ? ACCENT : 0xFF3A3A44);
			RenderUtil.textCentered(g, acc.getName().substring(0, 1).toUpperCase(), mx0 + 18, ry + 7, 0xFFFFFFFF);
			RenderUtil.text(g, acc.getName(), mx0 + 32, ry + 4, current ? 0xFFF2F2F2 : 0xFFC8C8D0);
			RenderUtil.text(g, "офлайн", mx0 + 32, ry + 13, 0xFF9A9AA5);
			if (current) {
				RenderUtil.textRight(g, "выбран", mx0 + mw - 10, ry + 4, ACCENT);
			} else {
				int loginW = 38, delW = 38;
				int bx = mx0 + mw - 10 - loginW - delW - 4;
				RenderUtil.rect(g, bx, ry + 4, loginW, 14, 0xE0252133);
				RenderUtil.outline(g, bx, ry + 4, loginW, 14, ACCENT_SOFT);
				RenderUtil.textCentered(g, "Войти", bx + loginW / 2, ry + 7, 0xFFE8E0FF);
				final Account a = acc;
				zones.add(new ClickZone(bx, ry + 4, loginW, 14, () -> {
					if (DarknessClient.getAccountManager().login(a)
						&& this.minecraft.screen == this) {
						// экран уже сменился, если нас выкинуло из мира — тогда ничего не трогаем
						onClose();
					}
				}));
				int dx = bx + loginW + 4;
				RenderUtil.rect(g, dx, ry + 4, delW, 14, 0xE02A1A1A);
				RenderUtil.outline(g, dx, ry + 4, delW, 14, 0x66FF5252);
				RenderUtil.textCentered(g, "Удалить", dx + delW / 2, ry + 7, 0xFFFFB0B0);
				zones.add(new ClickZone(dx, ry + 4, delW, 14, () -> {
					DarknessClient.getAccountManager().remove(a);
				}));
			}
			ry += rowH + 2;
		}
		if (accounts.isEmpty()) {
			RenderUtil.textCentered(g, "Аккаунтов нет — добавь ниже", mx0 + mw / 2, listY + 10, 0xFF9A9AA5);
		}

		// нижняя панель: ввод ника + добавить
		int fy = my0 + mh - 24;
		RenderUtil.rect(g, mx0 + 1, fy - 1, mw - 2, 1, 0x22FFFFFF);
		nickBox.setX(mx0 + 8);
		nickBox.setY(fy + 4);
		nickBox.setWidth(150);
		nickBox.render(g, mouseX, mouseY, partialTick);
		int addX = mx0 + 8 + 154, addW = mw - 16 - 154;
		RenderUtil.rect(g, addX, fy + 3, addW, 14, 0xE0252133);
		RenderUtil.outline(g, addX, fy + 3, addW, 14, ACCENT_SOFT);
		RenderUtil.textCentered(g, "Добавить", addX + addW / 2, fy + 6, 0xFFE8E0FF);
		zones.add(new ClickZone(addX, fy + 3, addW, 14, this::addAccount));

		RenderUtil.text(g, "Esc — назад", mx0 + 8, my0 + mh - 34, 0xFF6A6A75);
	}

	private void addAccount() {
		String nick = nickBox.getValue().trim();
		if (nick.isEmpty() || !nick.matches("[A-Za-z0-9_]{1,16}")) {
			DarknessClient.getNotifications().show("§cНик: 1-16 символов A-Z, 0-9, _");
			return;
		}
		Account acc = Account.offline(nick);
		DarknessClient.getAccountManager().add(acc);
		DarknessClient.getNotifications().show("Добавлен: §a" + nick);
		nickBox.setValue("");
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x(), my = event.y();
		if (mx >= nickBox.getX() && mx <= nickBox.getX() + nickBox.getWidth()
			&& my >= nickBox.getY() && my <= nickBox.getY() + nickBox.getHeight()) {
			setFocused(nickBox);
			nickBox.setFocused(true);
			return nickBox.mouseClicked(event, doubleClick);
		}
		if (event.button() == 0 && System.currentTimeMillis() - openedAt > 400) {
			for (int i = zones.size() - 1; i >= 0; i--) {
				ClickZone z = zones.get(i);
				if (mx >= z.x() && mx <= z.x() + z.w() && my >= z.y() && my <= z.y() + z.h()) {
					z.action().run();
					return true;
				}
			}
		}
		nickBox.setFocused(false);
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (nickBox.isFocused() && event.key() == GLFW.GLFW_KEY_ENTER) {
			addAccount();
			return true;
		}
		if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	public boolean isNickFocused() {
		return nickBox != null && nickBox.isFocused();
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(parent != null ? parent : new MainMenuScreen());
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
