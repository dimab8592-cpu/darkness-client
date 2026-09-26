package dev.darkness.client.util;

import dev.darkness.client.DarknessClient;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Ресурс-пак DarknessFont: перекрывает ванильный шрифт (minecraft:default).
 * Сейчас — Segoe UI: простой, чистый, с кириллицей. Пак создаётся в
 * resourcepacks игры из шрифта, вшитого в мод, и включается автоматически.
 *
 * При смене шрифта: заменить ttf в ресурсах, обновить FONT_JSON и PACK_VERSION —
 * старый zip пересоберётся сам (включённый пак сначала снимается, файл
 * отпускается асинхронным reload'ом, пересборка довыполнится на следующих кадрах).
 */
public final class FontPackManager {
	private static final String PACK_ID = "file/DarknessFont.zip";
	/** Менять при любом изменении содержимого пака — тогда zip пересоберётся сам. */
	private static final String PACK_VERSION = "v2-segoe-ui";
	private static final String FONT_JSON = """
		{
		    "providers": [
		        { "type": "reference", "id": "minecraft:include/space" },
		        { "type": "ttf", "file": "darknessfont:segoeui.ttf", "size": 11.0, "oversample": 3.0 },
		        { "type": "reference", "id": "minecraft:include/unifont" }
		    ]
		}
		""";
	private static final String PACK_META = """
		{
		    "pack": {
		        "description": "Darkness Client font",
		        "min_format": 75,
		        "max_format": 75
		    }
		}
		""";
	private static boolean settled = false;

	private FontPackManager() {
	}

	public static void ensureEnabled() {
		if (settled) return;
		try {
			Minecraft mc = Minecraft.getInstance();
			Path zip = mc.getResourcePackDirectory().resolve("DarknessFont.zip");
			var repo = mc.getResourcePackRepository();
			boolean enabled = repo.getSelectedIds().contains(PACK_ID);

			String diskVersion = readVersion(zip);
			if (!PACK_VERSION.equals(diskVersion)) {
				if (enabled) {
					// сначала снимаем пак и отпускаем файл (reload асинхронный)
					repo.removePack(PACK_ID);
					mc.reloadResourcePacks();
					return;
				}
				try {
					createPack(zip);
				} catch (IOException stillLocked) {
					return; // файл ещё держит старый reload — повторим на следующем кадре
				}
				DarknessClient.LOGGER.info("DarknessFont.zip rebuilt ({})", PACK_VERSION);
			}
			repo.reload();
			if (repo.isAvailable(PACK_ID) && !repo.getSelectedIds().contains(PACK_ID)) {
				repo.addPack(PACK_ID);
				mc.reloadResourcePacks();
			}
			settled = true;
		} catch (Exception e) {
			DarknessClient.LOGGER.warn("font pack failed", e);
			settled = true; // не спамим попытками, если окружение сломано
		}
	}

	private static String readVersion(Path zip) {
		if (!Files.isRegularFile(zip)) return null;
		try (ZipFile zf = new ZipFile(zip.toFile())) {
			ZipEntry e = zf.getEntry("fontversion");
			if (e == null) return null;
			return new String(zf.getInputStream(e).readAllBytes(), StandardCharsets.UTF_8).trim();
		} catch (IOException broken) {
			return null;
		}
	}

	private static void createPack(Path zip) throws IOException {
		try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
			put(out, "pack.mcmeta", PACK_META.getBytes(StandardCharsets.UTF_8));
			put(out, "fontversion", PACK_VERSION.getBytes(StandardCharsets.UTF_8));
			put(out, "assets/minecraft/font/default.json", FONT_JSON.getBytes(StandardCharsets.UTF_8));
			copyIn(out, "assets/darknessfont/font/segoeui.ttf", "/assets/darknessclient/fonts/segoeui.ttf");
		}
	}

	private static void put(ZipOutputStream out, String name, byte[] bytes) throws IOException {
		out.putNextEntry(new ZipEntry(name));
		out.write(bytes);
		out.closeEntry();
	}

	private static void copyIn(ZipOutputStream out, String name, String resource) throws IOException {
		try (InputStream in = FontPackManager.class.getResourceAsStream(resource)) {
			if (in == null) throw new IOException("missing " + resource);
			out.putNextEntry(new ZipEntry(name));
			in.transferTo(out);
			out.closeEntry();
		}
	}
}
