package l2d.game.serverpackets;

import l2d.game.RecipeController;
import l2d.game.model.L2Player;
import l2d.game.model.L2Recipe;

/**
 * format ddddd
 */
public class RecipeItemMakeInfo extends L2GameServerPacket
{
	private int _id;
	private int _status;
	private int _CurMP;
	private int _MaxMP;

	public RecipeItemMakeInfo(int id, L2Player pl, int status)
	{
		_id = id;
		_status = status;
		_CurMP = (int) pl.getCurrentMp();
		_MaxMP = pl.getMaxMp();
	}

	@Override
	protected final void writeImpl()
	{
		L2Recipe recipeList = RecipeController.getInstance().getRecipeByRecipeId(_id);
		if(recipeList == null)
			return;

		writeC(0xD7);

		writeD(_id); //Точно: ID рецепта
		writeD(recipeList.isDwarvenRecipe() ? 0 : 1);
		writeD(_CurMP); //Точно: текущее состояние полоски Creator MP
		writeD(_MaxMP); //Точно: максимальное состояние полоски Creator MP
		writeD(_status); //Точно: итог крафта; 0xFFFFFFFF нет статуса, 0 удача, 1 провал
	}
}