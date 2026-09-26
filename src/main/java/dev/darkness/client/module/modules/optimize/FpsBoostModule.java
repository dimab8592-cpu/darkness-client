package dev.darkness.client.module.modules.optimize;

import dev.darkness.client.module.BooleanSetting;
import dev.darkness.client.module.Category;
import dev.darkness.client.module.Module;
import dev.darkness.client.module.NumberSetting;
import net.minecraft.client.CloudStatus;
import net.minecraft.server.level.ParticleStatus;

public class FpsBoostModule extends Module {
	private final BooleanSetting minParticles;
	private final BooleanSetting noClouds;
	private final BooleanSetting noVignette;
	private final BooleanSetting noVsync;
	private final BooleanSetting lowBiomeBlend;
	private final BooleanSetting noMipmap;
	private final BooleanSetting halfEntityDistance;
	private final NumberSetting fpsLimit;

	private ParticleStatus prevParticles;
	private CloudStatus prevClouds;
	private Boolean prevVignette;
	private Integer prevMipmap;
	private Integer prevBlend;
	private Double prevEntityDist;
	private boolean vsyncApplied = false;

	public FpsBoostModule() {
		super("FpsBoost", "Выжимает максимум FPS: vsync, частицы, облака, биомы", Category.OPTIMIZATION);
		minParticles = new BooleanSetting("Минимум частиц", "Particle status: minimal", true);
		noClouds = new BooleanSetting("Без облаков", "Отключить облака", true);
		noVignette = new BooleanSetting("Без виньетки", "Отключить виньетку", false);
		noVsync = new BooleanSetting("Без vsync", "Отключить синхронизацию с монитором (самый большой прирост FPS)", true);
		lowBiomeBlend = new BooleanSetting("Без смешивания биомов", "biomeBlendRadius = 0", true);
		noMipmap = new BooleanSetting("Без мипмапов", "mipmapLevels = 0", true);
		halfEntityDistance = new BooleanSetting("Дальность сущностей 50%", "Мобы/игроки прорисовываются вдвое ближе (большой прирост в замесах)", true);
		fpsLimit = new NumberSetting("Лимит FPS", "Ограничение кадров (260 = почти без лимита)", 260, 60, 260, 10);
		register(minParticles, noClouds, noVignette, noVsync, lowBiomeBlend, noMipmap, halfEntityDistance, fpsLimit);
	}

	@Override
	protected void onEnable() {
		if (mc.options == null) return;
		prevParticles = mc.options.particles().get();
		prevClouds = mc.options.getCloudsType();
		prevVignette = mc.options.vignette().get();
		prevMipmap = mc.options.mipmapLevels().get();
		prevBlend = mc.options.biomeBlendRadius().get();
		prevEntityDist = mc.options.entityDistanceScaling().get();
		vsyncApplied = false;
		apply();
	}

	private void apply() {
		if (mc.options == null) return;
		if (minParticles.get()) mc.options.particles().set(ParticleStatus.MINIMAL);
		if (noClouds.get()) mc.options.cloudStatus().set(CloudStatus.OFF);
		if (noVignette.get()) mc.options.vignette().set(false);
		if (lowBiomeBlend.get()) mc.options.biomeBlendRadius().set(0);
		if (halfEntityDistance.get()) mc.options.entityDistanceScaling().set(0.5);
		// мипмапы запускают перезагрузку ресурсов — только из интерактивного меню,
		// не во время загрузки игры/мира (иначе возможен дедлок)
		if (noMipmap.get() && mc.options.mipmapLevels().get() != 0
			&& mc.getOverlay() == null && mc.screen != null) {
			mc.options.mipmapLevels().set(0);
		}
		int limit = (int) Math.round(fpsLimit.get());
		if (mc.options.framerateLimit().get() != limit) {
			mc.options.framerateLimit().set(limit);
		}
		// vsync: применяется на окно один раз, дальше не трогаем
		if (noVsync.get() && !vsyncApplied && mc.getWindow() != null) {
			mc.options.enableVsync().set(false);
			mc.getWindow().updateVsync(false);
			vsyncApplied = true;
		}
	}

	@Override
	public void onTick() {
		apply();
	}

	@Override
	protected void onDisable() {
		if (mc.options == null) return;
		if (prevParticles != null) mc.options.particles().set(prevParticles);
		if (prevClouds != null) mc.options.cloudStatus().set(prevClouds);
		if (prevVignette != null) mc.options.vignette().set(prevVignette);
		if (prevMipmap != null) mc.options.mipmapLevels().set(prevMipmap);
		if (prevBlend != null) mc.options.biomeBlendRadius().set(prevBlend);
		if (prevEntityDist != null) mc.options.entityDistanceScaling().set(prevEntityDist);
		if (vsyncApplied && mc.getWindow() != null) {
			mc.options.enableVsync().set(true);
			mc.getWindow().updateVsync(true);
		}
	}
}
