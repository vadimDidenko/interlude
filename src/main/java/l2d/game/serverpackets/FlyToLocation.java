package l2d.game.serverpackets;

import l2d.game.model.L2Character;
import l2d.util.Location;

public class FlyToLocation extends L2GameServerPacket
{
	private int _chaObjId;
	private final FlyType _type;
	private Location _loc;
	private Location _destLoc;

	public enum FlyType
	{
		THROW_UP,
		THROW_HORIZONTAL,
		DUMMY,
		CHARGE,
		NONE
	}

	public FlyToLocation(L2Character cha, Location destLoc, FlyType type)
	{
		_destLoc = destLoc;
		_type = type;
		_chaObjId = cha.getObjectId();
		_loc = cha.getLoc();
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xd4);
		writeD(_chaObjId);
		writeD(_destLoc.x);
		writeD(_destLoc.y);
		writeD(_destLoc.z);
		writeD(_loc.x);
		writeD(_loc.y);
		writeD(_loc.z);
		writeD(_type.ordinal());
	}
}