package l2d.game.serverpackets;

import l2d.game.model.instances.L2ItemInstance;

public class TradeOwnAdd extends L2GameServerPacket
{
	private L2ItemInstance temp;
	private long _amount;

	public TradeOwnAdd(L2ItemInstance x, long amount)
	{
		temp = x;
		_amount = amount;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x20);

		writeH(1); // item count

		writeH(temp.getItem().getType1()); // item type1
		writeD(temp.getObjectId());
		writeD(temp.getItemId());
		writeD((int) _amount);
		writeH(temp.getItem().getType2ForPackets()); // item type2
		writeH(0x00); // ?

		writeD(temp.getBodyPart()); // rev 415  slot    0006-lr.ear  0008-neck  0030-lr.finger  0040-head  0080-??  0100-l.hand  0200-gloves  0400-chest  0800-pants  1000-feet  2000-??  4000-r.hand  8000-r.hand
		writeH(temp.getEnchantLevel()); // enchant level
		writeH(0x00); // ?
		writeH(0x00);
	}
}