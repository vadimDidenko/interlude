package items;

import l2d.ext.scripts.ScriptFile;
import l2d.game.cache.Msg;
import l2d.game.handler.IItemHandler;
import l2d.game.handler.ItemHandler;
import l2d.game.model.L2Playable;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.serverpackets.Dice;
import l2d.game.serverpackets.SystemMessage;
import l2d.util.Rnd;

public class RollingDice implements IItemHandler, ScriptFile
{
	// all the items ids that this handler knowns
	private static final int[] _itemIds = { 4625, 4626, 4627, 4628 };

	static final SystemMessage YOU_MAY_NOT_THROW_THE_DICE_AT_THIS_TIMETRY_AGAIN_LATER = new SystemMessage(SystemMessage.YOU_MAY_NOT_THROW_THE_DICE_AT_THIS_TIMETRY_AGAIN_LATER);

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if(playable == null || !playable.isPlayer())
			return;
		L2Player player = (L2Player) playable;

		int itemId = item.getItemId();

		if(player.isInOlympiadMode())
		{
			player.sendPacket(Msg.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
			return;
		}

		if(player.isSitting())
		{
			player.sendPacket(Msg.YOU_CANNOT_MOVE_WHILE_SITTING);
			return;
		}

		if(itemId == 4625 || itemId == 4626 || itemId == 4627 || itemId == 4628)
		{
			int number = rollDice(player);
			if(number == 0)
			{
				player.sendPacket(YOU_MAY_NOT_THROW_THE_DICE_AT_THIS_TIMETRY_AGAIN_LATER);
				return;
			}

			player.broadcastPacket(new Dice(player.getObjectId(), itemId, number, player.getX() - 30, player.getY() - 30, player.getZ()));

			SystemMessage sm = new SystemMessage(SystemMessage.S1_HAS_ROLLED_S2);
			sm.addString(player.getName());
			sm.addNumber(number);
			player.sendPacket(sm);
			player.broadcastPacketToOthers(sm);
		}

	}

	private int rollDice(L2Player player)
	{
		// Reuse time 4000 ms
		if(System.currentTimeMillis() <= player.lastDiceThrown)
			return 0;
		player.lastDiceThrown = System.currentTimeMillis() + 4000L;
		return Rnd.get(1, 6);
	}

	public final int[] getItemIds()
	{
		return _itemIds;
	}

	public void onLoad()
	{
		ItemHandler.getInstance().registerItemHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}