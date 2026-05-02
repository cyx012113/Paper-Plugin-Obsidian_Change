package com.script.obsidian_change;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

public class obsidian_change extends JavaPlugin implements Listener, TabCompleter {

    private static obsidian_change instance;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, ConvertingPlayer> convertingPlayers = new HashMap<>();

    private int conversionTicks;
    private int cooldownSeconds;
    private Material resultMaterial;
    private Map<String, String> messages;

    private static class ConvertingPlayer {
        final Block targetBlock;
        final long startTime;
        ConvertingPlayer(Block targetBlock, long startTime) {
            this.targetBlock = targetBlock;
            this.startTime = startTime;
        }
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        loadConfig();

        Bukkit.getPluginManager().registerEvents(this, this);

        if (getCommand("obsidian") != null) {
            getCommand("obsidian").setExecutor(new ObsidianCommand(this));
            getCommand("obsidian").setTabCompleter(this);
        }

        getLogger().info("Obsidian Change Powder plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Obsidian Change Powder plugin disabled.");
    }

    private void loadConfig() {
        reloadConfig();
        FileConfiguration cfg = getConfig();

        conversionTicks = cfg.getInt("conversion-time-ticks", 60);
        cooldownSeconds = cfg.getInt("cooldown-seconds", 5);
        String matName = cfg.getString("result-block", "OAK_PLANKS");
        resultMaterial = Material.getMaterial(matName);
        if (resultMaterial == null) {
            getLogger().warning("Invalid result-block material: " + matName + ", using OAK_PLANKS");
            resultMaterial = Material.OAK_PLANKS;
        }

        messages = new HashMap<>();
        ConfigurationSection msgSec = cfg.getConfigurationSection("messages");
        if (msgSec != null) {
            for (String key : msgSec.getKeys(false)) {
                messages.put(key, ChatColor.translateAlternateColorCodes('&', msgSec.getString(key)));
            }
        }
        applyDefaultMessages();
    }

    private void applyDefaultMessages() {
        setDefault("must-look-obsidian", "&cYou must look at obsidian to use!");
        setDefault("cooldown", "&cSkill on cooldown, remaining {seconds} seconds");
        setDefault("already-converting", "&cYou already have an ongoing conversion!");
        setDefault("start-conversion", "&eStarting obsidian conversion, please wait {seconds} seconds...");
        setDefault("success", "&aObsidian successfully converted to {result}!");
        setDefault("failed", "&cConversion failed: target block has been changed or destroyed.");
        setDefault("usage-give", "&cUsage: /obsidian give [player] obsidian_change_powder [amount]");
        setDefault("unknown-item", "&cUnknown item: {item}");
        setDefault("player-not-online", "&cPlayer {player} is not online!");
        setDefault("given", "&aGiven {player} {amount} Obsidian Change Powder");
        setDefault("received", "&aYou have received {amount} Obsidian Change Powder");
        setDefault("console-must-specify", "&cConsole must specify player name: /obsidian give <player> obsidian_change_powder [amount]");
        setDefault("amount-integer", "&cAmount must be an integer!");
    }

    private void setDefault(String key, String defValue) {
        if (!messages.containsKey(key)) {
            messages.put(key, ChatColor.translateAlternateColorCodes('&', defValue));
        }
    }

