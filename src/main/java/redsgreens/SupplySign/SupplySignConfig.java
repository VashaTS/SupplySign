package redsgreens.SupplySign;

import org.bukkit.configuration.file.FileConfiguration;

public class SupplySignConfig {

	private final SupplySign Plugin;

	public boolean ShowErrorsInClient = false;
	public boolean AllowNonOpAccess = false;
	public SupplySignOnSign FixSignOnSignGlitch = SupplySignOnSign.Disabled;

	public SupplySignConfig(final SupplySign plugin)
	{
		Plugin = plugin;
	}

	// load config settings from config.yml (created from the copy in the jar when missing)
	public void loadConfig()
	{
		Plugin.saveDefaultConfig();
		Plugin.reloadConfig();
		FileConfiguration cfg = Plugin.getConfig();

		ShowErrorsInClient = cfg.getBoolean("ShowErrorsInClient", false);
		AllowNonOpAccess = cfg.getBoolean("AllowNonOpAccess", false);

		FixSignOnSignGlitch = SupplySignOnSign.Disabled;
		String configStr = cfg.getString("FixSignOnSignGlitch", "Disabled");
		if(configStr.equalsIgnoreCase("SupplySignOnly"))
			FixSignOnSignGlitch = SupplySignOnSign.SupplySignOnly;
		else if(configStr.equalsIgnoreCase("Global"))
			FixSignOnSignGlitch = SupplySignOnSign.Global;

		Plugin.getLogger().info("ShowErrorsInClient=" + ShowErrorsInClient
				+ ", FixSignOnSignGlitch=" + FixSignOnSignGlitch
				+ ", AllowNonOpAccess=" + AllowNonOpAccess);
	}

}
