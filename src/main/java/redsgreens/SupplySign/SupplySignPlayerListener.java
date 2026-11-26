package redsgreens.SupplySign;

import java.util.ArrayList;

import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Handle events for all Player related events
 * @author redsgreens
 */
public class SupplySignPlayerListener implements Listener {

	private final SupplySign Plugin;

	public SupplySignPlayerListener(SupplySign plugin)
	{
		Plugin = plugin;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerInteract(PlayerInteractEvent event)
	// catch player right-click events
	{
		// the off hand fires its own interact event for the same click
		if(event.getHand() == EquipmentSlot.OFF_HAND)
			return;

		Block block;
		Player player = event.getPlayer();

		// return if the event is not a right-click-block action
		Action action = event.getAction();
		if(action == Action.RIGHT_CLICK_BLOCK)
			block = event.getClickedBlock();
		else if(action == Action.RIGHT_CLICK_AIR)
		{
			try
			{
				block = player.getTargetBlockExact(4);
				if(block == null)
					return;
			}
			catch(Exception e)
			{
				return;
			}
		}
		else return;

		if (block == null || (!SupplySignUtil.isSignType(block.getType()) && !SupplySignUtil.isChest(block) && !SupplySignUtil.isDispenser(block)))
			return;

		Sign sign;
		if(SupplySignUtil.isChest(block)){
			sign = SupplySignUtil.getAttachedSign(block);
			if(sign == null) return;
		}
		else if(SupplySignUtil.isDispenser(block)){
			sign = SupplySignUtil.getAttachedSign(block);
			if(sign == null) return;
			else
			{ // prevent opening inventory of a dispenser with a supplysign attached
				event.setCancelled(true);
				Plugin.sendError(player, "SupplySign attached to dispenser, inventory unavailable.");
				return;
			}
		}
		else
			sign = (Sign)block.getState();

		try
		{
			if (SupplySignUtil.isSupplySign(sign)){
				event.setCancelled(true);

				if(SupplySignUtil.isWallSignType(sign.getType())) // special checks for wall signs on chests or dispensers
				{
					Block blockBehindSign = SupplySignUtil.getBlockBehindWallSign(sign.getBlock());
					if(blockBehindSign != null && SupplySignUtil.isDispenser(blockBehindSign)) // if it's a dispenser cancel right click on sign
					{
						Plugin.sendError(player, "SupplySign attached to dispenser, inventory unavailable.");
						return;
					}
				}

				ArrayList<Object> itemList = new ArrayList<Object>();

				// if it's a kit, test for generic access permission or access to this specific kit
				String line1 = SupplySignUtil.stripColorCodes(SupplySignUtil.getLine(sign, 1)).trim();
				if(line1.toLowerCase().contains("kit:")){
					String[] split = line1.split(":");
					String kitName = split.length > 1 ? split[1].trim() : "";

					if(Plugin.isAuthorized(player, "access") || Plugin.isAuthorized(player, "access." + kitName))
						itemList = Plugin.Kits.getKit(kitName);
					else
						Plugin.sendError(player, "you don't have permission to access this SupplySign.");
				}
				else
				{
					if(Plugin.isAuthorized(player, "access"))
					{
						// it's not a kit, so load the items from the lines on the sign
						for(int i = 1; i <= 3; i++)
						{
							String line = SupplySignUtil.stripColorCodes(SupplySignUtil.getLine(sign, i)).trim();
							if(!line.isEmpty())
								itemList.add(line);
						}
					}
					else
						Plugin.sendError(player, "you don't have permission to access this SupplySign.");
				}

				if(itemList.size() > 0)
					Plugin.Items.showInventory(player, itemList);

				return;
			}
		}
		catch (Throwable ex)
		{
			Plugin.sendError(player, String.valueOf(ex.getMessage()));
		}
	}
}
