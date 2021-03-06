package l2d.game.model.inventory.listeners;

import l2d.game.model.Inventory;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2ItemInstance;

public final class FormalWearListener implements PaperdollListener
{
	private Inventory _inv;

	public FormalWearListener(Inventory inv)
	{
		_inv = inv;
	}

	@Override
	public void notifyUnequipped(int slot, L2ItemInstance item)
	{
		if(!(_inv.getOwner() != null && _inv.getOwner().isPlayer()))
			return;

		L2Player owner = (L2Player) _inv.getOwner();

		if(item.getItemId() == 6408)
			owner.setIsWearingFormalWear(false);
	}

	@Override
	public void notifyEquipped(int slot, L2ItemInstance item)
	{
		if(!(_inv.getOwner() != null && _inv.getOwner().isPlayer()))
			return;

		L2Player owner = (L2Player) _inv.getOwner();

		// If player equip Formal Wear unequip weapons and abort cast/attack
		if(item.getItemId() == 6408)
			owner.setIsWearingFormalWear(true);
	}
}