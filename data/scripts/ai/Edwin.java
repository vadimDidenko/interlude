package ai;

import l2d.game.ai.DefaultAI;
import l2d.game.model.L2Character;
import l2d.game.model.instances.L2NpcInstance;
import l2d.util.Location;
import l2d.util.Rnd;

public class Edwin extends DefaultAI
{
	static final Location[] points = {
		new Location(89991, -144601, -1467), // start
		new Location(90538, -143470, -1467),
		new Location(90491, -142848, -1467),
		new Location(89563, -141455, -1467),
		new Location(89138, -140621, -1467),
		new Location(87459, -140192, -1467),
		new Location(85625, -140699, -1467),
		new Location(84538, -142382, -1467),
		new Location(84527, -143913, -1467), // finish
		new Location(84538, -142382, -1467),
		new Location(85625, -140699, -1467),
		new Location(87459, -140192, -1467),
		new Location(89138, -140621, -1467),
		new Location(89563, -141455, -1467),
		new Location(90491, -142848, -1467),
		new Location(90538, -143470, -1467),
		new Location(89991, -144601, -1467)  // start
	};

	private int current_point = -1;
	private long wait_timeout = 0;
	private boolean wait = false;

	public Edwin(L2Character actor)
	{
		super(actor);
	}

	@Override
	public boolean isGlobalAI()
	{
		return true;
	}

	@Override
	protected boolean thinkActive()
	{
		L2NpcInstance actor = getActor();
		if(actor == null || actor.isDead())
			return true;

		if(_def_think)
		{
			doTask();
			return true;
		}

		if(System.currentTimeMillis() > wait_timeout && (current_point > -1 || Rnd.chance(5)))
		{
			if(!wait)
				switch(current_point)
				{
					case 0:
						wait_timeout = System.currentTimeMillis() + 10000;
						wait = true;
						return true;
					case 8:
						wait_timeout = System.currentTimeMillis() + 10000;
						wait = true;
						return true;
				}

			wait_timeout = 0;
			wait = false;
			current_point++;

			if(current_point >= points.length)
				current_point = 0;

			addTaskMove(points[current_point]);
			return true;
		}

		if(randomAnimation())
			return true;

		return false;
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage)
	{}

	@Override
	protected void onEvtAggression(L2Character target, int aggro)
	{}
}