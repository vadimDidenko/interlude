package ai;

import l2d.ext.scripts.Functions;
import l2d.game.ai.DefaultAI;
import l2d.game.model.L2Character;
import l2d.game.model.instances.L2NpcInstance;
import l2d.util.Location;
import l2d.util.Rnd;

public class Turnir extends DefaultAI
{
	static final Location[] points = {
		new Location(82545, 148600, -3505, -3395),
		new Location(82410, 148277, -3505, -3395),
		new Location(82101, 148117, -3505, -3395),
		new Location(81673, 148070, -3505, -3395),
		new Location(81453, 148378, -3505, -3395),
		new Location(81432, 148792, -3505, -3395),
		new Location(81702, 149114, -3505, -3395),
		new Location(82115, 149111, -3505, -3395),
		new Location(82440, 148882, -3505, -3395),
	};
	
	private int current_point = -1;
	private long wait_timeout = 0;
	private boolean wait = false;

	public Turnir(L2Character actor)
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
					// Это паузы на определеных точках в милисекундах
					// (case 3:) Это номер точки на которой делать паузу
					// npcSayInRange - сказать в Зону
					// npcSayToAll - Сказать всем
					// npcSayToPlayer - Сказать определеному игроку
					// npcShout - просто сказать всем видимым
					// пользовать вот так например
					// Functions.npcShout(actor, "Всем лежать, у меня бомба!");
					// Использывание скиллов //
					// actor.broadcastPacket(new MagicSkillUse(actor, actor, _skillId, skilllevel, castTime, 0));
					// На примере 2025 Large Firework //
					// actor.broadcastPacket(new MagicSkillUse(actor, actor, 2025, 1, 500, 0));
					case 0:
						wait_timeout = System.currentTimeMillis() + 300000;
						Functions.npcShout(actor, "Tournament Registration !!!!");
						wait = true;
						return true;
					case 2:
						wait_timeout = System.currentTimeMillis() + 300000;
						Functions.npcShout(actor, "Tournament Registration !!!!");
						wait = true;
						return true;
					case 4:
						wait_timeout = System.currentTimeMillis() + 300000;
						Functions.npcShout(actor, "Tournament Registration !!!");
						wait = true;
						return true;
					case 6:
						wait_timeout = System.currentTimeMillis() + 300000;
						Functions.npcShout(actor, "Tournament Registration !!!!");
						wait = true;
						return true;
					case 8:
						wait_timeout = System.currentTimeMillis() + 300000;
						Functions.npcShout(actor, "Tournament Registration !!!!");
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