package l2d.game.skills.effects;

import javolution.util.FastList;
import javolution.util.FastTable;
import l2d.game.model.L2Character;
import l2d.game.model.L2Effect;
import l2d.game.model.L2Skill;
import l2d.game.model.L2Skill.SkillType;
import l2d.game.skills.Env;

public final class EffectRecoverForce extends L2Effect
{
	public EffectRecoverForce(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public EffectType getEffectType()
	{
		return L2Effect.EffectType.Buff;
	}

	@Override
	public void onStart()
	{
		if(getEffected().isPlayer())
		{
			FastTable<L2Skill> skills = getEffected().getSkillsByType(SkillType.CHARGE);
			if(skills.size() > 0 && skills.getFirst() != null)
				skills.getFirst().useSkill(getEffected(), new FastList<L2Character>());
		}
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}