package dev.darkness.client.util;

import dev.darkness.client.DarknessClient;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Кастомные MP3-звуки: положи свой файл в config/darkness/sounds/
 *   totem.mp3 — звук срабатывания тотема
 *   crit.mp3  — звук критического удара
 * Файлы подхватываются на лету, без перезапуска.
 */
public final class SoundManager {
	private static final AtomicInteger playing = new AtomicInteger(0);
	private static Path soundsDir;

	private SoundManager() {
	}

	public static void init(Path configDir) {
		soundsDir = configDir.resolve("sounds");
		try {
			Files.createDirectories(soundsDir);
			Path readme = soundsDir.resolve("readme.txt");
			if (!Files.exists(readme)) {
				Files.writeString(readme, """
					Кастомные звуки Darkness Client (формат MP3):
					totem.mp3 - звук срабатывания тотема
					crit.mp3  - звук критического удара
					Просто переименуй свой mp3 и положи в эту папку.
					""", java.nio.charset.StandardCharsets.UTF_8);
			}
		} catch (Exception e) {
			DarknessClient.LOGGER.warn("sounds dir failed", e);
		}
	}

	public static boolean hasCustom(String name) {
		return soundsDir != null && Files.isRegularFile(soundsDir.resolve(name + ".mp3"));
	}

	public static void play(String name, float volume) {
		if (soundsDir == null) return;
		File f = soundsDir.resolve(name + ".mp3").toFile();
		if (!f.isFile() || playing.get() >= 4) return;
		Thread t = new Thread(() -> {
			playing.incrementAndGet();
			try (var in = new BufferedInputStream(new FileInputStream(f))) {
				javazoom.jl.player.Player p = new javazoom.jl.player.Player(in);
				p.play();
			} catch (Throwable e) {
				DarknessClient.LOGGER.debug("mp3 {} failed: {}", name, e.toString());
			} finally {
				playing.decrementAndGet();
			}
		}, "darkness-sound");
		t.setDaemon(true);
		t.start();
	}

	public static void playTotem() {
		play("totem", 1f);
	}

	public static void playCrit() {
		play("crit", 0.8f);
	}
}
