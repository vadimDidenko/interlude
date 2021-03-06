package l2d.game.skills.effects;

import l2d.game.model.L2Character;
import l2d.game.model.L2Effect;
import l2d.game.skills.Env;

public final class EffectPetrification extends L2Effect
{
	public EffectPetrification(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		if(_effected.isParalyzeImmune())
		{
			exit();
			return;
		}
		_effected.startAbnormalEffect(L2Character.ABNORMAL_EFFECT_HOLD_2);
		_effected.setParalyzed(true);
		_effected.setIsInvul(true);
	}

	@Override
	public void onExit()
	{
		super.onExit();
		_effected.stopAbnormalEffect(L2Character.ABNORMAL_EFFECT_HOLD_2);
		_effected.setParalyzed(false);
		_effected.setIsInvul(false);
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}