package l2d.game.serverpackets;

import l2d.game.model.L2Object;
import l2d.util.Location;

/**
 * format  dddd
 *
 * sample
 * 0000: 3a  69 08 10 48  02 c1 00 00  f7 56 00 00  89 ea ff    :i..H.....V.....
 * 0010: ff  0c b2 d8 61                                     ....a
 */
public class TeleportToLocation extends L2GameServerPacket
{
	private int _targetId;
	private Location _loc;

	public TeleportToLocation(L2Object cha, Location loc)
	{
		_targetId = cha.getObjectId();
		_loc = loc;
	}

	public TeleportToLocation(L2Object cha, int x, int y, int z)
	{
		_targetId = cha.getObjectId();
		_loc = new Location(x, y, z, cha.getHeading());
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x28);
		writeD(_targetId);
		writeD(_loc.x);
		writeD(_loc.y);
		writeD(_loc.z);
		writeD(0x00); //IsValidation
		writeD(_loc.h);
	}
}