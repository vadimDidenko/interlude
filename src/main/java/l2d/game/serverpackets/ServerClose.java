package l2d.game.serverpackets;

public class ServerClose extends L2GameServerPacket
{
	@Override
	protected void writeImpl()
	{
		writeC(0x26);
	}
}