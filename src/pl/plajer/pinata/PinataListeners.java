package pl.plajer.pinata;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.shampaggon.crackshot.CSUtility;

import pl.plajer.pinata.pinataapi.PinataDeathEvent;

public class PinataListeners implements Listener {

	private Main plugin;

	public PinataListeners(Main plugin) {
		this.plugin = plugin;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		for(Entity en : Bukkit.getServer().getWorld(e.getPlayer().getWorld().getName()).getEntities()) {
			if(en instanceof Sheep) {
				if(plugin.getCommands().getPinata().containsKey(en)) {
					if(plugin.getCommands().getPinata().get(en).getPlayer().equals(e.getPlayer())) {
						plugin.getCommands().getPinata().get(en).getBuilder().getBlock().setType(Material.AIR);
						plugin.getCommands().getPinata().get(en).getLeash().remove();
						en.remove();
						plugin.getCommands().getPinata().remove(en);
						if(plugin.getCommands().getUsers().contains(e.getPlayer())) {
							plugin.getCommands().getUsers().remove(e.getPlayer());
						}
					}
				}
			}
		}
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		if(!e.getPlayer().hasPermission("pinata.admin.notify")) {
			return;
		}
		String currentVersion = "v" + Bukkit.getPluginManager().getPlugin("Pinata").getDescription().getVersion();
		if(plugin.getConfig().getBoolean("update-notify")) {
			try {
				UpdateChecker.checkUpdate(currentVersion);
				String latestVersion = UpdateChecker.getLatestVersion();
				if(latestVersion != null) {
					latestVersion = "v" + latestVersion;
					e.getPlayer().sendMessage(Utils.colorRawMessage("Other.Plugin-Up-To-Date").replaceAll("%old%", currentVersion).replaceAll("%new%", latestVersion));
				}
			} catch(Exception ex) {
				e.getPlayer().sendMessage(Utils.colorRawMessage("Other.Plugin-Update-Check-Failed").replaceAll("%error%", ex.getMessage()));
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPinataDamage(EntityDamageByEntityEvent e) {
		if(plugin.getPinataManager().getPinataList().contains(e.getEntity().getCustomName())) {
			if(plugin.getCommands().getPinata().get(e.getEntity()) != null) {
				if(plugin.getFileManager().getPinataConfig().get("pinatas." + e.getEntity().getCustomName() + ".type").equals("public")) {
					e.getEntity().getLocation().getWorld().playEffect(e.getEntity().getLocation().add(0, 1, 0), Effect.MOBSPAWNER_FLAMES, 10);
					//override World Guard blocking
					e.setCancelled(false);
				} else /* the type is private */{
					if(plugin.getCommands().getPinata().get(e.getEntity()).getPlayer().equals(e.getDamager())) {
						if(plugin.getConfig().getBoolean("halloween-mode")) {
							if(!Bukkit.getServer().getVersion().contains("1.8")) {
								e.getEntity().getWorld().playSound(e.getEntity().getLocation(), Sound.ENTITY_GHAST_HURT, 1, 1);
							}
						}
						e.getEntity().getLocation().getWorld().playEffect(e.getEntity().getLocation().add(0, 1, 0), Effect.MOBSPAWNER_FLAMES, 10);
						e.setCancelled(false);
					} else {
						e.getDamager().sendMessage(Utils.colorRawMessage("Pinata.Not-Own"));
						e.setCancelled(true);
					}
				}
			}
		}
	}

	@EventHandler
	public void onBatDamage(EntityDamageEvent e) {
		if(e.getEntityType().equals(EntityType.BAT) || (e.getEntity().getCustomName() != null && e.getEntity().getCustomName().equals(Utils.colorMessage("&6Halloween!")))) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onPinataDeath(final EntityDeathEvent e) {
		if(plugin.getCommands().getPinata().get(e.getEntity()) != null) {
			if(plugin.getCommands().getUsers().contains(plugin.getCommands().getPinata().get(e.getEntity()).getPlayer())) {
                List<Player> users = new ArrayList<>();
                users.addAll(plugin.getCommands().getUsers());
                users.remove(plugin.getCommands().getPinata().get(e.getEntity()).getPlayer());
                plugin.getCommands().setUsers(users);
				users.clear();
			}
			if(plugin.getConfig().getBoolean("halloween-mode")) {
				e.getEntity().getWorld().strikeLightningEffect(e.getEntity().getLocation());
				Random r = new Random();
				if(Bukkit.getServer().getVersion().contains("1.8")) {
					if(r.nextBoolean()) {
						e.getEntity().getWorld().playSound(e.getEntity().getLocation(), Sound.valueOf("WOLF_HOWL"), 1, 1);
					} else{
						e.getEntity().getWorld().playSound(e.getEntity().getLocation(), Sound.valueOf("WITHER_DEATH"), 1, 1);
					}
				} else {
					if(r.nextBoolean()) {
						e.getEntity().getWorld().playSound(e.getEntity().getLocation(), Sound.ENTITY_WOLF_HOWL, 1, 1);
					} else{
						e.getEntity().getWorld().playSound(e.getEntity().getLocation(), Sound.ENTITY_WITHER_DEATH, 1, 1);
					}
				}
				final ArrayList<Entity> bats = new ArrayList<>();
				for(int i = 0; i < 5; i++) {
					final Entity bat = e.getEntity().getWorld().spawnEntity(e.getEntity().getLocation(), EntityType.BAT);
					bat.setCustomName(Utils.colorMessage("&6Halloween!"));
					bats.add(bat);
				}
				Bukkit.getScheduler().runTaskLater(plugin, () -> {
					for(Entity bat : bats) {
						bat.getWorld().playEffect(bat.getLocation(), Effect.SMOKE, 3);
						bat.remove();
					}
					bats.clear();
				}, 30);
			}
			e.getEntity().setLeashHolder(null);
			e.getEntity().getLocation().getWorld().playEffect(e.getEntity().getLocation().add(0, 1, 0), Effect.POTION_BREAK, 10);
			e.getDrops().clear();
			e.setDroppedExp(0);
			plugin.getCommands().getPinata().get(e.getEntity()).getBuilder().getBlock().setType(Material.AIR);
			plugin.getCommands().getPinata().get(e.getEntity()).getLeash().remove();
			final ArrayList<Item> itemsToGive = new ArrayList<>();
			final Player p = e.getEntity() instanceof Player ? e.getEntity().getKiller() : plugin.getCommands().getPinata().get(e.getEntity()).getPlayer();
			if(plugin.getConfig().getBoolean("blindness-effect")) {
				if(p.hasPotionEffect(PotionEffectType.BLINDNESS)) {
					p.removePotionEffect(PotionEffectType.BLINDNESS);
				}
				if(plugin.getConfig().getBoolean("full-blindness-effect")) {
					if(p.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
						p.removePotionEffect(PotionEffectType.NIGHT_VISION);
					}
				}
			}
			final int timer = plugin.getFileManager().getPinataConfig().getInt("pinatas." + e.getEntity().getCustomName() + ".timer");
			final ArrayList<ItemStack[]> items = new ArrayList<>();
			int otherItemsSize = 0;
			Random rand = new Random();
			for(int i = 0; i < plugin.getPinataManager().getPinataDrop().get(e.getEntity().getCustomName()).size(); i++) {
				String drop = plugin.getPinataManager().getPinataDrop().get(e.getEntity().getCustomName()).get(i);
				final String[] parts = drop.split(";");
				int chance = parts[0].equals("item") ? Integer.parseInt(parts[4]) : Integer.parseInt(parts[3]);
				if(rand.nextInt(100) + 1 < chance) {
					if(parts[0].equals("item")) {
						String[] nameAndLore = parts[3].split("/");
						final Item item = e.getEntity().getWorld().dropItemNaturally(e.getEntity().getLocation(), new ItemStack(Material.getMaterial(parts[1].toUpperCase())));
						item.setPickupDelay(1000);
						if(plugin.getHologramsUse()) {
							final Hologram hologram = HologramsAPI.createHologram(plugin, item.getLocation().add(0.0, 1.5, 0.0));
							hologram.appendTextLine(Utils.colorMessage(nameAndLore[0].replaceAll("%player%", p.getName())) + " x" + parts[2]);
							new BukkitRunnable() {
								int ticksRun;

								@Override
								public void run() {
									ticksRun++;
									hologram.teleport(item.getLocation().add(0.0, 1.5, 0.0));
									if(ticksRun > timer * 20) {
										hologram.delete();
										item.remove();
										cancel();
									}
								}
							}.runTaskTimer(plugin, 1L, 1L);
						} else {
							Bukkit.getScheduler().runTaskLater(plugin, () -> item.remove(), timer * 20);
						}
						ItemMeta itemMeta = item.getItemStack().getItemMeta();
						itemMeta.setDisplayName(Utils.colorMessage(nameAndLore[0]));
						List<String> lore = new ArrayList<>();
						for(String s : nameAndLore) {
							String colorful = Utils.colorMessage(s);
							lore.add(colorful);
						}
						lore.remove(0);
						itemMeta.setLore(lore);
						item.getItemStack().setItemMeta(itemMeta);
						itemsToGive.add(item);
					} else { //command, money, gun
						Item item = null;
						switch(parts[0]) {
							case "command":
								item = e.getEntity().getWorld().dropItemNaturally(e.getEntity().getLocation(), new ItemStack(Material.getMaterial(plugin.getConfig().getString("command-item").toUpperCase())));
								Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[1].replaceAll("%player%", p.getName()));
								break;
							case "money":
								item = e.getEntity().getWorld().dropItemNaturally(e.getEntity().getLocation(), new ItemStack(Material.getMaterial(plugin.getConfig().getString("money-item").toUpperCase())));
								plugin.getEco().depositPlayer(Bukkit.getOfflinePlayer(p.getUniqueId()), Double.parseDouble(parts[1]));
								break;
							case "gun":
								CSUtility shot = new CSUtility();
								shot.giveWeapon(p, parts[1], 1);
								item = e.getEntity().getWorld().dropItemNaturally(e.getEntity().getLocation(), new ItemStack(Material.getMaterial(plugin.getConfig().getString("gun-item").toUpperCase())));
								break;
							default: break;
						}
						item.setPickupDelay(1000);
						final Item finalItem = item;
						if(plugin.getHologramsUse()) {
							final Hologram hologram = HologramsAPI.createHologram(plugin, item.getLocation().add(0.0, 1.5, 0.0));
							hologram.appendTextLine(Utils.colorMessage(parts[2]).replaceAll("%player%", p.getName()));
							new BukkitRunnable() {
								int ticksRun;

								@Override
								public void run() {
									ticksRun++;
									hologram.teleport(finalItem.getLocation().add(0.0, 1.5, 0.0));
									if(ticksRun > timer * 20) {
										hologram.delete();
										finalItem.remove();
										cancel();
									}
								}
							}.runTaskTimer(plugin, 1, 1);
						} else {
							Bukkit.getScheduler().runTaskLater(plugin, () -> finalItem.remove(), timer * 20);
						}
						String dropMsg = Utils.colorRawMessage("Pinata.Drop.DropMsg");
						p.sendMessage(dropMsg.replaceAll("%item%", Utils.colorMessage(parts[2])).replaceAll("%amount%", "1"));
						otherItemsSize++;
					}
				}
			}
			for(int i = 0; i < itemsToGive.size(); i++) {
				Item preItem = itemsToGive.get(i);
				p.sendMessage(Utils.colorRawMessage("Pinata.Drop.DropMsg").replaceAll("%item%", StringUtils.capitalize(preItem.getItemStack().getItemMeta().getDisplayName())).replaceAll("%amount%", String.valueOf(preItem.getItemStack().getAmount())));
				ItemStack[] temp = {(preItem.getItemStack())};
				items.add(temp);
				p.getInventory().addItem(items.get(i));
			}
			PinataDeathEvent pde = new PinataDeathEvent(e.getEntity().getKiller(), e.getEntity(), e.getEntity().getCustomName(), items);
			Bukkit.getPluginManager().callEvent(pde);
			if(items.size() == 0 && otherItemsSize == 0) {
				p.sendMessage(Utils.colorRawMessage("Pinata.Drop.No-Drops"));
			}
			plugin.getCommands().getPinata().remove(e.getEntity());
			itemsToGive.clear();
		}
	}
}
