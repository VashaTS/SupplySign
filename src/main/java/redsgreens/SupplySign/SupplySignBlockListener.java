package redsgreens.SupplySign;

import java.util.ArrayList;
import java.util.List;

import net.kyori.adventure.text.Component;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * SupplySign block listener
 * @author redsgreens
 */
public class SupplySignBlockListener implements Listener {

	private final SupplySign Plugin;

	public SupplySignBlockListener(final SupplySign plugin)
	{
		Plugin = plugin;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event)
	// this prevents a block from being placed against a SupplySign
	{
		Block against = event.getBlockAgainst();
		if(SupplySignUtil.isSignType(against.getType()) && SupplySignUtil.isSupplySign(against))
			event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event)
	// only allow players with permission to break a SupplySign or a chest/dispenser with one attached
	{
		Block block = event.getBlock();

		if(SupplySignUtil.isSignType(block.getType()))
		{
			if (SupplySignUtil.isSupplySign(block) && !Plugin.isAuthorized(event.getPlayer(), "destroy"))
				event.setCancelled(true);
		}
		else if (SupplySignUtil.isChest(block) || SupplySignUtil.isDispenser(block))
		{
			Sign sign = SupplySignUtil.getAttachedSign(block);
			if(sign != null && !Plugin.isAuthorized(event.getPlayer(), "destroy"))
				event.setCancelled(true);
		}
	}

