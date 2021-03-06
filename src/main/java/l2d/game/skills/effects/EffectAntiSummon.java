package l2d.game.skills.effects;

import l2d.game.model.L2Effect;
import l2d.game.model.L2Summon;
import l2d.game.skills.Env;

public final class EffectAntiSummon extends L2Effect
{
	public EffectAntiSummon(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.AntiSummon;
	}

	@Override
	public void onStart()
	{
		if(getEffected().isPlayer())
		{
			if(getEffected().getPet() != null)
				getEffected().getPet().unSummon();
		}
		else if(getEffected().isSummon())
			((L2Summon) getEffected()).unSummon();
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}