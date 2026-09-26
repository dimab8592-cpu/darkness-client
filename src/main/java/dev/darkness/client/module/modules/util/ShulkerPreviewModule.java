package dev.darkness.client.module.modules.util;

import dev.darkness.client.module.Category;
import dev.darkness.client.module.Module;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/** Просмотр содержимого шалкера при наведении (отрисовку делает миксин). */
public class ShulkerPreviewModule extends Module {
	public ShulkerPreviewModule() {
		super("ShulkerPreview", "Содержимое шалкер-бокса при наведении курсора", Category.UTILITY);
	}

	public static boolean isShulker(ItemStack stack) {
		if (stack.isEmpty()) return false;
		if (!(stack.getItem() instanceof BlockItem bi)) return false;
		String id = bi.getBlock().getDescriptionId();
		return id != null && id.contains("shulker_box");
	}

	public static ItemStack[] contents(ItemStack stack) {
		var contents = stack.get(DataComponents.CONTAINER);
		if (contents == null) return null;
		var list = net.minecraft.core.NonNullList.withSize(27, ItemStack.EMPTY);
		contents.copyInto(list);
		return list.toArray(new ItemStack[0]);
	}
}
