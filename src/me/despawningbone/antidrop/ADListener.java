package me.despawningbone.antidrop;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import me.despawningbone.antidrop.utils.Timer;

public class ADListener implements Listener {
	
	private ADMain plugin;
	
	public ADListener(ADMain instance) {
		plugin = instance;
	}
	
	public static HashMap<UUID, List<Integer>> slotsBuffer = new HashMap<UUID, List<Integer>>();
	
	public static List<UUID> cooldown = new ArrayList<UUID>();
	
	private HashMap<UUID, List<Entry<Integer, ItemStack>>> toBeKeeped = new HashMap<UUID, List<Entry<Integer, ItemStack>>>();  //will reset if player didnt respawn in time
	
	public void sendCooldownMessage(Player player, String msg) {
		if(!cooldown.contains(player.getUniqueId())) {
			player.sendMessage(msg);
			cooldown.add(player.getUniqueId());
			Timer.cooldown(player);
		}
	}
	
	@EventHandler
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		Player player = event.getPlayer();
		if(player.getGameMode() != GameMode.CREATIVE) {
			if((ConfigHandler.usePerms && player.hasPermission("adp.filter")) || !ConfigHandler.usePerms) {
				boolean cancel = filterCheckStart(player, event.getItemDrop().getItemStack());  //filter check
				if(cancel) {
					event.setCancelled(true);
					sendCooldownMessage(player, ADMain.prefix + ChatColor.BLUE + "Item is in the filter list! You will not be able to drop items the filtered list until you remove it with /antidrop remove.");
					return;
				}	
			}
			if((ConfigHandler.usePerms && player.hasPermission("adp.lockslot")) || !ConfigHandler.usePerms) {
				//SlotLock check
				if(runSlotCheck(player, player.getInventory().getHeldItemSlot())) {
					event.setCancelled(true);
					sendCooldownMessage(player, ADMain.prefix + ChatColor.BLUE + "Slot is locked! If you want to unlock it, do /antidrop lockslot and click on the slot you want to unlock.");
					return;
				}		
			}
		}
	}
	
	//checks run in async?
	private boolean filterCheckStart(Player player, ItemStack item) {
		final List<Boolean> ret = new ArrayList<Boolean>(Arrays.asList(false));
		ExecutorService executor = Executors.newFixedThreadPool(1);
		executor.execute(new Runnable() {
            @Override
            public void run() {
            	if(GUIHandler.filterParam.containsKey(player.getUniqueId())) {
        			for(int i = 0; i < GUIHandler.filterParam.get(player.getUniqueId()).size(); i++) {
        				List<Entry<String, Object>> filter = GUIHandler.filterParam.get(player.getUniqueId()).get(i);
        				boolean success = runFilterCheck(filter, item);
        				if(success) {
        					ret.set(0, true);
        				}
        			}
        		}
            }
        });
	    executor.shutdown();
	    try {
	        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
	    } 
	    catch (InterruptedException e) {
	        e.printStackTrace();
	    }
	    return ret.get(0);
	}
	private boolean runFilterCheck(List<Entry<String, Object>> filter, ItemStack item) {
		for(int i = 0; i < filter.size(); i++) {
			String checkType = filter.get(i).getKey();
			Object value = filter.get(i).getValue();
			if(checkType.equals("Exact")) {
		        YamlConfiguration check = new YamlConfiguration(); 
		        YamlConfiguration toCheck = new YamlConfiguration();
		        try {
		        	check.loadFromString((String) value);
		        } catch (InvalidConfigurationException e) {
		        	e.printStackTrace(); return false;
		        }
		        ItemStack itoCheck = item.clone(); itoCheck.setAmount(1);
		        toCheck.set("i", itoCheck);
				if(!toCheck.saveToString().equals(check.saveToString())){ return false; }
			} else {
				if(checkType.equals("Name")) {
					if(!(item.getItemMeta().hasDisplayName() && item.getItemMeta().getDisplayName().equals(value))) return false;
				}
				if(checkType.equals("Lore")) {
					if(!(item.getItemMeta().hasLore() && item.getItemMeta().getLore().equals(value))) return false;
				}
				if(checkType.equals("Type")) {
					if(!item.getType().equals(value)) return false;
				}
				if(checkType.equals("Damage")) {
					if(item.getDurability() != (short) value) return false;
				}
			}
		}
		return true;
	}
	
	private boolean runSlotCheck(Player player, int slot) {
		//SlotLock check
		if(GUIHandler.slotParam.containsKey(player.getUniqueId())) {
			if(GUIHandler.slotParam.containsKey(player.getUniqueId()) && !GUIHandler.slotParam.get(player.getUniqueId()).isEmpty()) {
				List<Integer> slotlist = GUIHandler.slotParam.get(player.getUniqueId());
				for(int i = 0; i < slotlist.size(); i++) {
					if(slotlist.get(i) == slot) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	//TODO check if works, update spigot
	
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		System.out.println(event.getKeepInventory());
		if(ConfigHandler.deathKeepEnable && !event.getKeepInventory()) {
			Player player = event.getEntity();
			
			if((ConfigHandler.usePerms && player.hasPermission("adp.death.keepfilter")) || (!ConfigHandler.usePerms && player.isOp())) {
				System.out.println("deathfil1");
				if(GUIHandler.filterParam.containsKey(player.getUniqueId()) && !GUIHandler.filterParam.get(player.getUniqueId()).isEmpty()) {
					System.out.println("deathfil2");
					List<ItemStack> items = event.getDrops();
					ExecutorService executor = Executors.newFixedThreadPool(1);
					executor.execute(new Runnable() {
						@Override
					    public void run() {
							System.out.println("deathfil3");
							//HashMap<Index, times> 
							HashMap<String, Object> invVal = new HashMap<String, Object>();
							List<String> name = new ArrayList<String>();
							List<List<String>> lore = new ArrayList<List<String>>();
							List<Short> damage = new ArrayList<Short>();
							List<Material> type = new ArrayList<Material>();
							for(int i = 0; i < items.size(); i++) {
								System.out.println("deathfilloop1");
								ItemStack item = items.get(i);
								name.add((item.hasItemMeta() && item.getItemMeta().hasDisplayName()) ? item.getItemMeta().getDisplayName() : null);
								lore.add((item.hasItemMeta() && item.getItemMeta().hasLore()) ? item.getItemMeta().getLore() : null);
								damage.add(item.getDurability());
								type.add(item.getType());
							}
							invVal.put("Name", name); invVal.put("Lore", lore); invVal.put("Damage", damage); invVal.put("Type", type);
							List<List<Entry<String, Object>>> filters = GUIHandler.filterParam.get(player.getUniqueId());
							for(int i = 0; i < filters.size(); i++) {
								System.out.println("deathfilloop2:1");
								HashMap<Integer, Integer> times = new HashMap<Integer, Integer>();
								List<Entry<String, Object>> filter = filters.get(i);	
								for(int i2 = 0; i2 < filter.size(); i2++) {
									System.out.println("deathfilloop2:2");
									Entry<String, Object> check = filter.get(i2);
									String checkType = check.getKey();
									if(checkType.equals("Exact")) {
										System.out.println("deathfiltodo");
									} else {
										System.out.println("deathfilcheck");
										@SuppressWarnings("unchecked")
										List<Object> list = new ArrayList<Object>((List<Object>) invVal.get(checkType));  //ClassCastException?
										Object val = check.getValue();
										if(list.contains(val)) {
											int index = list.indexOf(val);
											for(int i3 = 0; index >= 0; i3++) {
												times.put(index + i3, times.containsKey(index) ? times.get(index) + 1 : 1);
												System.out.println("Index: " + (index + i3));
												list.remove(index);
												index = list.indexOf(val);
											}
										}
									}
								}
								if(times.values().contains(filter.size())) {
									System.out.println("deathfil4");
									List<Integer> cloneval = new ArrayList<Integer>(times.values());
									List<Integer> clonekey = new ArrayList<Integer>(times.keySet());
									int mapIndex = cloneval.indexOf(filter.size());
									for(int i2 = 0; mapIndex >= 0; i2++) {
										System.out.println("MapIndex:" + mapIndex + i2);
										System.out.println("Index: " + clonekey.get(mapIndex + i2));
										ItemStack item = items.get(clonekey.get(mapIndex + i2));  //check if it is key actually corresponds to value
										System.out.println(item);
										int slot = player.getInventory().first(item);
										Entry<Integer, ItemStack> entry = new AbstractMap.SimpleEntry<Integer, ItemStack>(slot, item);
										player.getInventory().clear(slot);  //check if it will still appear as a drop or not
										items.remove(item);
										if(toBeKeeped.containsKey(player.getUniqueId())) {
											toBeKeeped.get(player.getUniqueId()).add(entry);	
										} else {
											toBeKeeped.put(player.getUniqueId(), new ArrayList<Entry<Integer, ItemStack>>(Arrays.asList(entry)));
										}	
										cloneval.remove(mapIndex);
										mapIndex = cloneval.indexOf(filter.size());
									}
								}
							}
						}
					});
					executor.shutdown();
				    try {
				        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
				    } 
				    catch (InterruptedException e) {
				        e.printStackTrace();
				    }
				}
			}
			if((ConfigHandler.usePerms && player.hasPermission("adp.death.keepslot")) || (!ConfigHandler.usePerms && player.isOp())) {
				if(GUIHandler.slotParam.containsKey(player.getUniqueId()) && !GUIHandler.slotParam.get(player.getUniqueId()).isEmpty()) {
					List<ItemStack> items = event.getDrops();
					List<Integer> slots = GUIHandler.slotParam.get(player.getUniqueId());
					for(int i = 0; i < slots.size(); i++) {
						ItemStack item = player.getInventory().getItem(slots.get(i));
						items.remove(item);
						Entry<Integer, ItemStack> entry = new AbstractMap.SimpleEntry<Integer, ItemStack>(slots.get(i), item);
						if(toBeKeeped.containsKey(player.getUniqueId())) {
							toBeKeeped.get(player.getUniqueId()).add(entry);	
						} else {
							toBeKeeped.put(player.getUniqueId(), new ArrayList<Entry<Integer, ItemStack>>(Arrays.asList(entry)));
						}
					}	
				}
			}
		}
	}
	
	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		if(toBeKeeped.containsKey(player.getUniqueId()) && !toBeKeeped.get(player.getUniqueId()).isEmpty()) {
			List<Entry<Integer, ItemStack>> playerKeep = toBeKeeped.get(player.getUniqueId());
			for(int i = 0; i < playerKeep.size(); i++) {
				Entry<Integer, ItemStack> entry = playerKeep.get(i);
				player.getInventory().setItem(entry.getKey(), entry.getValue());
			}
		}
	}
	
	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		Inventory inv = event.getInventory();
		Player player = (Player) event.getPlayer();
		String sslotLock = ChatColor.GREEN + "Antidrop lock slot";
		if(inv.getName().equals(sslotLock)) {  //SlotLock GUI
			if(GUIHandler.slotParam.containsKey(player.getUniqueId()) && !GUIHandler.slotParam.get(player.getUniqueId()).isEmpty()) {
				if(ConfigHandler.eco && ConfigHandler.slotFee > 0) {
					if(!((ConfigHandler.usePerms && event.getPlayer().hasPermission("adp.bypass.payment")) || (!ConfigHandler.usePerms && event.getPlayer().isOp()))) {
						List<Integer> slots = GUIHandler.slotParam.get(player.getUniqueId());
						double money = 0;
						if(slotsBuffer.containsKey(player.getUniqueId())) {
							money = ConfigHandler.slotFee * (slots.size() - slotsBuffer.get(player.getUniqueId()).size());
						} else {
							money = ConfigHandler.slotFee * slots.size();
						}
						if(ADMain.getMoney(player) >= money) {
							if(money > 0) {
								ADMain.econ.withdrawPlayer(player, money);
								player.sendMessage(ADMain.prefix + ChatColor.YELLOW + "$" + money + ChatColor.GREEN + " has been taken from your account.");	
							}
						} else {
							if(slotsBuffer.containsKey(player.getUniqueId())) {
								GUIHandler.slotParam.put(player.getUniqueId(), slotsBuffer.get(player.getUniqueId()));	
							} else {
								GUIHandler.slotParam.remove(player.getUniqueId());
							}
							player.sendMessage(ADMain.prefix + ChatColor.RED + "You do not have enough money to perform the slot locks.");
						}	
					}
					if(slotsBuffer.containsKey(player.getUniqueId())) {
						slotsBuffer.remove(player.getUniqueId());
					}
				}
				plugin.serialize(GUIHandler.slotParam, "SlotData.bin");
			}
		}
	}
	
	@EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
		if(event.getWhoClicked().getGameMode() != GameMode.CREATIVE) {
			if(event.getClickedInventory() != null) {
				if(event.getClickedInventory().equals(((Player) event.getWhoClicked()).getInventory())) {
					Player player = (Player) event.getWhoClicked();
					if((ConfigHandler.usePerms && player.hasPermission("adp.filter")) || !ConfigHandler.usePerms) {
						//Filter check
						if(ConfigHandler.clickCheck.contains(event.getClick())) { //number key?
							boolean cancel = filterCheckStart(player, event.getCurrentItem());
							if(cancel) {
								event.setCancelled(true);
								sendCooldownMessage(player, ADMain.prefix + ChatColor.BLUE + "Item is in the filter list! You will not be able to remove the items in the filtered list from inventory until you remove it with /antidrop remove.");
								return;
							}
						}						
					}
					if((ConfigHandler.usePerms && player.hasPermission("adp.lockslot")) || !ConfigHandler.usePerms) {
						//SlotLock check
						if(runSlotCheck(player, event.getSlot())) {
							event.setCancelled(true);
							sendCooldownMessage(player, ADMain.prefix + ChatColor.BLUE + "Slot is locked! If you want to unlock it, do /antidrop lockslot and click on the slot you want to unlock.");
							return;
						}
					}
				}
				
				Inventory inv = event.getInventory();
				String sslotLock = ChatColor.GREEN + "Antidrop lock slot";
				if(inv.getName().equals(sslotLock)) {  //SlotLock GUI
					if(event.getClickedInventory().getName().equals(sslotLock)) {
						Player player = (Player) event.getWhoClicked();
						if(event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
							int slot = event.getSlot();
							if(slot > 35) { slot -= 36; } else slot += 9;
							if(event.getCurrentItem().equals(GUIHandler.locked)) {
								event.getInventory().setItem(event.getSlot(), player.getInventory().getItem(slot));
								GUIHandler.slotParam.get(player.getUniqueId()).remove((Object) slot);
							} else {
								boolean bypassmax = (ConfigHandler.usePerms && player.hasPermission("adp.bypass.slotmax")) || (!ConfigHandler.usePerms && player.isOp());
								if(GUIHandler.slotParam.containsKey(player.getUniqueId()) && !GUIHandler.slotParam.get(player.getUniqueId()).isEmpty()) {
									if(bypassmax || !(GUIHandler.slotParam.get(player.getUniqueId()).size() >= ConfigHandler.slotMax)) {
										event.getInventory().setItem(event.getSlot(), GUIHandler.locked);
										if(!GUIHandler.slotParam.get(player.getUniqueId()).contains(slot)) {
											GUIHandler.slotParam.get(player.getUniqueId()).add(slot);	
										}	
									} else {
										player.sendMessage(ADMain.prefix + ChatColor.RED + "You have reached the slot limit of " + ConfigHandler.slotMax + ".");
									}
								} else {
									event.getInventory().setItem(event.getSlot(), GUIHandler.locked);
									GUIHandler.slotParam.put(player.getUniqueId(), new ArrayList<Integer>(Arrays.asList(slot)));
								}
							}
						}	
					}
					event.setCancelled(true);
				}
				
				String sfilter = ChatColor.BLUE + "AntiDrop Filter";
				if(inv.getName().equals(sfilter)) {  //Filter GUI
					if(event.getClickedInventory().getName().equals(sfilter)) {
						if(event.getCurrentItem() != null) {
							ItemStack currentItem = event.getCurrentItem();
							for(Map.Entry<String, ItemStack> entry : GUIHandler.checks.entrySet()) {
								if(currentItem.equals(entry.getValue())) {
									inv.setItem(event.getSlot(), GUIHandler.checkOffs.get(entry.getKey()));
								} else if(currentItem.equals(GUIHandler.checkOffs.get(entry.getKey()))) {
									inv.setItem(event.getSlot(), entry.getValue());
								}
							}
							
							if(event.getCurrentItem().equals(GUIHandler.submit)) {
								Player player = (Player)event.getWhoClicked();
								ItemStack main = inv.getItem(13);
								List<Entry<String, Object>> filter = new ArrayList<Entry<String, Object>>();
								if(inv.contains(GUIHandler.checks.get("Exact"))) {
									YamlConfiguration config = new YamlConfiguration();
									ItemStack itemFilt = main.clone(); itemFilt.setAmount(1);
							        config.set("i", itemFilt);
							        Entry<String, Object> entry = new AbstractMap.SimpleEntry<String, Object>("Exact", config.saveToString());
							        filter.add(entry);
								} else {
									if(inv.contains(GUIHandler.checks.get("Name"))) {
										if(main.getItemMeta().hasDisplayName()) {
											Entry<String, Object> entry = new AbstractMap.SimpleEntry<String, Object>("Name", main.getItemMeta().getDisplayName());
											filter.add(entry);
										} else {
											player.sendMessage(ADMain.prefix + ChatColor.RED + "Chosen item has no name. Skipping name check...");
										}
									}
									if(inv.contains(GUIHandler.checks.get("Lore"))) {
										if(main.getItemMeta().hasLore()) {
											Entry<String, Object> entry = new AbstractMap.SimpleEntry<String, Object>("Lore", main.getItemMeta().getLore());
											filter.add(entry);
										} else {
											player.sendMessage(ADMain.prefix + ChatColor.RED + "Chosen item has no lore. Skipping lore check...");
										}
									}
									if(inv.contains(GUIHandler.checks.get("Type"))) {
										Entry<String, Object> entry = new AbstractMap.SimpleEntry<String, Object>("Type", main.getType());
										filter.add(entry);
									}
									if(inv.contains(GUIHandler.checks.get("Damage"))) {
										Entry<String, Object> entry = new AbstractMap.SimpleEntry<String, Object>("Damage", main.getDurability());
										filter.add(entry);
									}
								}
								if(!filter.isEmpty()) {
									player.closeInventory();
									boolean useEco = false;
									if(ConfigHandler.eco && ConfigHandler.filterFee > 0) {
										if(!((ConfigHandler.usePerms && player.hasPermission("adp.bypass.payment")) || (!ConfigHandler.usePerms && player.isOp()))) {
											if(ADMain.getMoney(player) >= ConfigHandler.filterFee) {
												useEco = true;
											} else {
												player.sendMessage(ADMain.prefix + ChatColor.RED + "You have insufficient funds.");
												event.setCancelled(true);
												return;
											}
										}
									}
									boolean success = false;
									if(GUIHandler.filterParam.containsKey(player.getUniqueId()) && !GUIHandler.filterParam.get(player.getUniqueId()).isEmpty()) {
										if(GUIHandler.filterParam.get(player.getUniqueId()).contains(filter)) {
											player.sendMessage(ADMain.prefix + ChatColor.RED + "This filter already exists.");
										} else {
											GUIHandler.filterParam.get(player.getUniqueId()).add(filter);
											success = true;
										}	
									} else {
										List<List<Entry<String, Object>>> filterList = new ArrayList<List<Entry<String, Object>>>(Arrays.asList(filter));
										GUIHandler.filterParam.put(player.getUniqueId(), filterList);
										success = true;
									}	
									if(success) {
										plugin.serialize(GUIHandler.filterParam, "FilterData.bin");
										if(useEco) {
											ADMain.econ.withdrawPlayer(player, ConfigHandler.filterFee);
											player.sendMessage(ADMain.prefix + ChatColor.YELLOW + "$" + ConfigHandler.filterFee + ChatColor.GREEN + " has been taken from your account.");
										}
										player.sendMessage(ADMain.prefix + ChatColor.GREEN + "Successfully added the filter!");
									}
								} else {
									player.sendMessage(ADMain.prefix + "Please choose at least one check first!");
								}
							}
						}	
					}
					event.setCancelled(true);
				}
			}
	    }
	}	
}
