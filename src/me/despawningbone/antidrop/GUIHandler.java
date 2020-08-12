package me.despawningbone.antidrop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GUIHandler {
	
	public static HashMap<String, ItemStack> checks = new HashMap<String, ItemStack>();
	public static HashMap<String, ItemStack> checkOffs = new HashMap<String, ItemStack>();
	public static ItemStack locked;
	public static ItemStack submit;
	//Player -> List of filters(List of things to be filtered by this filter(Entry of filter types and values()))
	public static HashMap<UUID, List<List<Entry<String, Object>>>> filterParam = new HashMap<UUID, List<List<Entry<String, Object>>>>();
	public static HashMap<UUID, List<Integer>> slotParam = new HashMap<UUID, List<Integer>>();
	
	public static Enchantment glow = null;
	
	public static void initItems() {   //known bug: any items thats same as the checking items is gonna break the system in some ways //probably will not fix
		new Thread(new Runnable() {
			@Override
		    public void run() {
		    	locked = new ItemStack(Material.STAINED_GLASS_PANE);
				locked.setDurability((short) 14);
				locked = setItemValue(locked, ChatColor.RED + "Locked", Arrays.asList(ChatColor.GRAY + "Any Items in this slot will be locked", ChatColor.GRAY + "and will not be able to be moved", ChatColor.GRAY + "or dropped until you unlocked it."), ConfigHandler.glow);
				
				submit = setItemValue(new ItemStack(Material.SLIME_BALL, 1), ChatColor.GREEN + "Submit", null, ConfigHandler.glow);

				ItemStack checkBase = new ItemStack(Material.WOOL, 1);
				checkBase.setDurability((short) 5);
				checks = new HashMap<String, ItemStack>();
				checks.put("Name", setItemValue(checkBase, ChatColor.AQUA + "Name check", Arrays.asList(ChatColor.DARK_BLUE + "The filter will prevent the dropping of any", ChatColor.DARK_BLUE + "items with the same name in your inventory."), ConfigHandler.glow));
				checks.put("Lore", setItemValue(checkBase, ChatColor.AQUA + "Lore check", Arrays.asList(ChatColor.DARK_BLUE + "The filter will prevent the dropping of any", ChatColor.DARK_BLUE + "items with the same lore in your inventory."), ConfigHandler.glow));
				checks.put("Exact", setItemValue(checkBase, ChatColor.AQUA + "Exact Item", Arrays.asList(ChatColor.DARK_BLUE + "The filter will prevent the dropping of any items", ChatColor.DARK_BLUE + "that is exactly the same in your inventory",
						ChatColor.DARK_BLUE + "Great for items with custom properties like books and banners.",
						ChatColor.DARK_BLUE + "Note: Item amount is ignored."), ConfigHandler.glow));
				checks.put("Type", setItemValue(checkBase, ChatColor.AQUA + "Type check", Arrays.asList(ChatColor.DARK_BLUE + "The filter will prevent the dropping of any items", ChatColor.DARK_BLUE + "that is the same material in your inventory."), ConfigHandler.glow));
				checks.put("Damage", setItemValue(checkBase, ChatColor.AQUA + "Damage check", Arrays.asList(ChatColor.DARK_BLUE + "The filter will prevent the dropping of any", ChatColor.DARK_BLUE + "items with the same damage value in your inventory.",
						ChatColor.DARK_BLUE + "Useful for if you want to keep items",
						ChatColor.DARK_BLUE + "like specific color of wool only."), ConfigHandler.glow));
				if(!ConfigHandler.disableCheck.isEmpty()) {
					for(int i = 0; i < ConfigHandler.disableCheck.size(); i++) {
						checks.remove(ConfigHandler.disableCheck.get(i));
					}	
				}
				checkOffs = new HashMap<String, ItemStack>();
				short dura = 14;
				for (Map.Entry<String, ItemStack> entry : checks.entrySet()) {
					checkOffs.put(entry.getKey(), toOff(entry.getValue(), dura, ConfigHandler.glow));
				}
		    }
		}).start();
	}
	
	private static ItemStack toOff(ItemStack item, short dura, boolean removeench) {
		ItemStack itemOff = item.clone();
		if(removeench) {
			itemOff.removeEnchantment(glow);	
		}
		itemOff.setDurability(dura);
		return itemOff;
	}
	
	private static ItemStack setItemValue(ItemStack item, String name, List<String> lore, boolean addench) {
		ItemStack i = item.clone();
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName(name);
		if(lore != null) {
			meta.setLore(lore);	
		}
		if(addench) {
			meta.addEnchant(glow, 1, true);	
		}
		i.setItemMeta(meta);
		return i;
	}
	
	public static void OpenFilterGUI(ItemStack item, Player player) {
		Inventory filterinv = Bukkit.createInventory(null, 45, ChatColor.BLUE + "AntiDrop Filter");
		for(int i = 0; i < checkOffs.size(); i++) {
			Object[] val = checkOffs.values().toArray();
			filterinv.setItem(centralize(checkOffs.size()).get(i), (ItemStack) val[i]);
		}
		filterinv.setItem(13, item);
		filterinv.setItem(40, submit);
		player.openInventory(filterinv);
	}
	
	public static List<Integer> centralize(int num) {
		if(num == 5) { return Arrays.asList(29, 30, 31, 32, 33); }
		else if(num == 4) {	return Arrays.asList(29, 30, 32, 33); }
		else if(num == 3) {	return Arrays.asList(30, 31, 32); }
		else if(num == 2) {	return Arrays.asList(30, 32); }
		else if(num == 1) {	return Arrays.asList(31); }
		else throw new IllegalArgumentException("Invalid number!");
	}
	
	public static void OpenSlotLockGUI(Player player) {
		int slots = 27;
		if(ConfigHandler.hotbarSl) {
			slots = 45;
		}
		Inventory slinv = Bukkit.createInventory(null, slots, ChatColor.GREEN + "Antidrop lock slot");
		List<ItemStack> iteminv = new ArrayList<ItemStack>(Arrays.asList(player.getInventory().getContents()));
		List<ItemStack> hotbar = new ArrayList<ItemStack>();
		hotbar.addAll(iteminv.subList(0, 9));
		iteminv.subList(0, 9).clear();
		List<ItemStack> maininv = iteminv;
		if(ConfigHandler.hotbarSl) {
			maininv.addAll(Collections.nCopies(9, new ItemStack(Material.AIR)));
			maininv.addAll(hotbar);	
		}
		slinv.setContents(maininv.toArray(new ItemStack[maininv.size()]));  //TODO apparently breaks in 1.12
		if(!slotParam.isEmpty() && slotParam.containsKey(player.getUniqueId()) && !slotParam.get(player.getUniqueId()).isEmpty()) {
			List<Integer> lockList = slotParam.get(player.getUniqueId());
			for(int i = 0; i < lockList.size(); i++) {
				int slot = lockList.get(i);
				if(slot < 9) {
					slot += 36;
				} else {
					slot -= 9;
				}
				slinv.setItem(slot, GUIHandler.locked);
			}
		}
		player.openInventory(slinv);
	}
	
}
