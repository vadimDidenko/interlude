package l2d.game.serverpackets;

import l2d.game.model.L2Clan.SubPledge;

public class PledgeReceiveSubPledgeCreated extends L2GameServerPacket
{
	private int type;
	private String _name, leader_name;

	public PledgeReceiveSubPledgeCreated(SubPledge subPledge)
	{
		type = subPledge.getType();
		_name = subPledge.getName();
		leader_name = subPledge.getLeaderName();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x3f);

		writeD(0x01);
		writeD(type);
		writeS(_name);
		writeS(leader_name);
	}
}