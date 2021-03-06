package ai;

import l2d.ext.scripts.Functions;
import l2d.game.ai.DefaultAI;
import l2d.game.model.L2Character;
import l2d.game.model.instances.L2NpcInstance;
import l2d.util.Location;
import l2d.util.Rnd;

public class Leandro extends DefaultAI
{
	static final Location[] points = {
		new Location(-82428, 245204, -3720),
		new Location(-82422, 245448, -3704),
		new Location(-82080, 245401, -3720),
		new Location(-82108, 244974, -3720),
		new Location(-83595, 244051, -3728),
		new Location(-83898, 242776, -3728),
		new Location(-85966, 241371, -3728),
		new Location(-86079, 240868, -3720),
		new Location(-86076, 240392, -3712),
		new Location(-86519, 240706, -3712),
		new Location(-86343, 241130, -3720),
		new Location(-86519, 240706, -3712),
		new Location(-86076, 240392, -3712),
		new Location(-86079, 240868, -3720),
		new Location(-85966, 241371, -3728),
		new Location(-83898, 242776, -3728),
		new Location(-83595, 244051, -3728),
		new Location(-82108, 244974, -3720)
	};

	private int current_point = -1;
	private long wait_timeout = 0;
	private boolean wait = false;

	public Leandro(L2Character actor)
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
					case 5:
						wait_timeout = System.currentTimeMillis() + 30000;
						Functions.npcShout(actor, "Where has he gone?");
						wait = true;
						return true;
					case 10:
						wait_timeout = System.currentTimeMillis() + 60000;
						Functions.npcShout(actor, "Have you seen Windawood?");
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