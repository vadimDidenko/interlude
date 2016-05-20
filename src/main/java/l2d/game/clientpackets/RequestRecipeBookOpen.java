package l2d.game.clientpackets;

import l2d.game.RecipeController;
import l2d.game.model.L2Player;

public class RequestRecipeBookOpen extends L2GameClientPacket
{
	private boolean isDwarvenCraft = true;

	/**
	 * packet type id 0xB5
	 * format:		cd
	 */
	@Override
	public void readImpl()
	{
		if(_buf.hasRemaining())
			isDwarvenCraft = readD() == 0;
		else
			return;
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;
		RecipeController.getInstance().requestBookOpen(activeChar, isDwarvenCraft);
	}
}