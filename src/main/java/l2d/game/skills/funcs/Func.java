package l2d.game.skills.funcs;

import l2d.game.skills.Env;
import l2d.game.skills.Stats;
import l2d.game.skills.conditions.Condition;

/**
 * A Func object is a component of a Calculator created to manage and dynamically calculate the effect of a character property (ex : MAX_HP, REGENERATE_HP_RATE...).
 * In fact, each calculator is a table of Func object in which each Func represents a mathematic function : <BR><BR>
 * FuncAtkAccuracy -> Math.sqrt(_player.getDEX())*6+_player.getLevel()<BR><BR>
 * When the calc method of a calculator is launched, each mathematic function is called according to its priority <B>_order</B>.
 * Indeed, Func with lowest priority order is executed firsta and Funcs with the same order are executed in unspecified order.
 * The result of the calculation is stored in the value property of an Env class instance.<BR><BR>
 */

public abstract class Func
{
	/** Statistics, that is affected by this function (See L2Character.CALCULATOR_XXX constants) */
	public final Stats _stat;

	/**
	 * Order of functions calculation.
	 * Functions with lower order are executed first.
	 * Functions with the same order are executed in unspecified order.
	 * Usually add/substruct functions has lowest order,
	 * then bonus/penalty functions (multiplay/divide) are
	 * applied, then functions that do more complex calculations
	 * (non-linear functions).
	 */
	public final int _order;

	/**
	 * Owner can be an armor, weapon, skill, system event, quest, etc
	 * Used to remove all functions added by this owner.
	 */
	public final Object _funcOwner;
	protected Condition _cond;
	public final double _value;

	public Func(final Stats stat, final int order, final Object owner)
	{
		this(stat, order, owner, 0);
	}

	public Func(final Stats stat, final int order, final Object owner, final double value)
	{
		_stat = stat;
		_order = order;
		_funcOwner = owner;
		_value = value;
	}

	public void setCondition(final Condition cond)
	{
		_cond = cond;
	}

	/**
	 * Для отладки
	 */
	public Condition getCondition()
	{
		return _cond;
	}

	public abstract void calc(Env env);
}