package l2d.game.clientpackets;

import l2d.Config;
import l2d.game.ThreadPoolManager;
import l2d.game.model.L2Player;
import l2d.game.serverpackets.SocialAction;
import l2d.game.serverpackets.SystemMessage;
import l2d.util.Util;

public class RequestSocialAction extends L2GameClientPacket
{
	private int _actionId;

	/**
	 * packet type id 0x34
	 * format:		cd
	 */
	@Override
	public void readImpl()
	{
		_actionId = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		if(activeChar.isOutOfControl())
		{
			activeChar.sendActionFailed();
			return;
		}

		// You cannot do anything else while fishing
		if(activeChar.isFishing())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_DO_ANYTHING_ELSE_WHILE_FISHING));
			return;
		}

		// internal Social Action check
		if(_actionId < 2 || _actionId > 14)
		{
			Util.handleIllegalPlayerAction(activeChar, "RequestSocialAction[43]", "Character " + activeChar.getName() + " at account " + activeChar.getAccountName() + "requested an internal Social Action " + _actionId, 1);
			return;
		}

		if(activeChar.getPrivateStoreType() == L2Player.STORE_PRIVATE_NONE && activeChar.getTransactionRequester() == null && !activeChar.isActionsDisabled() && !activeChar.isSitting())
		{
			activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), _actionId));
			if(Config.ALT_SOCIAL_ACTION_REUSE)
			{
				ThreadPoolManager.getInstance().scheduleAi(new SocialTask(activeChar), 2600, true);
				activeChar.block();
			}
		}
	}

	class SocialTask implements Runnable
	{
		L2Player _player;

		SocialTask(L2Player player)
		{
			_player = player;
		}

		@Override
		public void run()
		{
			_player.unblock();
		}
	}
}