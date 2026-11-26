package redsgreens.SupplySign;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;

public class SupplySignUtil {

	// the first line of a supply sign: "[Supply]" in dark blue (legacy form: section sign, 1, "[Supply]")
	public static final String HEADER_LEGACY = "\u00A71[Supply]";
	public static final Component HEADER = Component.text("[Supply]", NamedTextColor.DARK_BLUE);

	private static final BlockFace[] SIDES = new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

	/* ---------- text helpers ---------- */

	public static String plain(Component c)
	{
		return c == null ? "" : PlainTextComponentSerializer.plainText().serialize(c);
	}

	public static String stripColorCodes(String str)
	{
		return str.replaceAll("\u00A7[0-9a-fA-Fk-oK-OrR]", "");
	}

	// true if this line is the blue "[Supply]" header
	public static boolean isSupplyLine(Component line)
	{
		return line != null && LegacyComponentSerializer.legacySection().serialize(line).equals(HEADER_LEGACY);
	}

	// true if this line is a not-yet-formatted "[Supply]" that a player just typed
	public static boolean isNewSupplyLine(Component line)
	{
		return isSupplyLine(line) || plain(line).trim().equalsIgnoreCase("[Supply]");
	}

	/* ---------- sign helpers ---------- */

	public static boolean isSignType(Material m)
	{
		return Tag.ALL_SIGNS.isTagged(m);
	}

	public static boolean isStandingSignType(Material m)
	{
		return Tag.STANDING_SIGNS.isTagged(m);
	}

	public static boolean isWallSignType(Material m)
	{
		return Tag.WALL_SIGNS.isTagged(m);
	}

	// the item a sign block drops: OAK_WALL_SIGN -> OAK_SIGN, SPRUCE_WALL_HANGING_SIGN -> SPRUCE_HANGING_SIGN
	public static Material signItemType(Material m)
	{
		Material item = Material.getMaterial(m.name().replace("_WALL_", "_"));
		return item != null ? item : m;
	}

	// OAK_SIGN -> OAK_WALL_SIGN
	public static Material wallSignType(Material standing)
	{
		return Material.getMaterial(standing.name().replace("_SIGN", "_WALL_SIGN"));
	}

	// which side of this sign carries the "[Supply]" header? null if neither
	public static Side getSupplySide(Sign sign)
	{
		if(isSupplyLine(sign.getSide(Side.FRONT).line(0)))
			return Side.FRONT;
		if(isSupplyLine(sign.getSide(Side.BACK).line(0)))
			return Side.BACK;
		return null;
	}

	public static Boolean isSupplySign(Sign sign)
	{
		return getSupplySide(sign) != null;
	}

	public static Boolean isSupplySign(Block b)
	{
		if(!isSignType(b.getType()))
			return false;
		return isSupplySign((Sign)b.getState());
	}

	// the text of line 0..3 of the supply side of the sign
	public static String getLine(Sign sign, int i)
	{
		Side side = getSupplySide(sign);
		if(side == null) side = Side.FRONT;
		SignSide ss = sign.getSide(side);
		return plain(ss.line(i));
	}

	// get the block that has a wall sign on it
	public static Block getBlockBehindWallSign(Block signBlock)
	{
		BlockData data = signBlock.getBlockData();
		if((isWallSignType(signBlock.getType()) || Tag.WALL_HANGING_SIGNS.isTagged(signBlock.getType())) && data instanceof Directional d)
			return signBlock.getRelative(d.getFacing().getOppositeFace());
		return null;
	}

	/* ---------- chest and dispenser helpers ---------- */

	public static boolean isChest(Block b)
	{
		return b.getType() == Material.CHEST;
	}

	public static boolean isDispenser(Block b)
	{
		return b.getType() == Material.DISPENSER;
	}

	// does this chest/dispenser already have a supply sign attached to it?
	private static boolean hasSupplySign(Block b)
	{
		for(BlockFace f : SIDES)
		{
			Block adj = b.getRelative(f);
			if(isWallSignType(adj.getType()) && b.equals(getBlockBehindWallSign(adj)) && isSupplySign(adj))
				return true;
		}
		return false;
	}

	// check to see if this is a chest without a supply sign already on it
	public static boolean isValidChest(Block b)
	{
		return isChest(b) && !hasSupplySign(b);
	}

	// check to see if this is a dispenser without a supply sign already on it
	public static boolean isValidDispenser(Block b)
	{
		return isDispenser(b) && !hasSupplySign(b);
	}

	// check to see if this is a single wide chest
	public static boolean isSingleChest(Block b)
	{
		return isChest(b) && ((org.bukkit.block.data.type.Chest)b.getBlockData()).getType() == org.bukkit.block.data.type.Chest.Type.SINGLE;
	}

	// check to see if this is a double chest
	public static boolean isDoubleChest(Block b)
	{
		return isChest(b) && ((org.bukkit.block.data.type.Chest)b.getBlockData()).getType() != org.bukkit.block.data.type.Chest.Type.SINGLE;
	}

	private static BlockFace clockwise(BlockFace f)
	{
		return switch(f) {
			case NORTH -> BlockFace.EAST;
			case EAST -> BlockFace.SOUTH;
			case SOUTH -> BlockFace.WEST;
			default -> BlockFace.NORTH;
		};
	}

	// the other half of a double chest, or null for a single chest
	public static Block findOtherHalfofChest(Block b)
	{
		if(!isChest(b)) return null;
		org.bukkit.block.data.type.Chest data = (org.bukkit.block.data.type.Chest)b.getBlockData();
		if(data.getType() == org.bukkit.block.data.type.Chest.Type.SINGLE) return null;

		BlockFace facing = data.getFacing();
		BlockFace dir = data.getType() == org.bukkit.block.data.type.Chest.Type.LEFT ? clockwise(facing) : clockwise(facing).getOppositeFace();
		Block other = b.getRelative(dir);
		return isChest(other) ? other : null;
	}

	// find a supply sign attached to a chest (either half of a double chest) or dispenser
	public static Sign getAttachedSign(Block b){
		if(!isChest(b) && !isDispenser(b))
			return null;

		Block b2 = isChest(b) ? findOtherHalfofChest(b) : null;

		for(Block part : b2 == null ? new Block[]{b} : new Block[]{b, b2})
		{
			for(BlockFace f : SIDES)
			{
				Block adj = part.getRelative(f);
				if(isWallSignType(adj.getType()) && part.equals(getBlockBehindWallSign(adj)) && isSupplySign(adj))
					return (Sign)adj.getState();
			}
		}

		return null;
	}
}
