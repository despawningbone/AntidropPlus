package me.despawningbone.antidrop;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class ADCommandMain implements CommandExecutor {
	
	private ADMain plugin;
	
	public ADCommandMain(ADMain instance) {
		plugin = instance;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if(args.length > 0 && args[0].equalsIgnoreCase("reload")) {
			if((ConfigHandler.usePerms && sender.hasPermission("adp.reload")) || (!ConfigHandler.usePerms && sender.isOp())) {
				if(ConfigHandler.getConfigValues()) {
					sender.sendMessage(ADMain.prefix + ChatColor.BLUE + "Antidrop+ has been reloaded.");	
				} else {
					sender.sendMessage(ADMain.prefix + ChatColor.DARK_RED + "There is a missing dependency. Disabled Antidrop+.");
				}
			} else { 
					sender.sendMessage(ADMain.prefix + ChatColor.RED + "You do not have permission.");
			}
			return true;
		}
		if (sender instanceof Player) {
			Player player = (Player) sender; 
			if (args.length <= 0){
				sender.sendMessage(ADMain.prefix + ChatColor.RED + "Unknown arguments. Do /antidrop help for more info.");
			} else if(args[0].equalsIgnoreCase("lockslot")) {
				if((ConfigHandler.usePerms && player.hasPermission("adp.lockslot")) || !ConfigHandler.usePerms) {
					if(ConfigHandler.eco && GUIHandler.slotParam.containsKey(player.getUniqueId()) && !GUIHandler.slotParam.get(player.getUniqueId()).isEmpty()) {
						List<Integer> buffer = new ArrayList<Integer>(GUIHandler.slotParam.get(player.getUniqueId()));
						ADListener.slotsBuffer.put(player.getUniqueId(), buffer);
					}
					boolean bypassmax = (ConfigHandler.usePerms && sender.hasPermission("adp.bypass.slotmax")) || (!ConfigHandler.usePerms && sender.isOp());
					if(!bypassmax && GUIHandler.slotParam.containsKey(player.getUniqueId()) && GUIHandler.slotParam.get(player.getUniqueId()).size() >= ConfigHandler.slotMax ) {
						player.sendMessage(ADMain.prefix + ChatColor.RED + "You have reached the slot limit of " + ConfigHandler.slotMax + ".");
					} else {
						GUIHandler.OpenSlotLockGUI(player);	
					}	
				} else {
					sender.sendMessage(ADMain.prefix + ChatColor.RED + "You do not have permission.");
				}
			} else if(args[0].equalsIgnoreCase("filter")) {
				if((ConfigHandler.usePerms && player.hasPermission("adp.filter")) || !ConfigHandler.usePerms) {
					ItemStack item = player.getItemInHand();
					boolean bypass = (ConfigHandler.usePerms && sender.hasPermission("adp.bypass.filterblacklist")) || (!ConfigHandler.usePerms && sender.isOp());
					if(item.getType() != Material.AIR) {
						if(!bypass && (ConfigHandler.filterBlacklist.containsKey(item.getType()) && (ConfigHandler.filterBlacklist.get(item.getType()) == -1 || ConfigHandler.filterBlacklist.get(item.getType()) == item.getDurability()))) {
							player.sendMessage(ADMain.prefix + ChatColor.RED + "Sorry, but you cannot filter this item.");
						} else {
							boolean bypassmax = (ConfigHandler.usePerms && sender.hasPermission("adp.bypass.filtermax")) || (!ConfigHandler.usePerms && sender.isOp());
							if(!bypassmax && (GUIHandler.filterParam.containsKey(player.getUniqueId()) && GUIHandler.filterParam.get(player.getUniqueId()).size() >= ConfigHandler.filterMax)) {
								player.sendMessage(ADMain.prefix + ChatColor.RED + "You have reached the filter limit of " + ConfigHandler.filterMax + "!");
							} else {
								GUIHandler.OpenFilterGUI(item, player);	
							}	
						}
					} else {
						player.sendMessage(ADMain.prefix + ChatColor.RED + "Please hold a item to filter!");
					}
				} else { 
					sender.sendMessage(ADMain.prefix + ChatColor.RED + "You do not have permission.");
				}
			} else if(args[0].equalsIgnoreCase("filterlist")) {
				if((ConfigHandler.usePerms && player.hasPermission("adp.filter")) || !ConfigHandler.usePerms) {
					String header = ChatColor.GRAY + (ChatColor.STRIKETHROUGH + "------------------") + ChatColor.GOLD + "AntiDrop+ Filter List" + ChatColor.GRAY + ChatColor.STRIKETHROUGH + "------------------";
					String nofilter = ADMain.prefix + ChatColor.RED + "You currently have no filters set.";
					if(args.length > 1) {
						if((ConfigHandler.usePerms && player.hasPermission("adp.others.filterlist")) || (!ConfigHandler.usePerms && player.isOp())) {
							String playername = args[1];
							Player otherPlayer = Bukkit.getServer().getPlayer(playername);
							if(otherPlayer != null) {
								player = otherPlayer;
								int length = 57 - player.getName().length() - 14;
								String repeat = ChatColor.GRAY + (ChatColor.STRIKETHROUGH + (StringUtils.repeat("-", (int) Math.floor(length / 2))));
								String lastletter = (length % 2) == 0 ? "" : "-";
								header = repeat + ChatColor.GOLD + player.getName() + "'s Filter List" + repeat + lastletter;
								nofilter = nofilter.replace("You", player.getName());
							} else {
								sender.sendMessage(ADMain.prefix + ChatColor.RED + "Unknown player.");
								return true;
							}
						} else {
							sender.sendMessage(ADMain.prefix + ChatColor.RED + "You do not have permission.");
							return true;
						}
					}
					//async?
					if(GUIHandler.filterParam.containsKey(player.getUniqueId()) && !GUIHandler.filterParam.get(player.getUniqueId()).isEmpty()) {
						List<List<Entry<String, Object>>> filterlist = GUIHandler.filterParam.get(player.getUniqueId());
						sender.sendMessage(header);
						for(int i = 0; i < filterlist.size(); i++) {
							List<Entry<String, Object>> filter = filterlist.get(i);
							sender.sendMessage(ChatColor.GOLD + String.valueOf(i + 1) + ":");
							for(int i2 = 0; i2 < filter.size(); i2++) {
								Entry<String, Object> val = filter.get(i2);
								if(val.getKey().equals("Exact")) {
									String config = val.getValue().toString().replaceAll("\n", "\n\u00A73");
									Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + sender.getName() + " {\"text\":\"\u00A76  Exact: \u00A77(Hover me!)\",\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"\u00A73" + config + "\"}}");
								} else if(val.getKey().equals("Lore")){
									sender.sendMessage(ChatColor.GOLD + "  " + val.getKey() + ":");
									@SuppressWarnings("unchecked")
									List<String> lore = (List<String>) val.getValue();
									for(int i3 = 0; i3 < lore.size(); i3++) {
										sender.sendMessage(ChatColor.GOLD + "    - " + ChatColor.GRAY + lore.get(i3));
									}
								} else {
									sender.sendMessage(ChatColor.GOLD + "  " + val.getKey() + ": " + ChatColor.GRAY + val.getValue());
								}
							}
						}
						sender.sendMessage(ChatColor.GRAY + (ChatColor.STRIKETHROUGH + "-----------------------------------------------------"));
					} else {
						sender.sendMessage(nofilter);
					}
				} else {
					sender.sendMessage(ADMain.prefix + ChatColor.RED + "You do not have permission.");
				}
			} else if(args[0].equalsIgnoreCase("remove")) {
				if((ConfigHandler.usePerms && player.hasPermission("adp.filter")) || !ConfigHandler.usePerms) {
					if(GUIHandler.filterParam.containsKey(player.getUniqueId()) && !GUIHandler.filterParam.get(player.getUniqueId()).isEmpty()) {
						if(args.length > 1) {
							int filtID = 0;
							try {
								filtID = Integer.parseInt(args[1]) - 1;
							} catch (NumberFormatException e) {
								player.sendMessage(ADMain.prefix + ChatColor.RED + "Please enter a valid integer.");
								return true;
							}
							List<List<Entry<String, Object>>> filterList = GUIHandler.filterParam.get(player.getUniqueId());
							if(filtID >= 0 && filtID < filterList.size()) {
								filterList.remove(filtID);
								GUIHandler.filterParam.put(player.getUniqueId(), filterList);
								player.sendMessage(ADMain.prefix + ChatColor.YELLOW + "Successfully removed filter #" + (filtID + 1) + ".");
								plugin.serialize(GUIHandler.filterParam, "FilterData.bin");
							} else {
								player.sendMessage(ADMain.prefix + ChatColor.RED + "The integer inputted is out of range.");
							}
						} else {
							player.sendMessage(ADMain.prefix + ChatColor.RED + "Please input the filter's ID. You can see it in /antidrop filterlist.");
						}
					} else {
						player.sendMessage(ADMain.prefix + ChatColor.RED + "You currently have no filters set.");
					}
				} else {
					sender.sendMessage(ADMain.prefix + ChatColor.RED + "You do not have permission.");
				}
			} else if(args[0].equalsIgnoreCase("help")) {
				sender.sendMessage(ChatColor.GRAY + (ChatColor.STRIKETHROUGH + "--------------------") + ChatColor.GOLD + "AntiDrop+ Help" + ChatColor.GRAY + ChatColor.STRIKETHROUGH + "---------------------");
				sender.sendMessage(ChatColor.DARK_AQUA + "Alias:" + ChatColor.DARK_GRAY + " /adp, /nodrop");
				if((ConfigHandler.usePerms && player.hasPermission("adp.lockslot")) || !ConfigHandler.usePerms) {
					sender.sendMessage(ChatColor.GOLD + "/antidrop lockslot" + ChatColor.GRAY + " - " + ChatColor.BLUE + "Opens the slot lock GUI.");
				}
				if((ConfigHandler.usePerms && player.hasPermission("adp.filter")) || !ConfigHandler.usePerms) {
					sender.sendMessage(ChatColor.GOLD + "/antidrop filter" + ChatColor.GRAY + " - " + ChatColor.BLUE + "Opens the filter GUI.");
					sender.sendMessage(ChatColor.GOLD + "/antidrop filterlist" + ChatColor.GRAY + " - " + ChatColor.BLUE + "Lists the filters you currently have.");
					sender.sendMessage(ChatColor.GOLD + "/antidrop remove <number>" + ChatColor.GRAY + " - " + ChatColor.BLUE + "Removes a filter.");	
				}
				if(sender.hasPermission("adp.reload")) {
					sender.sendMessage(ChatColor.GOLD + "/antidrop reload" + ChatColor.GRAY + " - " + ChatColor.BLUE + "Reloads the config.");
				}
				sender.sendMessage(ChatColor.GOLD + "/antidrop about" + ChatColor.GRAY + " - " + ChatColor.BLUE + "Displays the about page.");
			} else if(args[0].equalsIgnoreCase("about")) {
				sender.sendMessage(ChatColor.BLUE + "Antidrop+" + ChatColor.DARK_GRAY + " Version: " + ChatColor.GOLD + plugin.getDescription().getVersion());
				sender.sendMessage(ChatColor.GOLD + "Made by " + ChatColor.DARK_BLUE + "despawningbone");
			} else {
				sender.sendMessage(ADMain.prefix + ChatColor.RED + "Unknown arguments. Do /antidrop help for more info.");
			}
		} else {
			sender.sendMessage(ChatColor.RED + "This is a player only command.");
		}
		return true;
	}
	
}