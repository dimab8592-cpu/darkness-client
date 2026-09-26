package dev.darkness.client.module.modules.util;

import dev.darkness.client.DarknessClient;
import dev.darkness.client.module.Category;
import dev.darkness.client.module.Module;
import dev.darkness.client.module.NumberSetting;
import net.minecraft.core.Holder;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Отдельное оповещение именно для брони: порог ниже, звук-сигнал. */
public class ArmorNotifierModule extends Module {
	private final NumberSetting threshold;
	private final boolean[] alerted = new boolean[4];
	private static final EquipmentSlot[] SLOTS = {
		EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};
	private static final String[] NAMES = {"Шлем", "Нагрудник", "Штаны", "Ботинки"};

	public ArmorNotifierModule() {
		super("ArmorNotifier", "Звук и уведомление, когда броня близка к поломке", Category.UTILITY);
		threshold = new NumberSetting("Порог, %", "Предупреждать при прочности ниже", 20, 1, 60, 1);
		register(threshold);
	}

	@Override
	public void onTick() {
		if (mc.player == null) return;
		for (int i = 0; i < SLOTS.length; i++) {
			ItemStack st = mc.player.getItemBySlot(SLOTS[i]);
			if (st.isEmpty() || !st.isDamageableItem()) {
				alerted[i] = false;
				continue;
			}
			int left = st.getMaxDamage() - st.getDamageValue();
			float ratio = (float) left / st.getMaxDamage() * 100f;
			if (ratio <= threshold.get().floatValue()) {
				if (!alerted[i]) {
					alerted[i] = true;
					DarknessClient.getNotifications().show(
						"§e" + NAMES[i] + ": §c" + left + " §eпрочности!");
					try {
						mc.getSoundManager().play(SimpleSoundInstance.forUI(
							SoundEvents.NOTE_BLOCK_PLING, 1.4f));
					} catch (Throwable ignored) {
					}
				}
			} else {
				alerted[i] = false;
			}
		}
	}
}
