package l2d.game.clientpackets;

import java.util.logging.Logger;

import l2d.game.model.L2Clan;
import l2d.game.model.L2Player;
import l2d.game.serverpackets.ManagePledgePower;

public class RequestPledgePower extends L2GameClientPacket
{
	static Logger _log = Logger.getLogger(RequestPledgePower.class.getName());
	// format: cdd(d)
	private int _rank;
	private int _action;
	private int _privs;

	@Override
	public void readImpl()
	{
		_rank = readD();
		_action = readD();
		if(_action == 2)
			_privs = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;
		if(activeChar.isGM())
			activeChar.sendMessage("RequestPledgePower");
		if(_action == 2)
		{
			if(_rank < 0 || _rank > 9)
				return;
			if(activeChar.getClan() != null && (activeChar.getClanPrivileges() & L2Clan.CP_CL_MANAGE_RANKS) == L2Clan.CP_CL_MANAGE_RANKS)
			{
				if(_rank == 9) // Академикам оставляем только перечисленные ниже права
					_privs = (_privs & L2Clan.CP_CL_VIEW_WAREHOUSE) + (_privs & L2Clan.CP_CH_OPEN_DOOR) + (_privs & L2Clan.CP_CS_OPEN_DOOR);
				activeChar.getClan().setRankPrivs(_rank, _privs);
				activeChar.getClan().updatePrivsForRank(_rank);
			}
		}
		else if(activeChar.getClan() != null)
			activeChar.sendPacket(new ManagePledgePower(activeChar, _action, _rank));
		else
			activeChar.sendActionFailed();
	}
}