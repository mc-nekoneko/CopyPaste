package net.nekonekoserver.cbf;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CommandBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Nekoneko
 */
public final class CopyPaste extends JavaPlugin implements Listener {

    private final Gson GSON = new Gson();
    private final Type TYPE = new TypeToken<HashMap<Integer, String>>() {
    }.getType();
    private File FILE;

    private final Map<Integer, String> commands = new HashMap<>();

    private final String READ_MESSAGE = "[CP] Copy [ %s ]";
    private final String WRITE_MESSAGE = "[CP] Paste [ %s ]";

    @Override
    public void onLoad() {
        this.FILE = new File(getDataFolder(), "config.json");
    }

    @Override
    public void onEnable() {
        load();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        save();
    }

    private void load() {
        checkOrCreate();
        try {
            commands.clear();
            Map<Integer, String> loaded = GsonUtil.load(GSON, FILE, TYPE);
            if (loaded != null) {
                commands.putAll(loaded);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void save() {
        checkOrCreate();
        try {
            GsonUtil.save(GSON, FILE, commands, TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean checkOrCreate() {
        try {
            if (!this.FILE.getParentFile().exists() && !this.FILE.getParentFile().mkdirs()) {
                return false;
            }
            if (!this.FILE.exists()) {
                if (!this.FILE.createNewFile()) {
                    return false;
                }

                save();
            }

            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void playerInteract(PlayerInteractEvent event) {
        if (!event.hasItem()) {
            return;
        }

        Player player = event.getPlayer();
        Block clicked = event.getClickedBlock();
        if (clicked.getType() != Material.COMMAND && clicked.getType() != Material.COMMAND_CHAIN && clicked.getType() != Material.COMMAND_REPEATING) {
            return;
        }
        if (!player.hasPermission("cp.use") || !hasTool(player)) {
            return;
        }
        if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            read(player, clicked);
        } else if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            write(player, clicked);
        }
        event.setCancelled(true);
    }

    private boolean hasTool(Player player) {
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        return mainHandItem != null && mainHandItem.getType().equals(Material.PAPER);
    }

    private void read(Player player, Block block) {
        CommandBlock commandBlock = (CommandBlock) block.getState();
        if (commandBlock.getCommand().length() <= 0) {
            return;
        }

        String command = commandBlock.getCommand();
        ItemStack item = player.getInventory().getItemInMainHand();
        setDisplayName(item, command);
        commands.put(item.hashCode(), command);
        player.sendMessage(String.format(READ_MESSAGE, command));
    }

    private void write(Player player, Block block) {
        CommandBlock commandBlock = (CommandBlock) block.getState();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!commands.containsKey(item.hashCode())) {
            return;
        }

        String command = commands.get(item.hashCode());
        commandBlock.setCommand(command);
        commandBlock.update();
        player.sendMessage(String.format(WRITE_MESSAGE, command));
    }

    private void setDisplayName(ItemStack item, String name) {
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
    }
}
