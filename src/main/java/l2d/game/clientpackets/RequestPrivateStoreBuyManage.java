package l2d.game.clientpackets;

public class RequestPrivateStoreBuyManage extends L2GameClientPacket
{
	@Override
	public void runImpl()
	{
		System.out.println(getType());
	}

	@Override
	public void readImpl()
	{}
}