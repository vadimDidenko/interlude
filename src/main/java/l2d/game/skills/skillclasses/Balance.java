package l2d.game.skills.skillclasses;

import javolution.util.FastList;
import l2d.game.model.L2Character;
import l2d.game.model.L2Skill;
import l2d.game.templates.StatsSet;

public class Balance extends L2Skill
{
	public Balance(StatsSet set)
	{
		super(set);
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{
		double summaryCurrentHp = 0;
		int summaryMaximumHp = 0;

		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = n.getValue();
			if(target.isDead() || target.isHealBlocked())
				continue;
			summaryCurrentHp += target.getCurrentHp();
			summaryMaximumHp += target.getMaxHp();
		}

		double percent = summaryCurrentHp / summaryMaximumHp;

		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = n.getValue();
			if(target.isDead() || target.isHealBlocked())
				continue;
			target.setCurrentHp(target.getMaxHp() * percent, false);
			getEffects(activeChar, target, getActivateRate() > 0, false);
		}

		if(isSSPossible())
			activeChar.unChargeShots(isMagic());
	}
}
