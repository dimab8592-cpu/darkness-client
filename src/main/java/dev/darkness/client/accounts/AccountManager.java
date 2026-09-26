package dev.darkness.client.accounts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.darkness.client.DarknessClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Смена аккаунтов. Поле Minecraft.user — final, писать его нельзя
 * (IllegalAccessError), поэтому подменяем сам метод getUser() через
 * MinecraftMixin#darkness$overrideUser: весь клиент спрашивает сессию
 * через getUser(), включая рукопожатие с сервером.
 */
public class AccountManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private final List<Account> accounts = new ArrayList<>();
	private final Path file;
	private final Minecraft mc = Minecraft.getInstance();
	private String lastLogin;
	private boolean restoredOnce = false;
	/** Подменная сессия; null = играть под сессией лаунчера. */
	private volatile User sessionOverride;

	public AccountManager(Path configDir) {
		this.file = configDir.resolve("accounts.json");
		load();
	}

	public List<Account> getAccounts() {
		return accounts;
	}

	public String getLastLogin() {
		return lastLogin;
	}

	public User getSessionOverride() {
		return sessionOverride;
	}

	public void add(Account account) {
		accounts.removeIf(a -> a.getName().equalsIgnoreCase(account.getName()));
		accounts.add(account);
		save();
	}

	public void remove(Account account) {
		accounts.remove(account);
		if (account.getName().equalsIgnoreCase(lastLogin)) {
			lastLogin = null;
			sessionOverride = null;
		}
		save();
	}

	/** Смена аккаунта: в мире — сначала выходим в меню (через очередь кадра,
	 *  без реентерабельности из клика), сессия подменяется сразу. */
	public boolean login(Account account) {
		try {
			sessionOverride = new User(account.getName(), account.getUuid(), "0",
				Optional.empty(), Optional.empty());
			lastLogin = account.getName();
			save();
			DarknessClient.LOGGER.info("Account switched to {}", account.getName());
			DarknessClient.getNotifications().show("Аккаунт: §a" + account.getName());
			if (mc.level != null) {
				// выходим из мира так же, как ванильная кнопка «Сохранить и выйти»
				mc.execute(() -> mc.disconnect(new dev.darkness.client.ui.MainMenuScreen(), false));
			}
			return true;
		} catch (Exception e) {
			DarknessClient.LOGGER.warn("Account login failed", e);
			DarknessClient.getNotifications().show("§cОшибка входа: " + e.getMessage());
			return false;
		}
	}

	public String getCurrentName() {
		return mc.getUser().getName();
	}

	/** Один раз за запуск: если лаунчер стартовал с другим ником — возвращаем выбранный. */
	public void restoreOnce() {
		if (restoredOnce) return;
		restoredOnce = true;
		try {
			if (lastLogin == null || lastLogin.isBlank()) return;
			if (getCurrentName().equalsIgnoreCase(lastLogin)) {
				sessionOverride = null; // сессия лаунчера и так правильная
				return;
			}
			for (Account a : accounts) {
				if (a.getName().equalsIgnoreCase(lastLogin)) {
					sessionOverride = new User(a.getName(), a.getUuid(), "0",
						Optional.empty(), Optional.empty());
					DarknessClient.LOGGER.info("Account restored: {}", a.getName());
					return;
				}
			}
		} catch (Throwable ignored) {
		}
	}

	private void load() {
		if (!Files.exists(file)) return;
		try {
			JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
			accounts.clear();
			if (root.has("accounts")) {
				for (var e : root.getAsJsonArray("accounts")) {
					JsonObject o = e.getAsJsonObject();
					accounts.add(new Account(
						o.get("name").getAsString(),
						UUID.fromString(o.get("uuid").getAsString()),
						o.has("type") ? o.get("type").getAsString() : "offline"));
				}
			}
			if (root.has("lastLogin") && !root.get("lastLogin").isJsonNull()) {
				lastLogin = root.get("lastLogin").getAsString();
			}
		} catch (Exception ex) {
			DarknessClient.LOGGER.warn("Accounts load failed", ex);
		}
	}

	private void save() {
		try {
			Files.createDirectories(file.getParent());
			JsonObject root = new JsonObject();
			JsonArray arr = new JsonArray();
			for (Account a : accounts) {
				JsonObject o = new JsonObject();
				o.addProperty("name", a.getName());
				o.addProperty("uuid", a.getUuid().toString());
				o.addProperty("type", a.getType());
				arr.add(o);
			}
			root.add("accounts", arr);
			root.addProperty("lastLogin", lastLogin);
			Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
		} catch (IOException e) {
			DarknessClient.LOGGER.warn("Accounts save failed", e);
		}
	}
}
