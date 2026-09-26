package dev.darkness.client.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.darkness.client.DarknessClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Фон главного меню: встроенная картинка (donk + логотип) из ресурсов мода.
 * TLauncher подхватывает мод как classpath-jar без resource-pack, поэтому
 * текстуру грузим через classpath и регистрируем ДО рендера (в Screen.init) —
 * создавать GPU-текстуру посреди кадра нельзя. Ровно ОДНА текстура:
 * вторая подряд DynamicTexture в том же хуке получает пустой GPU-буфер.
 */
public final class CustomizeManager {
	public static final Identifier TITLE_TEXTURE = Identifier.fromNamespaceAndPath("darknessclient", "title_background");
	private static final String BUNDLED_BG_PATH = "/assets/darknessclient/textures/gui/title_background.png";
	private static Path dir;
	private static boolean registered = false;

	private CustomizeManager() {
	}

	public static void init(Path configDir) {
		dir = configDir.resolve("customize");
		try {
			Files.createDirectories(dir);
		} catch (Exception e) {
			DarknessClient.LOGGER.warn("customize dir", e);
		}
	}

	public static Path getDir() {
		return dir;
	}

	public static void openFolder() {
		try {
			if (Desktop.isDesktopSupported()) {
				Desktop.getDesktop().open(dir.toFile());
				return;
			}
		} catch (Exception ignored) {
		}
		try {
			Runtime.getRuntime().exec(new String[]{"explorer", dir.toString()});
		} catch (Exception ignored) {
		}
	}

	/** Готов ли фон к отрисовке. */
	public static boolean isReady() {
		return registered;
	}

	/** Ленивая загрузка: вызывается из renderBackground-хуков при первом кадре меню.
	 *  DynamicTexture + регистрация в момент меню — единственная связка, которая
	 *  стабильно рендерится (проверено); ранние регистрации во время загрузки игры
	 *  и класс SimpleTexture давали пустую текстуру. */
	public static void ensureTextureLoaded() {
		if (registered) return;
		try (var in = CustomizeManager.class.getResourceAsStream(BUNDLED_BG_PATH)) {
			if (in == null) {
				DarknessClient.LOGGER.warn("title background not found on classpath");
				registered = true; // не спамим попытками
				return;
			}
			com.mojang.blaze3d.platform.NativeImage img = com.mojang.blaze3d.platform.NativeImage.read(in);
			Minecraft.getInstance().getTextureManager().register(TITLE_TEXTURE,
				new net.minecraft.client.renderer.texture.DynamicTexture(() -> "darkness-title-bg", img));
			registered = true;
			DarknessClient.LOGGER.info("title background texture ready");
		} catch (Exception e) {
			DarknessClient.LOGGER.warn("title background load failed", e);
			registered = true;
		}
	}

	/** Вызывается из ScreenBackgroundMixin / TitleScreenMixin вместо ванильного фона. */
	public static void renderTitleBackground(GuiGraphics g, int screenW, int screenH) {
		if (!registered) return;
		// именно GUI_TEXTURED: обычный GUI не сэмплит текстуры — был невидимый квад
		RenderPipeline pipeline = RenderPipelines.GUI_TEXTURED;
		g.blit(pipeline, TITLE_TEXTURE, 0, 0, 0.0F, 0.0F, screenW, screenH, screenW, screenH);
	}
}
