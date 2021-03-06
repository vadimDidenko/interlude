package l2d.game.serverpackets;

import l2d.game.model.L2Character;
import l2d.util.Location;

/**
 * format   dddddd		(player id, target id, distance, startx, starty, startz)<p>
 */
public class ValidateLocation extends L2GameServerPacket
{
	private int _chaObjId;
	private Location _loc;

	public ValidateLocation(L2Character cha)
	{
		_chaObjId = cha.getObjectId();
		_loc = cha.getLoc();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x61);

		writeD(_chaObjId);
		writeD(_loc.x);
		writeD(_loc.y);
		writeD(_loc.z);
		writeD(_loc.h);
	}
}