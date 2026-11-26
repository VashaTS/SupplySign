package redsgreens.SupplySign;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.kyori.adventure.text.Component;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionType;

public class SupplySignItems {

	private static final Pattern LEGACY_ID = Pattern.compile("^(\\d+)(?:[:.](\\d+))?$");

	private final SupplySign Plugin;

	// alias name -> item (items.csv + items-custom.csv)
	private Map<String, SupplySignItemStack> ItemsMap = new HashMap<String, SupplySignItemStack>();
	// "diamondpickaxe" -> DIAMOND_PICKAXE: every item of the running Minecraft version
	private final Map<String, Material> MaterialIndex = new HashMap<String, Material>();
	// legacy numeric ids (pre-1.13): id -> damage -> material
	private final Map<Integer, Map<Integer, Material>> LegacyIds = new HashMap<Integer, Map<Integer, Material>>();

	public SupplySignItems(final SupplySign plugin)
	{
		Plugin = plugin;
	}

	// read the item tables
	public void loadItems()
	{
		try {
			loadMaterialIndex();
			loadLegacyIds();
			ItemsMap = new HashMap<String, SupplySignItemStack>();
			loadBaseItems();
			loadCustomItems();
		} catch (IOException e) {
			Plugin.getLogger().warning("Error loading items: " + e.getMessage());
		}

		Plugin.getLogger().info("Loaded " + ItemsMap.size() + " item names.");
	}

	private static String normalize(String name)
	{
		return name.toLowerCase().replace("minecraft:", "").replace("_", "").replace(" ", "").replace("-", "");
	}

	private void loadMaterialIndex()
	{
		MaterialIndex.clear();
		for(Material m : Material.values())
			if(m.isItem() && !m.isAir())
				MaterialIndex.put(normalize(m.name()), m);
	}

	private void loadLegacyIds() throws IOException
	{
		LegacyIds.clear();
		try (InputStream res = SupplySign.class.getResourceAsStream("/legacy-ids.csv"))
		{
			if(res == null) return;
			BufferedReader rx = new BufferedReader(new InputStreamReader(res, StandardCharsets.UTF_8));
			String line;
			while((line = rx.readLine()) != null)
			{
				line = line.trim();
				if(line.isEmpty() || line.startsWith("#")) continue;
				String[] parts = line.split(",");
				Material m = Material.getMaterial(parts[2].trim());
				if(m == null) continue;
				LegacyIds.computeIfAbsent(Integer.parseInt(parts[0]), k -> new HashMap<Integer, Material>()).put(Integer.parseInt(parts[1]), m);
			}
		}
	}

	// look up a pre-1.13 "id" or "id:damage" and return the modern material
	private Material legacyMaterial(int id, int damage)
	{
		Map<Integer, Material> byDamage = LegacyIds.get(id);
		if(byDamage == null) return null;
		Material m = byDamage.get(damage);
		return m != null ? m : byDamage.get(0);
	}

	private static PotionType potionType(String name)
	{
		if(name == null || name.isEmpty()) return null;
		return Registry.POTION.get(NamespacedKey.minecraft(name.toLowerCase()));
	}

	// parse one line of items.csv / items-custom.csv; returns false if the line was skipped
	private boolean parseLine(String line, boolean base)
	{
		String[] parts = line.split(",", -1);
		for(int i = 0; i < parts.length; i++)
			parts[i] = parts[i].trim();
		if(parts.length < 2) throw new IllegalArgumentException("expected name,MATERIAL");

		String itemName = parts[0].toLowerCase();
		Material material;
		int amount;
		PotionType potion = null;

		if(parts[1].matches("\\d+"))
		{
			// legacy format: name,id,damage,stacksize
			int id = Integer.parseInt(parts[1]);
			if(id <= 0) return false;
			int damage = parts.length > 2 && !parts[2].isEmpty() ? Integer.parseInt(parts[2]) : 0;
			material = legacyMaterial(id, damage);
			if(material == null) throw new IllegalArgumentException("unknown legacy item id " + id + ":" + damage);
			amount = parts.length > 3 && !parts[3].isEmpty() ? Integer.parseInt(parts[3]) : material.getMaxStackSize();
		}
		else
		{
			material = MaterialIndex.get(normalize(parts[1]));
			if(material == null)
			{
				if(base) return false; // fluids, fire etc. that are not items any more
				throw new IllegalArgumentException("unknown or non-item material " + parts[1]);
			}
			amount = parts.length > 2 && !parts[2].isEmpty() ? Integer.parseInt(parts[2]) : material.getMaxStackSize();
			if(parts.length > 3)
			{
				potion = potionType(parts[3]);
				if(potion == null && !parts[3].isEmpty())
					throw new IllegalArgumentException("unknown potion type " + parts[3]);
			}
		}

		ItemsMap.put(itemName, new SupplySignItemStack(material, amount, potion));
		return true;
	}

