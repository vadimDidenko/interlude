package l2d.game.serverpackets;

import java.util.List;

import javolution.util.FastList;
import l2d.game.model.L2Clan;

public class PledgeReceiveWarList extends L2GameServerPacket
{
	private FastList<WarInfo> infos = new FastList<WarInfo>();
	private int _updateType;
	@SuppressWarnings("unused")
	private int _page;

	public PledgeReceiveWarList(L2Clan clan, int type, int page)
	{
		_updateType = type;
		_page = page;
		infos.clear();
		List<L2Clan> clans = _updateType == 1 ? clan.getAttackerClans() : clan.getEnemyClans();
		for(L2Clan _clan : clans)
		{
			if(_clan == null)
				continue;
			infos.add(new WarInfo(_clan.getName(), _updateType, 0));
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x3e);
		writeD(_updateType); //which type of war list sould be revamped by this packet
		writeD(0x00); //page number goes here(_page ), made it static cuz not sure how many war to add to one page so TODO here
		writeD(infos.size());
		for(WarInfo _info : infos)
		{
			writeS(_info.clan_name);
			writeD(_info.unk1);
			writeD(_info.unk2); //filler ??
		}
	}

	static class WarInfo
	{
		public String clan_name;
		public int unk1, unk2;

		public WarInfo(String _clan_name, int _unk1, int _unk2)
		{
			clan_name = _clan_name;
			unk1 = _unk1;
			unk2 = _unk2;
		}
	}
}