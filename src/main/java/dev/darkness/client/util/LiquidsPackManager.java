package dev.darkness.client.util;

import dev.darkness.client.DarknessClient;
import net.minecraft.client.Minecraft;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Ресурс-пак DarknessLiquids: полупрозрачные текстуры воды и лавы.
 * PNG генерируются на лету (32bpp ARGB), пак включается автоматически —
 * тот же ванильный механизм, что и у шрифтового пака.
 */
public final class LiquidsPackManager {
	private static final String PACK_ID = "file/DarknessLiquids.zip";
	private static final String PACK_META = """
		{
		    "pack": {
		        "description": "Darkness Client transparent liquids",
		        "min_format": 75,
		        "max_format": 75
		    }
		}
		""";
	private static String writtenConfig = "";

	private LiquidsPackManager() {
	}

	/** Включает/пересобирает пак с заданными параметрами. Вызывать на тике модуля:
	 *  пересборка после отключения пака довыполнится на следующих тиках. */
	public static void apply(boolean water, boolean lava, int alpha) {
		String cfg = water + "|" + lava + "|" + alpha;
		try {
			Minecraft mc = Minecraft.getInstance();
			Path zip = mc.getResourcePackDirectory().resolve("DarknessLiquids.zip");
			var repo = mc.getResourcePackRepository();
			boolean enabled = repo.getSelectedIds().contains(PACK_ID);
			if (!Files.exists(zip) || !cfg.equals(writtenConfig)) {
				if (enabled) {
					// сначала снимаем пак и отпускаем файл (reload асинхронный)
					repo.removePack(PACK_ID);
					mc.reloadResourcePacks();
					return;
				}
				try {
					createPack(zip, water, lava, alpha);
					writtenConfig = cfg;
					DarknessClient.LOGGER.info("DarknessLiquids.zip rebuilt: {}", cfg);
				} catch (IOException stillLocked) {
					return; // файл ещё держит старый reload — повторим на следующем тике
				}
			}
			repo.reload();
			if (repo.isAvailable(PACK_ID) && !repo.getSelectedIds().contains(PACK_ID)) {
				repo.addPack(PACK_ID);
				mc.reloadResourcePacks();
			}
		} catch (Exception e) {
			DarknessClient.LOGGER.warn("liquids pack failed", e);
		}
	}

	public static void disable() {
		try {
			Minecraft mc = Minecraft.getInstance();
			var repo = mc.getResourcePackRepository();
			if (repo.getSelectedIds().contains(PACK_ID)) {
				repo.removePack(PACK_ID);
				mc.reloadResourcePacks();
			}
		} catch (Exception e) {
			DarknessClient.LOGGER.warn("liquids pack disable failed", e);
		}
	}

	private static void createPack(Path zip, boolean water, boolean lava, int alpha) throws IOException {
		try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
			put(out, "pack.mcmeta", PACK_META.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			if (water) {
				// вода красится биомом — текстура почти серая
				put(out, "assets/minecraft/textures/block/water_still.png", liquidPng(225, 225, 235, alpha));
				put(out, "assets/minecraft/textures/block/water_flow.png", liquidPng(225, 225, 235, alpha));
			}
			if (lava) {
				put(out, "assets/minecraft/textures/block/lava_still.png", liquidPng(207, 92, 20, alpha));
				put(out, "assets/minecraft/textures/block/lava_flow.png", liquidPng(207, 92, 20, alpha));
			}
		}
	}

	private static byte[] liquidPng(int r, int g, int b, int alpha) throws IOException {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		int argb = (alpha << 24) | (r << 16) | (g << 8) | b;
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				img.setRGB(x, y, argb);
			}
		}
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ImageIO.write(img, "png", bos);
		return bos.toByteArray();
	}

	private static void put(ZipOutputStream out, String name, byte[] bytes) throws IOException {
		out.putNextEntry(new ZipEntry(name));
		out.write(bytes);
		out.closeEntry();
	}
}