	private void readItemFile(File file, boolean base) throws IOException
	{
		List<String> skippedNames = new ArrayList<String>();
		List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
		for(int i = 0; i < lines.size(); i++)
		{
			String line = lines.get(i).trim();
			if(line.isEmpty() || line.startsWith("#")) continue;
			try
			{
				if(!parseLine(line, base)) skippedNames.add(line.split(",")[0]);
			}
			catch (Exception ex)
			{
				Plugin.getLogger().warning("Error parsing " + file.getName() + " on line " + (i + 1) + ". " + ex.getMessage());
			}
		}
		if(!skippedNames.isEmpty())
			Plugin.getLogger().fine("Skipped " + skippedNames.size() + " entries in " + file.getName() + " that are not items in this Minecraft version: " + skippedNames);
	}

	private void loadCustomItems() throws IOException
	{
		// create the file from the one in the jar if it doesn't exist on disk
		File itemsFile = new File(Plugin.getDataFolder(), "items-custom.csv");
		if (!itemsFile.exists())
		{
			Plugin.saveResource("items-custom.csv", false);
			return;
		}
		readItemFile(itemsFile, false);
	}

	private void loadBaseItems() throws IOException
	{
		// recreate the reference copy from the one in the jar every time
		File itemsFile = new File(Plugin.getDataFolder(), "items-base.csv");
		try (InputStream res = SupplySign.class.getResourceAsStream("/items.csv"))
		{
			if(res == null) throw new IOException("items.csv is missing from the plugin jar");
			Files.copy(res, itemsFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		readItemFile(itemsFile, true);
	}

	// return an ItemStack by name; null if the name is unknown
	public ItemStack getItem(String id)
	{
		String id2 = SupplySignUtil.stripColorCodes(id).trim().toLowerCase();

		// see if it's in the config files
		SupplySignItemStack known = ItemsMap.get(id2);
		if(known == null)
			known = ItemsMap.get(normalize(id2));
		if(known != null)
			return known.getItemStack();

		// not in the config files: any item of this Minecraft version by its name, e.g. "diamond_pickaxe" or "minecraft:diamond"
		Material m = MaterialIndex.get(normalize(id2));
		if(m != null)
			return new ItemStack(m, m.getMaxStackSize());

		// pre-1.13 numeric ids: "id" or "id:damage" (or "id.damage")
		Matcher legacy = LEGACY_ID.matcher(id2);
		if(legacy.matches())
		{
			int damage = legacy.group(2) != null ? Integer.parseInt(legacy.group(2)) : 0;
			m = legacyMaterial(Integer.parseInt(legacy.group(1)), damage);
			if(m != null && m.isItem())
				return new ItemStack(m, m.getMaxStackSize());
		}

		return null;
	}

	// arranges the items to be displayed and shows the inventory dialog
	public void showInventory(Player p, ArrayList<Object> itemList){
		ArrayList<ItemStack> items = new ArrayList<ItemStack>();
		Iterator<Object> itr = itemList.iterator();
		while(itr.hasNext())
		{
			ItemStack is = getItem(itr.next().toString());
			if(is != null)
				items.add(is);
		}

		// 36 slots = four rows of nine, the layout the arrangements below were written for
		Inventory inv = Plugin.getServer().createInventory(null, 36, Component.text("SupplySign"));

		try {
			switch(items.size()){
			case 0:
				return;
			case 1:
				for(int i=0; i<36; i++)
					inv.setItem(i, items.get(0));
				break;

			case 2:
				for(int i=0; i<18; i++)
					inv.setItem(i, items.get(0));
				for(int i=0; i<18; i++)
					inv.setItem(i+18, items.get(1));
				break;

			case 3:
					for(int i=0; i<4; i++){
						inv.setItem((i*9), items.get(0)); inv.setItem((i*9)+1, items.get(0)); inv.setItem((i*9)+2, items.get(0));
						inv.setItem((i*9)+3, items.get(1)); inv.setItem((i*9)+4, items.get(1)); inv.setItem((i*9)+5, items.get(1));
						inv.setItem((i*9)+6, items.get(2)); inv.setItem((i*9)+7, items.get(2)); inv.setItem((i*9)+8, items.get(2));
					}
				break;

			case 4:
					for(int i=0; i<4; i++)
						for(int j=0; j<9; j++)
							inv.setItem((i*9)+j, items.get(i));
				break;

			case 5:
					for(int i=0; i<4; i++)
						for(int j=0; j<7; j++)
							  inv.setItem((i*9)+j, items.get(i));
					for(int k=0; k<4; k++){
						inv.setItem((k*9)+7, items.get(4));
						inv.setItem((k*9)+8, items.get(4));
					}
				break;

			case 6:
				for(int i=0; i<2; i++){
					inv.setItem((i*9), items.get(0)); inv.setItem((i*9)+1, items.get(0)); inv.setItem((i*9)+2, items.get(0));
					inv.setItem((i*9)+3, items.get(1)); inv.setItem((i*9)+4, items.get(1)); inv.setItem((i*9)+5, items.get(1));
					inv.setItem((i*9)+6, items.get(2)); inv.setItem((i*9)+7, items.get(2)); inv.setItem((i*9)+8, items.get(2));
				}
				for(int i=2; i<4; i++){
					inv.setItem((i*9), items.get(3)); inv.setItem((i*9)+1, items.get(3)); inv.setItem((i*9)+2, items.get(3));
					inv.setItem((i*9)+3, items.get(4)); inv.setItem((i*9)+4, items.get(4)); inv.setItem((i*9)+5, items.get(4));
					inv.setItem((i*9)+6, items.get(5)); inv.setItem((i*9)+7, items.get(5)); inv.setItem((i*9)+8, items.get(5));
				}

				break;

			case 7:
					int pos1 = 0;
					for(int i=0; i<7; i++){
						int jmax;
						if(i % 2 == 0) jmax = 5;
						else jmax = 4;

						for(int j=0; j<jmax; j++)
							  inv.setItem(pos1++, items.get(i));

					}
				break;

			case 8:
					int pos2 = 0;
					for(int i=0; i<4; i++){
						  inv.setItem(pos2++, items.get((2*i)));
						  inv.setItem(pos2++, items.get((2*i)));
						  inv.setItem(pos2++, items.get((2*i)));
						  inv.setItem(pos2++, items.get((2*i)));
						  inv.setItem(pos2++, items.get((2*i)+1));
						  inv.setItem(pos2++, items.get((2*i)+1));
						  inv.setItem(pos2++, items.get((2*i)+1));
						  inv.setItem(pos2++, items.get((2*i)+1));
						  inv.setItem(pos2++, items.get((2*i)+1));
					}
				break;

			case 9:
					int pos3 = 0;
					for(int j=0; j<4; j++)
						for(int i=0; i<9; i++)
							  inv.setItem(pos3++, items.get(i));
				break;

			case 10:
			case 11:
			case 12:
				int pos4 = 0;
					for(int i=0; i<items.size(); i++)
						for(int j=0; j<3; j++)
							  inv.setItem(pos4++, items.get(i));
				break;

			case 13:
			case 14:
			case 15:
			case 16:
			case 17:
			case 18:
				int pos;
				for(int i=0; i<items.size(); i++){
					if(i<9)
						pos=i;
					else pos=i+9;
					inv.setItem(pos, items.get(i));
					inv.setItem(pos+9, items.get(i));
				}
				break;

			default:
				for(int i=0; i<Math.min(items.size(), 36); i++)
					inv.setItem(i, items.get(i));
			}
		} catch (Exception e) {
			Plugin.getLogger().warning("Error arranging supply inventory: " + e);
		}

		// show the inventory dialog
		p.openInventory(inv);
	}

}
