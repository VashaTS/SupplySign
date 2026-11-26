package redsgreens.SupplySign;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.bukkit.configuration.file.YamlConfiguration;

public class SupplySignKits {

	private final SupplySign Plugin;

	private HashMap<String,ArrayList<Object>> KitsMap = new HashMap<String,ArrayList<Object>>();

	public SupplySignKits(final SupplySign plugin)
	{
		Plugin = plugin;
	}

	// load kits from kits.yml (created from the copy in the jar when missing)
	public void loadKits()
	{
		File kitsFile = new File(Plugin.getDataFolder(), "kits.yml");
		if (!kitsFile.exists())
			Plugin.saveResource("kits.yml", false);

		YamlConfiguration yaml = YamlConfiguration.loadConfiguration(kitsFile);

		HashMap<String,ArrayList<Object>> tmpMap = new HashMap<String,ArrayList<Object>>();
		for(String key : yaml.getKeys(false))
		{
			java.util.List<?> list = yaml.getList(key);
			if(list == null)
				continue;
			ArrayList<Object> items = new ArrayList<Object>();
			for(Object o : list)
				items.add(String.valueOf(o));
			tmpMap.put(key.toLowerCase(), items);
		}

		KitsMap = tmpMap;
		Plugin.getLogger().info("Loaded " + KitsMap.size() + " kits from kits.yml.");
	}

	// return a kit by name
	public ArrayList<Object> getKit(String kit) throws Exception{
		String k = SupplySignUtil.stripColorCodes(kit.toLowerCase()).trim();
		if (KitsMap.containsKey(k))
			return KitsMap.get(k);
		throw new Exception("Unknown kit name: " + k);
	}

	public Set<String> getKitNames()
	{
		return KitsMap.keySet();
	}
}