    private String getMsg(String key, String... placeholders) {
        String msg = messages.getOrDefault(key, key);
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            msg = msg.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        }
        return msg;
    }

    public static ItemStack createObsidianChangePowder() {
        ItemStack item = new ItemStack(Material.REDSTONE, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Obsidian Change Powder");
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isObsidianChangePowder(ItemStack item) {
        if (item == null || item.getType() != Material.REDSTONE || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() && meta.getDisplayName().equals(ChatColor.LIGHT_PURPLE + "Obsidian Change Powder");
    }

    private void consumeOnePowder(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isObsidianChangePowder(item)) {
                item.setAmount(item.getAmount() - 1);
                if (item.getAmount() <= 0) player.getInventory().remove(item);
                break;
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (!isObsidianChangePowder(item)) return;
        event.setCancelled(true);

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType() != Material.OBSIDIAN) {
            player.sendMessage(getMsg("must-look-obsidian"));
            return;
        }

        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (cooldowns.containsKey(playerId)) {
            long timeLeft = (cooldowns.get(playerId) - now) / 1000;
            if (timeLeft > 0) {
                player.sendMessage(getMsg("cooldown", "seconds", String.valueOf(timeLeft)));
                return;
            }
        }
        if (convertingPlayers.containsKey(playerId)) {
            player.sendMessage(getMsg("already-converting"));
            return;
        }

        consumeOnePowder(player);
        int seconds = conversionTicks / 20;
        player.sendMessage(getMsg("start-conversion", "seconds", String.valueOf(seconds)));

        convertingPlayers.put(playerId, new ConvertingPlayer(clickedBlock, now));
        new BukkitRunnable() {
            @Override
            public void run() {
                ConvertingPlayer cp = convertingPlayers.remove(playerId);
                if (cp == null) return;
                Player p = Bukkit.getPlayer(playerId);
                if (p == null || !p.isOnline()) return;
                Block block = cp.targetBlock;
                if (block.getType() == Material.OBSIDIAN) {
                    block.setType(resultMaterial);
                    p.sendMessage(getMsg("success", "result", resultMaterial.name().toLowerCase().replace("_", " ")));
                } else {
                    p.sendMessage(getMsg("failed"));
                }
                cooldowns.put(playerId, now + cooldownSeconds * 1000L);
            }
        }.runTaskLater(this, conversionTicks);
    }

    private static class ObsidianCommand implements org.bukkit.command.CommandExecutor {
        private final obsidian_change plugin;
        ObsidianCommand(obsidian_change plugin) { this.plugin = plugin; }
        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (args.length < 2 || !args[0].equalsIgnoreCase("give")) {
                if (sender instanceof Player) sender.sendMessage(plugin.getMsg("usage-give"));
                else sender.sendMessage(plugin.getMsg("console-must-specify"));
                return true;
            }
            String targetName, itemType;
            int amount = 1;
            if (args.length >= 3) {
                targetName = args[1];
                itemType = args[2];
                if (args.length >= 4) {
                    try { amount = Integer.parseInt(args[3]); }
                    catch (NumberFormatException e) { sender.sendMessage(plugin.getMsg("amount-integer")); return true; }
                }
            } else {
                if (!(sender instanceof Player)) { sender.sendMessage(plugin.getMsg("console-must-specify")); return true; }
                targetName = sender.getName();
                itemType = args[1];
                if (args.length >= 3) {
                    try { amount = Integer.parseInt(args[2]); }
                    catch (NumberFormatException e) { sender.sendMessage(plugin.getMsg("amount-integer")); return true; }
                }
            }
            if (!itemType.equalsIgnoreCase("obsidian_change_powder")) {
                sender.sendMessage(plugin.getMsg("unknown-item", "item", itemType));
                return true;
            }
            Player target = Bukkit.getPlayer(targetName);
            if (target == null) {
                sender.sendMessage(plugin.getMsg("player-not-online", "player", targetName));
                return true;
            }
            ItemStack item = createObsidianChangePowder();
            item.setAmount(Math.min(amount, 64));
            target.getInventory().addItem(item);
            String givenMsg = plugin.getMsg("given", "player", targetName, "amount", String.valueOf(amount));
            sender.sendMessage(givenMsg);
            if (!sender.equals(target)) {
                target.sendMessage(plugin.getMsg("received", "amount", String.valueOf(amount)));
            }
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("obsidian")) {
            if (args.length == 1) {
                return filter(Arrays.asList("give"), args[0]);
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
                List<String> playerNames = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                return filter(playerNames, args[1]);
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
                return filter(Collections.singletonList("obsidian_change_powder"), args[2]);
            }
            if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
                return filter(Arrays.asList("1", "8", "16", "32", "64"), args[3]);
            }
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String token) {
        if (token.isEmpty()) return list;
        return list.stream().filter(s -> s.toLowerCase().startsWith(token.toLowerCase())).collect(Collectors.toList());
    }
}