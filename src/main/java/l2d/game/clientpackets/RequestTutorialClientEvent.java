package l2d.game.clientpackets;

import l2d.game.instancemanager.QuestManager;
import l2d.game.model.L2Player;
import l2d.game.model.quest.Quest;

public class RequestTutorialClientEvent extends L2GameClientPacket
{
	// format: cd
	int event = 0;

	/**
	 * Пакет от клиента, если вы в туториале подергали мышкой как надо - клиент пришлет его со значением 1 ну или нужным ивентом
	 */
	@Override
	public void readImpl()
	{
		event = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getActiveChar();
		if(player == null)
			return;

		Quest tutorial = QuestManager.getQuest(255);
		if(tutorial != null)
			player.processQuestEvent(tutorial.getName(), "CE" + event);
	}
}