package l2d.game.clientpackets;

/**
 * format: ddS
 */
public class PetitionVote extends L2GameClientPacket
{
	private int _type;

	@Override
	public void runImpl()
	{}

	@Override
	public void readImpl()
	{
		_type = readD();
		if(_type == 1)
		{
			int unk2 = readD();
			String petition = readS();
			System.out.println(getType() + " [1] :: " + unk2 + " :: " + petition);
		}
	}
}