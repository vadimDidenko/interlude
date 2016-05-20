package com.lineage.game.skills.skillclasses;

import com.lineage.game.tables.ItemTable;
import com.lineage.game.templates.StatsSet;
import javolution.util.FastList;
import com.lineage.game.model.Inventory;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.model.instances.L2PetInstance;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.util.Rnd;

public class SummonItem extends L2Skill
{
	private final short _itemId;
	private final int _minId;
	private final int _maxId;
	private final int _minCount;
	private final int _maxCount;

	public SummonItem(final StatsSet set)
	{
		super(set);

		_itemId = set.getShort("SummonItemId", (short) 0);
		_minId = set.getInteger("SummonMinId", 0);
		_maxId = set.getInteger("SummonMaxId", _minId);
		_minCount = set.getInteger("SummonMinCount");
		_maxCount = set.getInteger("SummonMaxCount", _minCount);
	}

	@Override
	public void useSkill(final L2Character activeChar, final FastList<L2Character> targets)
	{
		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = n.getValue();
			Inventory inventory;
			if(target.isPlayer())
				inventory = ((L2Player) target).getInventory();
			else if(target.isPet())
				inventory = ((L2PetInstance) target).getInventory();
			else
				continue;

			L2ItemInstance item;
			if(_minId > 0)
				item = ItemTable.getInstance().createItem(Rnd.get(_minId, _maxId));
			else
				item = ItemTable.getInstance().createItem(_itemId);

			final int count = Rnd.get(_minCount, _maxCount);
			item.setCount(count);
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S2_S1S).addItemName(item.getItemId()).addNumber(count));
			item = inventory.addItem(item);
			activeChar.sendChanges();
			getEffects(activeChar, target, getActivateRate() > 0, false);
		}
	}
}