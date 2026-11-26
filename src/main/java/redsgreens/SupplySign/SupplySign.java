package redsgreens.SupplySign;

import java.util.ArrayList;
import java.util.List;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;

/**
 * SupplySign for Paper
 *
 * @author redsgreens (original plugin), VashaTS (Paper 26.x port)
 */
public class SupplySign extends JavaPlugin implements TabExecutor {
	private final SupplySignBlockListener blockListener = new SupplySignBlockListener(this);
	private final SupplySignPlayerListener playerListener = new SupplySignPlayerListener(this);

	public final SupplySignConfig Config = new SupplySignConfig(this);
	public final SupplySignKits Kits = new SupplySignKits(this);
	public final SupplySignItems Items = new SupplySignItems(this);

	@Override
	public void onEnable() {
		try {
			// create the data folder if it doesn't exist
			getDataFolder().mkdirs();

			Config.loadConfig();
			Items.loadItems();
			Kits.loadKits();
		} catch (Exception e) {
			getLogger().severe("Error loading data: " + e);
			e.printStackTrace();
		}

		// Register our events
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(blockListener, this);
		pm.registerEvents(playerListener, this);

		getCommand("supplysign").setExecutor(this);
		getCommand("supplysign").setTabCompleter(this);

		getLogger().info("SupplySign version " + getPluginMeta().getVersion() + " is enabled!");
	}

	@Override
	public void onDisable() {
		getLogger().info("SupplySign version " + getPluginMeta().getVersion() + " is disabled!");
	}

	// send an error message to a player if the config says so
	public void sendError(Player player, String message) {
		if(Config.ShowErrorsInClient)
			player.sendMessage(Component.text("Err: " + message, NamedTextColor.RED));
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
	// catches the /supplysign command
	{
		if(!cmd.getName().equalsIgnoreCase("supplysign") || args.length != 1)
			return false;

		if(args[0].equalsIgnoreCase("reload"))
		{
			if(isAuthorized(sender, "reload"))
			{
				try {
					Config.loadConfig();
					Items.loadItems();
					Kits.loadKits();
				} catch (Exception e) {
					getLogger().warning("SupplySign error: " + e.getMessage());
				}

				sender.sendMessage(Component.text("SupplySign data reloaded."));
			}
			else
				sender.sendMessage(Component.text("Err: You don't have SupplySign reload permission.", NamedTextColor.RED));
			return true;
		}
		else if(args[0].equalsIgnoreCase("listkits"))
		{
			sender.sendMessage(Component.text("Available SupplySign kits:"));
			for(String kit : Kits.getKitNames())
				sender.sendMessage(Component.text(kit));
			return true;
		}

		return false;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
		List<String> result = new ArrayList<String>();
		if(args.length == 1)
			for(String option : new String[]{"reload", "listkits"})
				if(option.startsWith(args[0].toLowerCase()))
					result.add(option);
		return result;
	}

	// return true if the sender has the permission perm
	public boolean isAuthorized(CommandSender sender, String perm){
		return sender.isOp() || (Config.AllowNonOpAccess && perm.equalsIgnoreCase("access")) || sender.hasPermission("supplysign." + perm);
	}
}
