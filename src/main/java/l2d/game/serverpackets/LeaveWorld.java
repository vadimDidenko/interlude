package l2d.game.serverpackets;

public class LeaveWorld extends L2GameServerPacket
{
	@Override
	protected final void writeImpl()
	{
		writeC(0x7e);
	}
}