package l2d.game.serverpackets;

import java.util.logging.Logger;

import l2d.Config;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.templates.L2Item;

/**
 * 5e
 * 01 00 00 00 		01 - added ?  02 - modified
 * 7b 86 73 42      object id
 * 08 00 00 00      body slot
 *
 * body slot
 * 0000  ?? underwear
 * 0001  ear
 * 0002  ear
 * 0003  neck
 * 0004  finger   (magic ring)
 * 0005  finger   (magic ring)
 * 0006  head     (l.cap)
 * 0007  r.hand   (dagger)
 * 0008  l.hand   (arrows)
 * 0009  hands    (short gloves)
 * 000a  chest    (squire shirt)
 * 000b  legs     (squire pants)
 * 000c  feet
 * 000d  ?? back
 * 000e  lr.hand   (bow)
 *
 * format  ddd
 */
public class EquipUpdate extends L2GameServerPacket
{
	private static Logger _log = Logger.getLogger(EquipUpdate.class.getName());

	private L2ItemInstance _item;
	private int _change;

	public EquipUpdate(L2ItemInstance item, int change)
	{
		_item = item;
		_change = change;
	}

	@Override
	protected final void writeImpl()
	{
		int bodypart = 0;
		writeC(0x4b);
		writeD(_change);
		writeD(_item.getObjectId());
		switch(_item.getBodyPart())
		{
			case L2Item.SLOT_L_EAR:
				bodypart = 0x01;
				break;
			case L2Item.SLOT_R_EAR:
				bodypart = 0x02;
				break;
			case L2Item.SLOT_NECK:
				bodypart = 0x03;
				break;
			case L2Item.SLOT_R_FINGER:
				bodypart = 0x04;
				break;
			case L2Item.SLOT_L_FINGER:
				bodypart = 0x05;
				break;
			case L2Item.SLOT_HEAD:
				bodypart = 0x06;
				break;
			case L2Item.SLOT_R_HAND:
				bodypart = 0x07;
				break;
			case L2Item.SLOT_L_HAND:
				bodypart = 0x08;
				break;
			case L2Item.SLOT_GLOVES:
				bodypart = 0x09;
				break;
			case L2Item.SLOT_CHEST:
				bodypart = 0x0a;
				break;
			case L2Item.SLOT_LEGS:
				bodypart = 0x0b;
				break;
			case L2Item.SLOT_FEET:
				bodypart = 0x0c;
				break;
			case L2Item.SLOT_BACK:
				bodypart = 0x0d;
				break;
			case L2Item.SLOT_LR_HAND:
				bodypart = 0x0e;
				break;
			case L2Item.SLOT_HAIR:
				bodypart = 0x0f;
				break;
		}

		if(Config.DEBUG)
			_log.fine("body:" + bodypart);
		writeD(bodypart);
	}
}