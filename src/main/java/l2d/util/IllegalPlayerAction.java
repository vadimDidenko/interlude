package l2d.util;

import l2d.Config;
import l2d.ext.multilang.CustomMessage;
import l2d.game.model.L2Player;
import l2d.game.tables.GmListTable;

public final class IllegalPlayerAction implements Runnable
{
	String etc_str1;
	String etc_str2;
	int isBug;
	L2Player actor;

	public static final int INFO = 0;
	public static final int WARNING = 1;
	public static final int CRITICAL = 2;

	public IllegalPlayerAction(L2Player actor, String etc_str1, String etc_str2, int isBug)
	{
		this.etc_str1 = etc_str1;
		this.etc_str2 = etc_str2;
		this.isBug = isBug;
		this.actor = actor;
	}

	@Override
	public void run()
	{
		StringBuffer msgb = new StringBuffer(160);
		int punishment = -1;
		msgb.append("IllegalAction: " + actor.getName() + " " + etc_str1 + " " + etc_str2);

		switch(isBug)
		{
			case INFO:
				punishment = 0;
				break;
			case WARNING:
				punishment = Config.DEFAULT_PUNISH;
				break;
			case CRITICAL:
				punishment = Config.BUGUSER_PUNISH;
				break;
		}

		if(actor.isGM())
			punishment = 0;

		Log.LogBug(actor, isBug > 1 ? Log.BugUse : Log.IllegalAction, etc_str1, etc_str2, "", punishment, 0);
		switch(punishment)
		{
			case 0:
				msgb.append(" punish: none");
				actor.sendMessage(new CustomMessage("l2d.Util.IllegalAction.case0", actor));
				return;
			case 1:
				actor.sendMessage(new CustomMessage("l2d.Util.IllegalAction.case1", actor));
				try
				{
					Thread.sleep(1000);
				}
				catch(InterruptedException e)
				{
					e.printStackTrace();
				}
				actor.logout(false, false, true);
				msgb.append(" punish: kicked");
				break;
			case 2:
				actor.sendMessage(new CustomMessage("l2d.Util.IllegalAction.case2", actor));
				actor.setAccessLevel(-100);
				actor.setAccountAccesslevel(-100, "Autoban: " + etc_str2 + " in " + etc_str1, -1);
				try
				{
					Thread.sleep(1000);
				}
				catch(InterruptedException e)
				{
					e.printStackTrace();
				}
				actor.logout(false, false, true);
				msgb.append(" punish: banned");
				Log.add(msgb.toString(), "banned");
		}
		GmListTable.broadcastMessageToGMs(msgb.toString());
	}
}