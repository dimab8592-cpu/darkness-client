package dev.darkness.client.module.modules.util;

import dev.darkness.client.DarknessClient;
import dev.darkness.client.module.Category;
import dev.darkness.client.module.Module;
import dev.darkness.client.module.NumberSetting;
import dev.darkness.client.module.BooleanSetting;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;

/** Быстрая очистка основного инвентаря: выбрасывает всё из слотов 9–35. */
public class ClearInventoryModule extends Module {
	private final BooleanSetting keepTotems;
	private final NumberSetting speed;

	public ClearInventoryModule() {
		super("ClearInventory", "Выбрасывает весь мусор из инвентаря (слоты 9–35)", Category.UTILITY);
		keepTotems = new BooleanSetting("Сохранять тотемы", "Не выбрасывать тотемы бессмертия", true);
		speed = new NumberSetting("Скорость", "Стопок за тик", 3, 1, 10, 1);
		register(keepTotems, speed);
	}

	@Override
	public boolean isActionOnly() {
		return true;
	}

	@Override
	public void onKeyPressed() {
		setEnabled(true); // запускаем очистку, модуль сам выключится по завершении
	}

	@Override
	protected void onEnable() {
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.gameMode == null) {
			setEnabled(false);
			return;
		}
		int dropped = 0;
		int budget = (int) Math.round(speed.get());
		for (int slot = 9; slot <= 35 && dropped < budget; slot++) {
			var st = mc.player.inventoryMenu.getSlot(slot).getItem();
			if (st.isEmpty()) continue;
			if (keepTotems.get() && st.is(Items.TOTEM_OF_UNDYING)) continue;
			mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, slot, 1,
				ClickType.THROW, mc.player);
			dropped++;
		}
		if (dropped == 0) {
			setEnabled(false);
			DarknessClient.getNotifications().show("§aИнвентарь очищен");
		}
	}

	@Override
	public void toggle() {
		// как действие: нажатие = запуск очистки, а не вкл/выкл
		onKeyPressed();
	}
}
