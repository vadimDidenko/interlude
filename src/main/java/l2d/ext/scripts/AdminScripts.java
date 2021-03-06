package l2d.ext.scripts;

import l2d.game.handler.IAdminCommandHandler;
import l2d.game.model.L2Player;
import l2d.game.model.quest.Quest;
import l2d.game.model.quest.QuestState;

public class AdminScripts implements IAdminCommandHandler
{
	private static enum Commands
	{
		admin_scripts_reload,
		admin_sreload,
		admin_sqreload
	}

	@Override
	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, L2Player activeChar)
	{
		Commands command = (Commands) comm;

		if(!activeChar.getPlayerAccess().IsGM)
			return false;

		switch(command)
		{
			case admin_scripts_reload:
			case admin_sreload:
				if(wordList.length < 2)
					return false;
				String param = wordList[1];
				if(param.equalsIgnoreCase("all"))
				{
					activeChar.sendMessage("Scripts reload starting...");
					if(Scripts.getInstance().reload())
						activeChar.sendMessage("Scripts reloaded with errors. Loaded " + Scripts.getInstance().getClasses().size() + " classes.");
					else
						activeChar.sendMessage("Scripts successfully reloaded. Loaded " + Scripts.getInstance().getClasses().size() + " classes.");
				}
				else if(Scripts.getInstance().reloadClass(param))
					activeChar.sendMessage("Scripts reloaded with errors. Loaded " + Scripts.getInstance().getClasses().size() + " classes.");
				else
					activeChar.sendMessage("Scripts successfully reloaded. Loaded " + Scripts.getInstance().getClasses().size() + " classes.");
				break;
			case admin_sqreload:
				if(wordList.length < 2)
					return false;
				String quest = wordList[1];
				if(Scripts.getInstance().reloadQuest(quest))
					activeChar.sendMessage("Quest '" + quest + "' reloaded with errors.");
				else
					activeChar.sendMessage("Quest '" + quest + "' successfully reloaded.");
				reloadQuestStates(activeChar);
				break;
		}
		return true;
	}

	private void reloadQuestStates(L2Player p)
	{
		for(QuestState qs : p.getAllQuestsStates())
			p.delQuestState(qs.getQuest().getName());

		Quest.playerEnter(p);
	}

	@Override
	public Enum[] getAdminCommandEnum()
	{
		return Commands.values();
	}
}