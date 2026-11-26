package redsgreens.SupplySign;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

/**
 * One entry of the item tables: a material, a stack size and an optional potion type.
 */
public class SupplySignItemStack {

	private final Material material;
	private final int amount;
	private final PotionType potionType;

	public SupplySignItemStack(Material m, int a)
	{
		this(m, a, null);
	}

	public SupplySignItemStack(Material m, int a, PotionType p)
	{
		material = m;
		amount = Math.max(1, Math.min(99, a));
		potionType = p;
	}

	public ItemStack getItemStack()
	{
		ItemStack stack = new ItemStack(material, amount);
		if(potionType != null && stack.getItemMeta() instanceof PotionMeta meta)
		{
			meta.setBasePotionType(potionType);
			stack.setItemMeta(meta);
		}
		return stack;
	}

	public Material getMaterial()
	{
		return material;
	}

	public int getAmount()
	{
		return amount;
	}

	public PotionType getPotionType()
	{
		return potionType;
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
			return true;
		if (!(other instanceof SupplySignItemStack otherIS))
			return false;
		return this.material == otherIS.material && this.amount == otherIS.amount && java.util.Objects.equals(this.potionType, otherIS.potionType);
	}

	@Override
	public int hashCode()
	{
		return java.util.Objects.hash(material, amount, potionType);
	}

	@Override
	public String toString()
	{
		return "Material=" + material.name().toLowerCase() + ", Amount=" + amount + (potionType != null ? ", Potion=" + potionType : "");
	}
}
