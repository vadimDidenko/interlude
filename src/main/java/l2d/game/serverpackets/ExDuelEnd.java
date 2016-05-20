package l2d.game.serverpackets;

public class ExDuelEnd extends L2GameServerPacket
{
	int _duelType;

	public ExDuelEnd(int duelType)
	{
		_duelType = duelType;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x4e);
		writeD(_duelType); //хз почему 0, возможно 1 для party duel. нет возможности проверить.
	}
}