package l2d.game.skills.conditions;

import l2d.game.model.L2Character;
import l2d.game.model.L2Player;
import l2d.game.skills.Env;

class ConditionPlayerBaseStats extends Condition
{
	private final BaseStat _stat;

	private final byte _value;

	ConditionPlayerBaseStats(L2Character player, BaseStat stat, byte value)
	{
		super();
		_stat = stat;
		_value = value;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(!env.character.isPlayer())
			return false;
		L2Player player = (L2Player) env.character;
		switch(_stat)
		{
			case Int:
				return player.getINT() >= _value;
			case Str:
				return player.getSTR() >= _value;
			case Con:
				return player.getCON() >= _value;
			case Dex:
				return player.getDEX() >= _value;
			case Men:
				return player.getMEN() >= _value;
			case Wit:
				return player.getWIT() >= _value;
		}
		return false;
	}
}

enum BaseStat
{
	Int,
	Str,
	Con,
	Dex,
	Men,
	Wit
}