	// give a player back the sign they just placed
	private void returnSign(Player player, Material signType)
	{
		if(player.getGameMode() == GameMode.CREATIVE)
			return;
		ItemStack signStack = new ItemStack(SupplySignUtil.signItemType(signType), 1);
		for(ItemStack left : player.getInventory().addItem(signStack).values())
			player.getWorld().dropItemNaturally(player.getLocation(), left);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onSignChange(SignChangeEvent event)
	// looks for a new SupplySign and tests it for validity
	{
		final Block signBlock = event.getBlock();
		final Material signType = signBlock.getType();

		if(!SupplySignUtil.isSignType(signType))
			return;

		if(Plugin.Config.FixSignOnSignGlitch != SupplySignOnSign.Disabled){
			// delete this sign if it's against another sign
			Block blockAgainst = null;

			if(SupplySignUtil.isStandingSignType(signType)){
				Block below = signBlock.getRelative(BlockFace.DOWN);
				if(SupplySignUtil.isSignType(below.getType()))
					blockAgainst = below;
			}
			else if(SupplySignUtil.isWallSignType(signType))
				blockAgainst = SupplySignUtil.getBlockBehindWallSign(signBlock);

			if(blockAgainst != null && SupplySignUtil.isSignType(blockAgainst.getType())){
				// the new sign is against another sign
				Sign signAgainst = (Sign)blockAgainst.getState();

				// check the config file to make sure the sign should be deleted
				if((Plugin.Config.FixSignOnSignGlitch == SupplySignOnSign.SupplySignOnly && SupplySignUtil.isSupplySign(signAgainst)) || Plugin.Config.FixSignOnSignGlitch == SupplySignOnSign.Global){
					signBlock.setType(Material.AIR);
					returnSign(event.getPlayer(), signType);
					event.setCancelled(true);
					return;
				}
			}
		}

		// done checking sign-on-sign bs, now on with setting up new signs
		try
		{
			// only proceed if it's a new sign
			if (SupplySignUtil.isNewSupplyLine(event.line(0)))
			{
				// and they have create permission
				if (Plugin.isAuthorized(event.getPlayer(), "create")){

					// they are allowed, continue

					// set the first line blue if it's not already
					if(!SupplySignUtil.isSupplyLine(event.line(0)))
						event.line(0, SupplySignUtil.HEADER);

					final Side side = event.getSide();
					final List<Component> lines = new ArrayList<Component>(event.lines());

					// a wall sign that is already on a chest or dispenser stays where it is
					Block behind = SupplySignUtil.isWallSignType(signType) ? SupplySignUtil.getBlockBehindWallSign(signBlock) : null;
					if(behind != null && (SupplySignUtil.isChest(behind) || SupplySignUtil.isDispenser(behind)))
						return;

					// only standing and wall signs can be attached to a chest or dispenser
					if(!SupplySignUtil.isStandingSignType(signType) && !SupplySignUtil.isWallSignType(signType))
						return;

					BlockFace attachTo = null;
					boolean isChest = false;
					BlockFace[] order = new BlockFace[]{BlockFace.WEST, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH};

					// if there is a chest nearby, then create a wallsign against it
					for(BlockFace f : order)
						if(SupplySignUtil.isValidChest(signBlock.getRelative(f))){
							attachTo = f;
							isChest = true;
							break;
						}

					// if it's a dispenser, put the sign there
					if(attachTo == null)
						for(BlockFace f : order)
							if(SupplySignUtil.isValidDispenser(signBlock.getRelative(f))){
								attachTo = f;
								break;
							}

					if(attachTo == null)
						return;

					final Dispenser dispenser = isChest ? null : (Dispenser)signBlock.getRelative(attachTo).getState();

					// the sign faces away from the block it is attached to
					Material wallType = SupplySignUtil.isWallSignType(signType) ? signType : SupplySignUtil.wallSignType(signType);
					WallSign data = (WallSign)wallType.createBlockData();
					data.setFacing(attachTo.getOppositeFace());
					signBlock.setBlockData(data);

					// the server applies the typed text to the old block entity, so write it again on the next tick
					Plugin.getServer().getScheduler().runTask(Plugin, new Runnable() {
						public void run() {
							if(!SupplySignUtil.isSignType(signBlock.getType()))
								return;

							Sign sign = (Sign)signBlock.getState();
							for(int i=0; i<lines.size() && i<4; i++)
								sign.getSide(side).line(i, lines.get(i));
							sign.update(true);

							if(dispenser != null)
								fillDispenser((Dispenser)dispenser.getBlock().getState(), sign);
						}
					});
				}
				else{
					// not allowed
					Plugin.sendError(event.getPlayer(), "Sign cannot be placed");

					signBlock.setType(Material.AIR);
					signBlock.getWorld().dropItemNaturally(signBlock.getLocation(), new ItemStack(SupplySignUtil.signItemType(signType), 1));
					event.setCancelled(true);
				}
				return;
			}
		}
		catch (Throwable ex)
		{
			Plugin.sendError(event.getPlayer(), String.valueOf(ex.getMessage()));
		}
	}

	// refill the dispenser after it fires
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockDispense(BlockDispenseEvent event)
	{
		BlockState state = event.getBlock().getState();
		if(state instanceof Dispenser)
		{
			final Block dispenserBlock = event.getBlock();
			final Sign s = SupplySignUtil.getAttachedSign(dispenserBlock);

			if(s != null)
				Plugin.getServer().getScheduler().runTask(Plugin, new Runnable() {
					public void run() {
						if(dispenserBlock.getState() instanceof Dispenser d)
							fillDispenser(d, s);
					}
				});
		}
	}

	private void fillDispenser(Dispenser dispenser, Sign sign){
		try{
			ArrayList<Object> itemList = new ArrayList<Object>();

			// test to see if it's a kit
			String line1 = SupplySignUtil.stripColorCodes(SupplySignUtil.getLine(sign, 1)).trim();
			if(line1.toLowerCase().contains("kit:")){
				String[] split = line1.split(":");
				itemList = Plugin.Kits.getKit(split.length > 1 ? split[1] : "");
			}
			else
			{
				// it's not a kit, so load the items from the lines on the sign
				for(int i = 1; i <= 3; i++)
				{
					String line = SupplySignUtil.stripColorCodes(SupplySignUtil.getLine(sign, i)).trim();
					if(!line.isEmpty())
						itemList.add(line);
				}
			}

			// if any valid items were found, fill the inventory with the items in itemList (one slot each, first nine)
			if(itemList.size() > 0)
			{
				Inventory inv = dispenser.getInventory();
				inv.clear();

				int max = itemList.size();

				for(int x=0; x < 9; x++)
				{
					if(x < max)
						inv.setItem(x, Plugin.Items.getItem(itemList.get(x).toString()));
				}
			}
		}
		catch(Exception ex){
			Plugin.getLogger().fine("Could not fill dispenser: " + ex.getMessage());
		}

	}
}
