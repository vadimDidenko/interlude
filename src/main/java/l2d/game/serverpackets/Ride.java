package l2d.game.serverpackets;

import l2d.game.model.L2Player;
import l2d.util.Location;

public class Ride extends L2GameServerPacket
{
	private boolean _canWriteImpl = false;
	private int _mountType, _id, _rideClassID;
	private Location _loc;

	/**
	 * 0x8c Ride dddd
	 */
	public Ride(L2Player cha)
	{
		if(cha == null)
			return;

		_id = cha.getObjectId();
		_mountType = cha.getMountType();
		_rideClassID = cha.getMountNpcId() + 1000000;
		_loc = cha.getLoc();

		_canWriteImpl = true;
	}

	@Override
	protected final void writeImpl()
	{
		if(!_canWriteImpl)
			return;

		writeC(0x86);
		writeD(_id);
		writeD(_mountType == 0 ? 0 : 1);
		writeD(_mountType);
		writeD(_rideClassID);
		writeD(_loc.x);
		writeD(_loc.y);
		writeD(_loc.z);
	}
}