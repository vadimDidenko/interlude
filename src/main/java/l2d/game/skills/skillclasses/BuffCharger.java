package l2d.game.skills.skillclasses;

import javolution.util.FastList;
import l2d.game.model.L2Character;
import l2d.game.model.L2Effect;
import l2d.game.model.L2Skill;
import l2d.game.tables.SkillTable;
import l2d.game.templates.StatsSet;

public class BuffCharger extends L2Skill
{
	private int _target;

	public BuffCharger(StatsSet set)
	{
		super(set);
		_target = set.getInteger("targetBuff", 0);
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{
		for(L2Character target : targets)
		{
			int level = 0;
			FastList<L2Effect> el = target.getEffectList().getEffectsBySkillId(_target);
			if(el != null)
				level = el.getFirst().getSkill().getLevel();

			L2Skill next = SkillTable.getInstance().getInfo(_target, level + 1);
			if(next != null)
				next.getEffects(activeChar, target, false, false);
		}
	}
}