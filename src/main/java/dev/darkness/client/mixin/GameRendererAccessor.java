package dev.darkness.client.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Доступ к приватному getFov: ровно тот FOV, которым рендерится мир
 *  (учитывает спринт/эффекты и наш NoFov) — чтобы 2D-оверлеи не съезжали. */
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
	@Invoker("getFov")
	float darkness$invokeGetFov(Camera camera, float partialTick, boolean useFovSetting);
}
