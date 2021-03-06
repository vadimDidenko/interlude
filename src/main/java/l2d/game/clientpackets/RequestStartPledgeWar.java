package l2d.game.clientpackets;

import java.util.logging.Logger;

import l2d.game.model.L2Clan;
import l2d.game.model.L2Player;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.ClanTable;

public class RequestStartPledgeWar extends L2GameClientPacket
{
	//Format: cS
	private static Logger _log = Logger.getLogger(RequestStartPledgeWar.class.getName());

	String _pledgeName;
	L2Clan _clan;

	@Override
	public void readImpl()
	{
		_pledgeName = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;
		if(activeChar.isGM())
			activeChar.sendMessage("RequestStartPledgeWar");

		_clan = activeChar.getClan();
		if(_clan == null)
		{
			activeChar.sendActionFailed();
			return;
		}

		if(!((activeChar.getClanPrivileges() & L2Clan.CP_CL_PLEDGE_WAR) == L2Clan.CP_CL_PLEDGE_WAR))
		{
			activeChar.sendActionFailed();
			return;
		}

		if(_clan.getWarsCount() >= 30)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.A_DECLARATION_OF_WAR_AGAINST_MORE_THAN_30_CLANS_CANT_BE_MADE_AT_THE_SAME_TIME));
			activeChar.sendActionFailed();
			return;
		}

		/*if(_clan.getLevel() < 3 || _clan.getMembersCount() < 15)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.A_CLAN_WAR_CAN_BE_DECLARED_ONLY_IF_THE_CLAN_IS_LEVEL_THREE_OR_ABOVE_AND_THE_NUMBER_OF_CLAN_MEMBERS_IS_FIFTEEN_OR_GREATER));
			activeChar.sendActionFailed();
			return;
		}*/

		L2Clan clan = ClanTable.getInstance().getClanByName(_pledgeName);
		if(clan == null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.THE_DECLARATION_OF_WAR_CANT_BE_MADE_BECAUSE_THE_CLAN_DOES_NOT_EXIST_OR_ACT_FOR_A_LONG_PERIOD));
			activeChar.sendActionFailed();
			return;
		}

		else if(_clan.equals(clan))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.FOOL_YOU_CANNOT_DECLARE_WAR_AGAINST_YOUR_OWN_CLAN));
			activeChar.sendActionFailed();
			return;
		}

		else if(_clan.isAtWarWith(clan.getClanId()))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.THE_DECLARATION_OF_WAR_HAS_BEEN_ALREADY_MADE_TO_THE_CLAN));
			activeChar.sendActionFailed();
			return;
		}

		else if(_clan.getAllyId() == clan.getAllyId() && _clan.getAllyId() != 0)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.A_DECLARATION_OF_CLAN_WAR_AGAINST_AN_ALLIED_CLAN_CANT_BE_MADE));
			activeChar.sendActionFailed();
			return;
		}

		/*else if(clan.getLevel() < 3 || clan.getMembersCount() < 15)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.A_CLAN_WAR_CAN_BE_DECLARED_ONLY_IF_THE_CLAN_IS_LEVEL_THREE_OR_ABOVE_AND_THE_NUMBER_OF_CLAN_MEMBERS_IS_FIFTEEN_OR_GREATER));
			activeChar.sendActionFailed();
			return;
		}*/

		_log.info("RequestStartPledgeWar: By player: " + activeChar.getName() + " of clan: " + _clan.getName() + " to clan: " + _pledgeName);
		ClanTable.getInstance().startClanWar(activeChar.getClan(), clan);
	}
}