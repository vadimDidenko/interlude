package l2d.game.ai;

import java.util.concurrent.ConcurrentHashMap;

import l2d.game.instancemanager.SiegeManager;
import l2d.game.model.L2Character;
import l2d.game.model.L2Clan;
import l2d.game.model.L2Playable;
import l2d.game.model.instances.L2NpcInstance.AggroInfo;
import l2d.game.model.instances.L2SiegeGuardInstance;
import l2d.util.Location;
import l2d.util.Rnd;

public abstract class SiegeGuard extends DefaultAI
{
	public SiegeGuard(L2Character actor)
	{
		super(actor);
	}

	@Override
	public int getMaxPathfindFails()
	{
		return Integer.MAX_VALUE;
	}

	@Override
	public int getMaxAttackTimeout()
	{
		return Integer.MAX_VALUE;
	}

	@Override
	protected boolean randomWalk()
	{
		return false;
	}

	@Override
	protected boolean randomAnimation()
	{
		return false;
	}

	@Override
	public boolean isSilentMoveNotVisible(L2Playable target)
	{
		// Осадные гварды могут видеть игроков в режиме Silent Move с вероятностью 10%
		return target.isSilentMoving() && Rnd.chance(90);
	}

	@Override
	public void checkAggression(L2Character target)
	{
		L2SiegeGuardInstance actor = getActor();
		if(actor == null || target == null || !(target instanceof L2Playable) || !actor.isAutoAttackable(target))
			return;
		super.checkAggression(target);
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage)
	{
		L2SiegeGuardInstance actor = getActor();
		if(actor != null && attacker != null && attacker.getPlayer() != null)
		{
			L2Clan clan = attacker.getPlayer().getClan();
			if(clan != null && SiegeManager.getSiege(actor, true) == clan.getSiege() && clan.isDefender())
				return;
		}
		super.onEvtAttacked(attacker, damage);
	}

	@Override
	protected void onEvtAggression(L2Character target, int aggro)
	{
		L2SiegeGuardInstance actor = getActor();
		if(actor != null && target != null && target.getPlayer() != null)
		{
			L2Clan clan = target.getPlayer().getClan();
			if(clan != null && SiegeManager.getSiege(actor, true) == clan.getSiege() && clan.isDefender())
				return;
		}
		super.onEvtAggression(target, aggro);
	}

	@Override
	protected boolean thinkActive()
	{
		if(_globalAggro < 0)
			_globalAggro++;
		else if(_globalAggro > 0)
			_globalAggro--;

		L2SiegeGuardInstance actor = getActor();
		if(actor == null || actor.isDead())
			return true;

		if(_def_think)
		{
			doTask();
			return true;
		}

		actor.setRunning();
		actor.setAttackTimeout(Long.MAX_VALUE);

		Location sloc = actor.getSpawnedLoc();
		int homeX = actor.getX() - sloc.x;
		int homeY = actor.getY() - sloc.y;

		// Проверка на расстояние до точки спауна
		if(homeX * homeX + homeY * homeY > 10000)
		{
			teleportHome();
			return true;
		}

		return false;
	}

	@Override
	protected void thinkAttack()
	{
		L2SiegeGuardInstance actor = getActor();
		if(actor == null)
			return;
		actor.setRunning();
		actor.setAttackTimeout(Long.MAX_VALUE);

		Location sloc = actor.getSpawnedLoc();
		int homeX = actor.getX() - sloc.x;
		int homeY = actor.getY() - sloc.y;

		// Проверка на расстояние до точки спауна
		if(homeX * homeX + homeY * homeY > 9000000)
		{
			teleportHome();
			return;
		}

		super.thinkAttack();
	}

	private L2Character getMostHated()
	{
		L2SiegeGuardInstance actor = getActor();
		if(actor == null)
			return null;

		ConcurrentHashMap<L2Playable, AggroInfo> aggroList = actor.getAggroList();

		AggroInfo mosthated1 = null;
		for(AggroInfo ai : aggroList.values())
			if(mosthated1 == null || mosthated1.attacker == null)
				mosthated1 = ai;
			else if(ai.attacker != null && actor.getDistance(ai.attacker) < actor.getDistance(mosthated1.attacker))
				mosthated1 = ai;

		AggroInfo mosthated2 = null;
		for(AggroInfo ai : aggroList.values())
			if(mosthated2 == null || mosthated2.attacker == null)
				mosthated2 = ai;
			else if(ai.attacker != null && mosthated2.hate < ai.hate)
				mosthated2 = ai;

		if(mosthated1 != null && mosthated2 != null)
			return Rnd.chance(50) ? mosthated1.attacker : mosthated2.attacker;

		if(mosthated1 != null)
			return mosthated1.attacker;
		if(mosthated2 != null)
			return mosthated2.attacker;
		return null;
	}

	@Override
	protected L2Character prepareTarget()
	{
		L2SiegeGuardInstance actor = getActor();
		if(actor == null)
			return null;
		// Новая цель исходя из агрессивности
		L2Character hated = getMostHated();
		if(hated != null && hated != actor)
		{
			setAttackTarget(hated);
			return hated;
		}
		returnHome(false);
		return null;
	}

	@Override
	public L2SiegeGuardInstance getActor()
	{
		return (L2SiegeGuardInstance) super.getActor();
	}
}