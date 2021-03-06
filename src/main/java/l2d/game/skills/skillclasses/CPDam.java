package l2d.game.skills.skillclasses;

import javolution.util.FastList;
import l2d.game.model.L2Character;
import l2d.game.model.L2Skill;
import l2d.game.templates.StatsSet;

public class CPDam extends L2Skill
{
	public CPDam(StatsSet set)
	{
		super(set);
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{
		boolean ss = activeChar.getChargedSoulShot() && isSSPossible();
		if(ss)
			activeChar.unChargeShots(false);

		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = n.getValue();
			if(target.isDead())
				continue;

			target.doCounterAttack(this, activeChar);

			if(target.checkReflectSkill(activeChar, this))
				target = activeChar;

			if(target.isCurrentCpZero())
				continue;

			double damage = _power * target.getCurrentCp();

			if(damage < 1)
				damage = 1;

			target.reduceCurrentHp(damage, activeChar, this, true, true, false, true);

			getEffects(activeChar, target, getActivateRate() > 0, false);
		}
	}
}