package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2World;
import com.lineage.game.serverpackets.GMHennaInfo;
import com.lineage.game.serverpackets.GMViewCharacterInfo;
import com.lineage.game.serverpackets.GMViewItemList;
import com.lineage.game.serverpackets.GMViewPledgeInfo;
import com.lineage.game.serverpackets.GMViewQuestInfo;
import com.lineage.game.serverpackets.GMViewSkillInfo;
import com.lineage.game.serverpackets.GMViewWarehouseWithdrawList;

public class RequestGMCommand extends L2GameClientPacket
{
	// format: cSdd
	private String _targetName;
	private int _command;
	@SuppressWarnings("unused")
	private int _unknown;

	@Override
	public void readImpl()
	{
		_targetName = readS();
		_command = readD();
		_unknown = readD(); //always 0
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = L2World.getPlayer(_targetName);
		if(activeChar == null)
			return;
		if(getClient().getActiveChar() == null)
			return;
		if(!getClient().getActiveChar().getPlayerAccess().CanViewChar)
			return;

		switch(_command)
		{
			case 1:
				sendPacket(new GMViewCharacterInfo(activeChar));
				sendPacket(new GMHennaInfo(activeChar));
				break;
			case 2:
				if(activeChar.getClan() != null)
					sendPacket(new GMViewPledgeInfo(activeChar.getClan(), activeChar));
				break;
			case 3:
				sendPacket(new GMViewSkillInfo(activeChar));
				break;
			case 4:
				sendPacket(new GMViewQuestInfo(activeChar));
				break;
			case 5:
				sendPacket(new GMViewItemList(activeChar));
				break;
			case 6:
				sendPacket(new GMViewWarehouseWithdrawList(activeChar));
				break;
			default:
				System.out.println("Request Unknown GMCommand :: " + _command);
		}
	}
}