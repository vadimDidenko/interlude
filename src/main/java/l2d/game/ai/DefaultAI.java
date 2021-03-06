package l2d.game.ai;

import static l2d.game.ai.CtrlIntention.AI_INTENTION_ACTIVE;
import static l2d.game.ai.CtrlIntention.AI_INTENTION_ATTACK;
import static l2d.game.ai.CtrlIntention.AI_INTENTION_IDLE;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import javolution.util.FastMap;
import l2d.Config;
import l2d.game.ThreadPoolManager;
import l2d.game.geodata.GeoEngine;
import l2d.game.model.L2Character;
import l2d.game.model.L2Character.HateInfo;
import l2d.game.model.L2Playable;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.L2Skill.SkillType;
import l2d.game.model.L2Spawn;
import l2d.game.model.L2Zone.ZoneType;
import l2d.game.model.entity.SevenSigns;
import l2d.game.model.instances.L2ChestInstance;
import l2d.game.model.instances.L2FestivalMonsterInstance;
import l2d.game.model.instances.L2MinionInstance;
import l2d.game.model.instances.L2MonsterInstance;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.model.instances.L2NpcInstance.AggroInfo;
import l2d.game.model.instances.L2TamedBeastInstance;
import l2d.game.model.quest.QuestEventType;
import l2d.game.model.quest.QuestState;
import l2d.game.serverpackets.MagicSkillUse;
import l2d.game.skills.Stats;
import l2d.game.tables.TerritoryTable;
import l2d.game.templates.StatsSet;
import l2d.util.Location;
import l2d.util.Rnd;

public class DefaultAI extends L2CharacterAI implements Runnable
{
	protected static Logger _log = Logger.getLogger(DefaultAI.class.getName());
	
	public static enum TaskType
	{
		MOVE,
		ATTACK,
		CAST,
		BUFF
	}

	public static class Task
	{
		public TaskType type;
		public L2Skill skill;
		public L2Character target;
		public Location loc;
		public int weight = TaskDefaultWeight;
	}

	private static class TaskComparator implements Comparator<Task>
	{
		@Override
		public int compare(Task o1, Task o2)
		{
			return o2.weight - o1.weight;
		}

		public boolean equals(Task source, Task target)
		{
			return false;
		}
	}

	private static final int TaskDefaultWeight = 10000;
	private static final TaskComparator task_comparator = new TaskComparator();

	public class Teleport implements Runnable
	{
		Location _destination;

		public Teleport(Location destination)
		{
			_destination = destination;
		}

		@Override
		public void run()
		{
			L2NpcInstance actor = getActor();
			if(actor != null)
				actor.teleToLocation(_destination);
		}
	}

	public class RunningTask implements Runnable
	{
		@Override
		public void run()
		{
			L2NpcInstance actor = getActor();
			if(actor != null)
				actor.setRunning();
			_runningTask = null;
		}
	}

	protected StatsSet this_params = null;
	protected int AI_TASK_DELAY = Config.AI_TASK_DELAY;
	protected final int MAX_PURSUE_RANGE;

	/** The L2NpcInstance AI task executed every 1s (call onEvtThink method)*/
	protected Future<?> _aiTask;

	protected ScheduledFuture<?> _runningTask;

	/** The flag used to indicate that a thinking action is in progress */
	private boolean _thinking = false;

	/** The L2NpcInstance aggro counter */
	protected int _globalAggro;

	protected long _randomAnimationEnd;
	protected int _pathfind_fails;

	/** Список заданий */
	protected TreeSet<Task> _task_list = new TreeSet<Task>(task_comparator);

	/** Показывает, есть ли задания */
	protected boolean _def_think = false;

	public final L2Skill[] _dam_skills, _dot_skills, _debuff_skills;
	public final L2Skill[] _heal, _buff, _stun;

	public void addTaskMove(Location loc)
	{
		Task task = new Task();
		task.type = TaskType.MOVE;
		task.loc = loc;
		_task_list.add(task);
		_def_think = true;
	}

	public void addTaskMove(int locX, int locY, int locZ)
	{
		addTaskMove(new Location(locX, locY, locZ));
	}

	public DefaultAI(L2Character actor)
	{
		super(actor);

		_globalAggro = -10; // 10 seconds timeout of ATTACK after respawn
		L2NpcInstance thisActor = (L2NpcInstance) actor;
		thisActor.setAttackTimeout(Long.MAX_VALUE);

		_dam_skills = thisActor.getTemplate().getDamageSkills();
		_dot_skills = thisActor.getTemplate().getDotSkills();
		_debuff_skills = thisActor.getTemplate().getDebuffSkills();

		_heal = thisActor.getTemplate().getSkillsByType(SkillType.HEAL);
		_buff = thisActor.getTemplate().getSkillsByType(SkillType.BUFF);
		_stun = thisActor.getTemplate().getSkillsByType(SkillType.STUN);
		// TODO SkillType.HEAL_PERCENT / SkillType.HOT

		// Preload some AI params
		MAX_PURSUE_RANGE = getInt("MaxPursueRange", actor.isRaid() ? Config.MAX_PURSUE_RANGE_RAID : Config.MAX_PURSUE_RANGE);
		thisActor.minFactionNotifyInterval = getInt("FactionNotifyInterval", thisActor.minFactionNotifyInterval);
	}

	@Override
	public void run()
	{
		onEvtThink();
	}

	@Override
	public void startAITask()
	{
		if(_aiTask == null)
		{
			_aiTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(this, Rnd.get(AI_TASK_DELAY), AI_TASK_DELAY);
			setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
		}
	}

	@Override
	public void stopAITask()
	{
		if(_aiTask != null)
		{
			_aiTask.cancel(false);
			_aiTask = null;
			setIntention(CtrlIntention.AI_INTENTION_IDLE);
		}
	}

	/**
	 * Определяет, может ли этот тип АИ видеть персонажей в режиме Silent Move.
	 * @param target L2Playable цель
	 * @return true если цель не видна в режиме Silent Move
	 */
	@Override
	public boolean isSilentMoveNotVisible(L2Playable target)
	{
		if(getBool("isSilentMoveVisible", false))
			return false;
		return target.isSilentMoving();
	}

	@Override
	public void checkAggression(L2Character target)
	{
		L2NpcInstance actor = getActor();
		if(actor == null)
			return;
		if(getIntention() != AI_INTENTION_ACTIVE || _globalAggro < 0)
			return;
		if(actor instanceof L2FestivalMonsterInstance && !target.getPlayer().isFestivalParticipant())
			return;
		if(!actor.isAggressive() || actor.getDistance(target) > actor.getAggroRange() || Math.abs(actor.getZ() - target.getZ()) > 200)
			return;
		if(target instanceof L2Playable && isSilentMoveNotVisible((L2Playable) target))
			return;
		if(actor.getFactionId().equalsIgnoreCase("varka_silenos_clan") && target.getPlayer().getVarka() > 0)
			return;
		if(actor.getFactionId().equalsIgnoreCase("ketra_orc_clan") && target.getPlayer().getKetra() > 0)
			return;
		if(target.isInZonePeace())
			return;
		if(target.isFollow && target.getFollowTarget().isPlayer() && !((L2Player) target.getFollowTarget()).isSilentMoving())
			return;
		if(target.isPlayer() && ((L2Player) target).getPlayerAccess().IsGM && ((L2Player) target).isInvisible())
			return;
		if(!actor.canAttackCharacter(target))
			return;
		if(!GeoEngine.canSeeTarget(actor, target, false))
			return;

		target.addDamageHate(actor, 0, 2);

		if((target.isSummon() || target.isPet()) && target.getPlayer() != null)
			target.getPlayer().addDamageHate(actor, 0, 1);

		startRunningTask(2000);
		setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
	}

	protected boolean randomAnimation()
	{
		L2NpcInstance actor = getActor();
		if(actor != null && !actor.isMoving && actor.hasRandomAnimation() && Rnd.chance(Config.RND_ANIMATION_RATE))
		{
			// Анимации мобов
			_randomAnimationEnd = System.currentTimeMillis();
			actor.onRandomAnimation();
			return true;
		}
		return false;
	}

	protected boolean randomWalk()
	{
		if(getBool("noRandomWalk", false))
			return false;
		L2Character actor = getActor();
		return actor != null && !actor.isMoving && maybeMoveToHome();
	}

	/**
	 * @return true если действие выполнено, false если нет
	 */
	protected boolean thinkActive()
	{
		L2NpcInstance actor = getActor();
		if(actor == null || actor.isDead())
			return true;

		// Update every 1s the _globalAggro counter to come close to 0
		if(_globalAggro < 0)
			_globalAggro++;
		else if(_globalAggro > 0)
			_globalAggro--;

		if(_randomAnimationEnd > System.currentTimeMillis())
			return true;

		if(_def_think)
		{
			if(doTask())
				clearTasks();
			return true;
		}

		// If this is a festival monster or chest, then it remains in the same location
		if(actor instanceof L2FestivalMonsterInstance || actor instanceof L2ChestInstance || actor instanceof L2TamedBeastInstance)
			return false;

		if(actor instanceof L2MinionInstance)
		{
			L2MonsterInstance leader = ((L2MinionInstance) actor).getLeader();
			if(leader == null)
				return false;
			double distance = actor.getDistance(leader.getX(), leader.getY());
			if(distance > 1000)
				actor.teleToLocation(leader.getMinionPosition());
			else if(distance > 200)
				actor.moveToLocation(leader.getMinionPosition(), 0, true);
			return false;
		}

		if(randomAnimation())
			return true;

		if(randomWalk())
			return true;

		return false;
	}

	@Override
	protected void onIntentionActive()
	{
		L2NpcInstance actor = getActor();
		if(actor != null)
			actor.setAttackTimeout(Long.MAX_VALUE);
		super.onIntentionActive();
	}

	protected boolean checkTarget(L2Character target, boolean canSelf, int range)
	{
		L2NpcInstance actor = getActor();
		if(actor == null || target == null || target == actor && !canSelf || target.isAlikeDead() || !actor.isInRange(target, range))
			return true;

		if(actor.isConfused())
			return false;

		if(!canSelf && target.getHateList().get(actor) == null)
			return true;

		/**
		if(actor.getAttackTimeout() < System.currentTimeMillis())
		{
			if(actor.isRunning() && actor.getAggroListSize() == 1)
			{
				actor.setWalking();
				actor.setAttackTimeout(MAX_ATTACK_TIMEOUT / 4 + System.currentTimeMillis());
				return false;
			}
			target.removeFromHatelist(actor);
			return true;
		}
		 */

		return false;
	}

	protected void thinkAttack()
	{
		L2NpcInstance actor = getActor();
		if(actor == null)
			return;
		Location loc = actor.getSpawnedLoc();
		if(loc.x != 0 && loc.y != 0 && !actor.isInRange(loc, getMaxPursueRange()))
		{
			teleportHome();
			return;
		}

		if(doTask() && !actor.isAttackingNow() && !actor.isCastingNow())
			createNewTask();
	}

	@Override
	protected void onEvtReadyToAct()
	{
		onEvtThink();
	}

	@Override
	protected void onEvtArrivedTarget()
	{
		onEvtThink();
	}

	protected boolean tryMoveToTarget(L2Character target)
	{
		return tryMoveToTarget(target, 0);
	}

	protected boolean tryMoveToTarget(L2Character target, int range)
	{
		L2NpcInstance actor = getActor();
		if(actor == null)
			return false;
		range = range == 0 ? actor.getPhysicalAttackRange() : Math.max(0, range);
		if(!actor.followToCharacter(target, actor.getPhysicalAttackRange(), true))
			_pathfind_fails++;

		if(_pathfind_fails >= getMaxPathfindFails() && System.currentTimeMillis() - (actor.getAttackTimeout() - getMaxAttackTimeout()) < getTeleportTimeout() && actor.isInRange(target, 2000))
		{
			_pathfind_fails = 0;
			HateInfo hate = target.getHateList().get(actor);
			if(hate == null || hate.damage < 100 && hate.hate < 100)
			{
				returnHome(true);
				return false;
			}
			actor.broadcastPacketToOthers(new MagicSkillUse(actor, actor, 2036, 1, 500, 600000));
			ThreadPoolManager.getInstance().scheduleAi(new Teleport(GeoEngine.moveCheckForAI(target.getLoc(), actor.getLoc())), 500, false);
			return false;
		}

		return true;
	}

	protected boolean maybeNextTask(Task currentTask)
	{
		// Следующее задание
		_task_list.remove(currentTask);
		// Если заданий больше нет - определить новое
		if(_task_list.size() == 0)
			return true;
		return false;
	}

	protected boolean doTask()
	{
		L2NpcInstance actor = getActor();
		if(actor == null)
			return false;

		if(_task_list.size() == 0)
		{
			clearTasks();
			return true;
		}

		Task currentTask = null;
		try
		{
			currentTask = _task_list.first();
		}
		catch(Exception e)
		{}

		if(currentTask == null)
			clearTasks();

		if(!_def_think)
			return true;

		assert currentTask != null;
		L2Character temp_attack_target = currentTask.target;

		switch(currentTask.type)
		{
			// Задание "прибежать в заданные координаты"
			case MOVE:
			{
				if(actor.isMovementDisabled() || !getIsMobile())
					return true;

				if(actor.isInRange(currentTask.loc, 100))
					return maybeNextTask(currentTask);

				if(actor.isMoving)
					return false;

				if(!actor.moveToLocation(currentTask.loc, 0, true))
				{
					clientStopMoving();
					_pathfind_fails = 0;
					actor.broadcastPacketToOthers(new MagicSkillUse(actor, actor, 2036, 1, 500, 600000));
					ThreadPoolManager.getInstance().scheduleAi(new Teleport(currentTask.loc), 500, false);
					return maybeNextTask(currentTask);
				}
			}
				break;
			// Задание "добежать - ударить"
			case ATTACK:
			{
				if(checkTarget(temp_attack_target, false, 2000))
					return true;

				setAttackTarget(temp_attack_target);

				if(actor.isMoving)
					return Rnd.chance(25);

				if(actor.getRealDistance(temp_attack_target) <= actor.getPhysicalAttackRange() + 40 && GeoEngine.canSeeTarget(actor, temp_attack_target, false))
				{
					clientStopMoving();
					_pathfind_fails = 0;
					actor.setAttackTimeout(getMaxAttackTimeout() + System.currentTimeMillis());
					actor.doAttack(temp_attack_target);
					return maybeNextTask(currentTask);
				}

				if(actor.isMovementDisabled() || !getIsMobile())
					return true;

				tryMoveToTarget(temp_attack_target);
			}
				break;
			// Задание "добежать - атаковать скиллом"
			case CAST:
			{
				if(actor.isMuted() && currentTask.skill.isMagic() || actor.isPMuted() && !currentTask.skill.isMagic() || actor.isAMuted() || actor.isSkillDisabled(currentTask.skill.getId()))
					return true;

				boolean isAoE = currentTask.skill.getTargetType() == L2Skill.SkillTargetType.TARGET_AURA;

				if(checkTarget(temp_attack_target, false, 3000))
					return true;

				setAttackTarget(temp_attack_target);

				int castRange = isAoE ? Math.max(150, currentTask.skill.getSkillRadius() - 100) : currentTask.skill.getCastRange();

				if(actor.getRealDistance(temp_attack_target) <= castRange + 60 && GeoEngine.canSeeTarget(actor, temp_attack_target, false))
				{
					clientStopMoving();
					_pathfind_fails = 0;
					actor.setAttackTimeout(getMaxAttackTimeout() + System.currentTimeMillis());
					actor.doCast(currentTask.skill, isAoE ? actor : temp_attack_target, !(temp_attack_target instanceof L2Playable));
					return maybeNextTask(currentTask);
				}

				if(actor.isMoving)
					return Rnd.chance(10);

				if(actor.isMovementDisabled() || !getIsMobile())
					return true;

				tryMoveToTarget(temp_attack_target, castRange);
			}
				break;
			// Задание "добежать - применить скилл"
			case BUFF:
			{
				if(actor.isMuted() && currentTask.skill.isMagic() || actor.isPMuted() && !currentTask.skill.isMagic() || actor.isAMuted() || actor.isSkillDisabled(currentTask.skill.getId()))
					return true;

				if(temp_attack_target == null || temp_attack_target.isAlikeDead() || !actor.isInRange(temp_attack_target, 2000))
					return true;

				boolean isAoE = currentTask.skill.getTargetType() == L2Skill.SkillTargetType.TARGET_AURA;
				int castRange = isAoE ? Math.max(150, currentTask.skill.getSkillRadius() - 100) : currentTask.skill.getCastRange();

				if(actor.isMoving)
					return Rnd.chance(10);

				if(actor.getRealDistance(temp_attack_target) <= castRange + 60 && GeoEngine.canSeeTarget(actor, temp_attack_target, false))
				{
					clientStopMoving();
					_pathfind_fails = 0;
					actor.doCast(currentTask.skill, isAoE ? actor : temp_attack_target, !(temp_attack_target instanceof L2Playable));
					return maybeNextTask(currentTask);
				}

				if(actor.isMovementDisabled() || !getIsMobile())
					return true;

				tryMoveToTarget(temp_attack_target);
			}
				break;
		}

		return false;
	}

	protected boolean createNewTask()
	{
		return false;
	}

	protected boolean defaultNewTask()
	{
		clearTasks();

		L2NpcInstance actor = getActor();
		L2Character target;
		if(actor == null || (target = prepareTarget()) == null)
			return false;

		double distance = actor.getDistance(target);
		return chooseTaskAndTargets(null, target, distance);
	}

	@Override
	protected void onIntentionAttack(L2Character target)
	{
		// Удаляем все задания
		clearTasks();

		setAttackTarget(target);
		changeIntention(AI_INTENTION_ATTACK, target, null);
		clientStopMoving();
		onEvtThink();
	}

	@Override
	protected void onEvtThink()
	{
		L2NpcInstance actor = getActor();
		if(_thinking || actor == null || actor.isActionsDisabled() || actor.isAfraid() || actor.isDead())
			return;

		if(_randomAnimationEnd > System.currentTimeMillis())
			return;

		if(actor.isRaid() && (actor.isInZonePeace() || actor.isInZone(ZoneType.battle_zone) || actor.isInZone(ZoneType.Siege)))
			teleportHome();

		_thinking = true;
		try
		{
			if(getIntention() == AI_INTENTION_ACTIVE || getIntention() == AI_INTENTION_IDLE)
				thinkActive();
			else if(getIntention() == AI_INTENTION_ATTACK)
				thinkAttack();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			_thinking = false;
		}
	}

	@Override
	protected void onEvtDead()
	{
		stopAITask();

		L2NpcInstance actor = getActor();
		if(actor != null)
			actor.clearAggroList(false);

		// 10 seconds timeout of ATTACK after respawn
		_globalAggro = -10;

		if(actor != null)
			actor.setAttackTimeout(Long.MAX_VALUE);

		// Удаляем все задания
		clearTasks();

		super.onEvtDead();
	}

	@Override
	protected void onEvtClanAttacked(L2Character attacked_member, L2Character attacker, int damage)
	{
		if(getIntention() != AI_INTENTION_ACTIVE || !isGlobalAggro())
			return;
		L2NpcInstance actor = getActor();
		if(actor == null || !actor.isInRange(attacked_member, actor.getFactionRange()))
			return;
		if(Math.abs(attacker.getZ() - actor.getZ()) > 400)
			return;

		if(GeoEngine.canSeeTarget(actor, attacked_member, false))
			notifyEvent(CtrlEvent.EVT_AGGRESSION, new Object[] { attacker, attacker.isSummon() ? damage : 3 });
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage)
	{
		L2NpcInstance actor = getActor();
		if(attacker == null || actor == null)
			return;

		if(!actor.canAttackCharacter(attacker))
			return;

		L2Player player = attacker.getPlayer();
		if(player != null)
		{
			ArrayList<QuestState> quests = player.getQuestsForEvent(actor, QuestEventType.MOBGOTATTACKED);
			if(quests != null)
				for(QuestState qs : quests)
					qs.getQuest().notifyAttack(actor, qs);
		}

		actor.callFriends(attacker, damage);
	}

	@Override
	protected void onEvtAggression(L2Character attacker, int aggro)
	{
		if(attacker == null || attacker.getPlayer() == null)
			return;

		L2NpcInstance actor = getActor();
		if(actor == null || !actor.canAttackCharacter(attacker))
			return;

		L2Player player = attacker.getPlayer();

		if(getIntention() != CtrlIntention.AI_INTENTION_ATTACK && (SevenSigns.getInstance().isSealValidationPeriod() || SevenSigns.getInstance().isCompResultsPeriod()) && actor.isSevenSignsMonster())
		{
			int pcabal = SevenSigns.getInstance().getPlayerCabal(player);
			int wcabal = SevenSigns.getInstance().getCabalHighestScore();
			if(pcabal != wcabal && wcabal != SevenSigns.CABAL_NULL)
			{
				player.sendMessage("You have been teleported to the nearest town because you not signed for winning cabal.");
				player.teleToClosestTown();
				return;
			}
		}

		actor.setAttackTimeout(getMaxAttackTimeout() + System.currentTimeMillis());
		_globalAggro = 0;

		attacker.addDamageHate(actor, 0, aggro);

		// 1 хейт добавляется хозяину суммона, чтобы после смерти суммона моб накинулся на хозяина.
		if(player != null && aggro > 0 && attacker.getPlayer() != null && (attacker.isSummon() || attacker.isPet()))
			attacker.getPlayer().addDamageHate(actor, 0, 1);

		if(!actor.isRunning())
			startRunningTask(1000);

		if(getIntention() != CtrlIntention.AI_INTENTION_ATTACK)
		{
			// Показываем анимацию зарядки шотов, если есть таковые.
			switch(actor.getTemplate().shots)
			{
				case SOUL:
					actor.unChargeShots(false);
					break;
				case SPIRIT:
				case BSPIRIT:
					actor.unChargeShots(true);
					break;
				case SOUL_SPIRIT:
				case SOUL_BSPIRIT:
					actor.unChargeShots(false);
					actor.unChargeShots(true);
					break;
			}

			setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
		}
	}

	protected boolean maybeMoveToHome()
	{
		L2NpcInstance actor = getActor();
		if(actor == null)
			return false;

		boolean randomWalk = actor.hasRandomWalk();
		Location sloc = actor.getSpawnedLoc();
		if(sloc == null)
			return false;

		// Random walk or not?
		if(randomWalk && (!Config.RND_WALK || !Rnd.chance(Config.RND_WALK_RATE)))
			return false;

		if(!randomWalk && actor.isInRangeZ(sloc, Config.MAX_DRIFT_RANGE))
			return false;

		int x = sloc.x + Rnd.get(-Config.MAX_DRIFT_RANGE, Config.MAX_DRIFT_RANGE);
		int y = sloc.y + Rnd.get(-Config.MAX_DRIFT_RANGE, Config.MAX_DRIFT_RANGE);
		int z = GeoEngine.getHeight(x, y, sloc.z);

		if(sloc.z - z > 200)
			return false;

		L2Spawn spawn = actor.getSpawn();
		if(spawn != null && spawn.getLocation() != 0 && !TerritoryTable.getInstance().getLocation(spawn.getLocation()).isInside(x, y))
			return false;

		actor.setWalking();

		if(!actor.moveToLocation(x, y, z, 0, true))
			teleportHome();

		return true;
	}

	public void returnHome(boolean clearAggro)
	{
		L2NpcInstance actor = getActor();
		if(actor == null)
			return;

		if(clearAggro)
			actor.clearAggroList(true);
		else
			actor.setRunning();

		setIntention(AI_INTENTION_ACTIVE);

		// Удаляем все задания
		clearTasks();

		// Прибежать в заданную точку и переключиться в состояние AI_INTENTION_ACTIVE
		Task task = new Task();
		task.type = TaskType.MOVE;
		task.loc = actor.getSpawnedLoc();
		_task_list.add(task);
		_def_think = true;
	}

	@Override
	public void teleportHome()
	{
		L2NpcInstance actor = getActor();
		if(actor == null)
			return;

		actor.clearAggroList(true);
		setIntention(AI_INTENTION_ACTIVE);

		// Удаляем все задания
		clearTasks();

		Location sloc = actor.getSpawnedLoc();
		if(sloc != null)
		{
			actor.broadcastPacketToOthers(new MagicSkillUse(actor, actor, 2036, 1, 500, 0));
			actor.teleToLocation(sloc.x, sloc.y, GeoEngine.getHeight(sloc));
		}
	}

	protected L2Character prepareTarget()
	{
		L2NpcInstance actor = getActor();
		if(actor == null)
			return null;
		// Новая цель исходя из агрессивности
		L2Character hated = actor.isConfused() && getAttackTarget() != actor ? getAttackTarget() : actor.getMostHated();
		if(hated != null && hated != actor)
		{
			setAttackTarget(hated);
			return hated;
		}
		returnHome(false);
		return null;
	}

	protected boolean canUseSkill(L2Skill sk, L2Character target, double distance)
	{
		L2NpcInstance actor = getActor();

		if(actor == null || sk == null || actor.isSkillDisabled(sk.getId()))
			return false;

		double mpConsume2 = sk.getMpConsume2();
		if(sk.isMagic())
			mpConsume2 = actor.calcStat(Stats.MP_MAGIC_SKILL_CONSUME, mpConsume2, null, sk);
		else
			mpConsume2 = actor.calcStat(Stats.MP_PHYSICAL_SKILL_CONSUME, mpConsume2, null, sk);

		if(Rnd.chance(Config.СHANCE_SKILLS))
			return false;
		else if(actor.getCurrentMp() < mpConsume2)
			return false;

		if(sk.isMagic())
		{
			if(actor.isMuted())
				return false;
		}
		else if(actor.isPMuted())
			return false;
		if(distance > 0)
		{
			if(sk.getCastRange() > 200 && distance <= 200)
				return false;
			if(sk.getCastRange() <= 200 && distance > 200)
				return false;
		}
		if(target.getEffectList().getEffectsBySkill(sk) != null)
			return false;
		return true;
	}

	protected boolean canUseSkill(L2Skill sk, L2Character target)
	{
		return canUseSkill(sk, target, 0);
	}

	@Deprecated
	protected void addSkillToUse(ArrayList<L2Skill> skill_list, L2Character target, double distance, L2Skill[] skills_for_use)
	{
		ArrayList<L2Skill> t_skill = new ArrayList<L2Skill>();
		for(L2Skill sk : skills_for_use)
			if(canUseSkill(sk, target, distance))
				t_skill.add(sk);
		if(t_skill.size() > 0)
			skill_list.add(t_skill.get(Rnd.get(t_skill.size())));
	}

	protected void addDesiredSkill(Map<L2Skill, Integer> skill_list, L2Character target, double distance, L2Skill[] skills_for_use)
	{
		if(skills_for_use == null || skills_for_use.length == 0 || target == null)
			return;
		for(L2Skill sk : skills_for_use)
			addDesiredSkill(skill_list, target, distance, sk);
	}

	protected void addDesiredSkill(Map<L2Skill, Integer> skill_list, L2Character target, double distance, L2Skill skill_for_use)
	{
		if(skill_for_use == null || target == null || !canUseSkill(skill_for_use, target))
			return;
		int weight = (int) -Math.abs(skill_for_use.getCastRange() - distance);
		if(skill_for_use.getCastRange() >= distance)
			weight += 1000000;
		else if(skill_for_use.isNotTargetAoE() && skill_for_use.getTargets(getActor(), target, false).size() == 0)
			return;
		skill_list.put(skill_for_use, weight);
	}

	protected void addDesiredHeal(Map<L2Skill, Integer> skill_list, L2Skill[] skills_for_use)
	{
		L2NpcInstance actor = getActor();
		if(actor == null || skills_for_use == null || skills_for_use.length == 0)
			return;
		double hp_reduced = actor.getMaxHp() - actor.getCurrentHp();
		double hp_precent = actor.getCurrentHpPercents();
		if(hp_reduced < 1)
			return;
		int weight;
		for(L2Skill sk : skills_for_use)
			if(canUseSkill(sk, actor) && sk.getPower() <= hp_reduced)
			{
				weight = (int) sk.getPower();
				if(hp_precent < 50)
					weight += 1000000;
				skill_list.put(sk, weight);
			}
	}

	protected L2Skill selectTopSkill(Map<L2Skill, Integer> skill_list)
	{
		if(skill_list == null || skill_list.size() == 0)
			return null;
		int next_weight, top_weight = Integer.MIN_VALUE;
		for(L2Skill next : skill_list.keySet())
			if((next_weight = skill_list.get(next)) > top_weight)
				top_weight = next_weight;
		if(top_weight == Integer.MIN_VALUE)
			return null;
		for(L2Skill next : skill_list.keySet())
			if(skill_list.get(next) < top_weight)
				skill_list.remove(next);
		next_weight = skill_list.size();
		return skill_list.keySet().toArray(new L2Skill[next_weight])[Rnd.get(next_weight)];
	}

	protected boolean chooseTaskAndTargets(L2Skill r_skill, L2Character target, double distance)
	{
		L2NpcInstance actor = getActor();
		if(actor == null)
			return true;
		// Использовать скилл если можно, иначе атаковать
		if(r_skill != null && !(actor.isMuted() && r_skill.isMagic() || actor.isPMuted() && !r_skill.isMagic() || actor.isAMuted()))
		{
			// Проверка цели, и смена если необходимо
			if(r_skill.getTargetType() == L2Skill.SkillTargetType.TARGET_SELF)
				target = actor;
			//else if(r_skill.getTargetType() == L2Skill.SkillTargetType.TARGET_AURA)
			//	target = actor;
			else if(actor.isMovementDisabled() && r_skill.isOffensive() && distance > r_skill.getCastRange() + 60)
			{
				ArrayList<L2Playable> targets = new ArrayList<L2Playable>();
				for(AggroInfo ai : actor.getAggroList().values())
					if(ai.attacker != null && actor.getDistance(ai.attacker) <= r_skill.getCastRange() + 60)
						targets.add(ai.attacker);
				if(targets.size() > 0)
					target = targets.get(Rnd.get(targets.size()));
			}

			// Добавить новое задание
			Task task = new Task();
			task.type = r_skill.isOffensive() ? TaskType.CAST : TaskType.BUFF;
			task.target = target;
			task.skill = r_skill;
			_task_list.add(task);
			_def_think = true;
			return true;
		}

		// Смена цели, если необходимо
		if(actor.isMovementDisabled() && distance > actor.getPhysicalAttackRange() + 40)
		{
			ArrayList<L2Playable> targets = new ArrayList<L2Playable>();
			for(AggroInfo ai : actor.getAggroList().values())
				if(ai.attacker != null && actor.getDistance(ai.attacker) <= actor.getPhysicalAttackRange() + 40)
					targets.add(ai.attacker);
			if(targets.size() > 0)
				target = targets.get(Rnd.get(targets.size()));
		}

		// Добавить новое задание
		Task task = new Task();
		task.type = TaskType.ATTACK;
		task.target = target;
		_task_list.add(task);
		_def_think = true;
		return true;
	}

	@Override
	public boolean isActive()
	{
		return _aiTask != null;
	}

	@Override
	public void clearTasks()
	{
		_def_think = false;
		_task_list.clear();
	}

	/**
	 * Шедюлит мобу переход в режим бега через определенный интервал времени
	 * @param interval
	 */
	public void startRunningTask(int interval)
	{
		L2NpcInstance actor = getActor();
		if(actor != null && _runningTask == null && !actor.isRunning())
			_runningTask = ThreadPoolManager.getInstance().scheduleAi(new RunningTask(), interval, false);
	}

	@Override
	public boolean isGlobalAggro()
	{
		return _globalAggro >= 0;
	}

	@Override
	public void setGlobalAggro(int value)
	{
		_globalAggro = value;
	}

	private StatsSet getAIParams()
	{
		if(this_params != null)
			return this_params;
		L2NpcInstance actor = getActor();
		if(actor != null && actor.getTemplate().getAIParams() != null)
			return actor.getTemplate().getAIParams();
		this_params = new StatsSet();
		return this_params;
	}

	private StatsSet getThisAIParams()
	{
		if(this_params == null)
		{
			L2NpcInstance actor = getActor();
			this_params = actor == null || actor.getTemplate().getAIParams() == null ? new StatsSet() : actor.getTemplate().getAIParams().clone();
		}
		return this_params;
	}

	public void AddUseSkillDesire(L2Character target, L2Skill skill, int weight)
	{
		Task task = new Task();
		task.type = skill.isOffensive() ? TaskType.CAST : TaskType.BUFF;
		task.target = target;
		task.skill = skill;
		task.weight = weight;
		_task_list.add(task);
		_def_think = true;
	}

	public static void DebugTask(Task task)
	{
		if(task == null)
			System.out.println("NULL");
		else
		{
			System.out.print("Weight=" + task.weight);
			System.out.print("; Type=" + task.type);
			System.out.print("; Skill=" + task.skill);
			System.out.print("; Target=" + task.target);
			System.out.print("; Loc=" + task.loc);
			System.out.println();
		}
	}

	public void DebugTasks()
	{
		if(_task_list.size() == 0)
		{
			System.out.println("No Tasks");
			return;
		}

		int i = 0;
		for(Task task : _task_list)
		{
			System.out.print("Task [" + i + "]: ");
			DebugTask(task);
			i++;
		}
	}

	public boolean getBool(String name)
	{
		return getAIParams().getBool(name);
	}

	public boolean getBool(String name, boolean _defult)
	{
		return getAIParams().getBool(name, _defult);
	}

	public int getInt(String name)
	{
		return getAIParams().getInteger(name);
	}

	public int getInt(String name, int _defult)
	{
		return getAIParams().getInteger(name, _defult);
	}

	public long getLong(String name)
	{
		return getAIParams().getLong(name);
	}

	public long getLong(String name, long _defult)
	{
		return getAIParams().getLong(name, _defult);
	}

	public float getFloat(String name)
	{
		return getAIParams().getFloat(name);
	}

	public float getFloat(String name, float _defult)
	{
		return getAIParams().getFloat(name, _defult);
	}

	public String getString(String name)
	{
		return getAIParams().getString(name);
	}

	public String getString(String name, String _defult)
	{
		return getAIParams().getString(name, _defult);
	}

	public void set(String name, boolean value)
	{
		getThisAIParams().set(name, value);
	}

	public void set(String name, int value)
	{
		getThisAIParams().set(name, value);
	}

	public void set(String name, long value)
	{
		getThisAIParams().set(name, value);
	}

	public void set(String name, double value)
	{
		getThisAIParams().set(name, value);
	}

	public void set(String name, String value)
	{
		getThisAIParams().set(name, value);
	}

	@Override
	public L2NpcInstance getActor()
	{
		return (L2NpcInstance) super.getActor();
	}

	protected boolean defaultThinkBuff(int rateSelf)
	{
		return defaultThinkBuff(rateSelf, 0);
	}

	protected boolean defaultThinkBuff(int rateSelf, int rateFriends)
	{
		L2NpcInstance actor = getActor();
		if(actor == null || actor.isDead())
			return true;

		//TODO сделать более разумный выбор баффа, сначала выбирать подходящие а потом уже рандомно 1 из них
		if(_buff.length > 0)
		{
			if(Rnd.chance(rateSelf))
			{
				L2Skill r_skill = _buff[Rnd.get(_buff.length)];
				if(actor.getEffectList().getEffectsBySkill(r_skill) == null)
				{
					// Добавить новое задание
					Task task = new Task();
					task.type = TaskType.BUFF;
					task.target = actor;
					task.skill = r_skill;
					_task_list.add(task);
					_def_think = true;
					return true;
				}
			}

			if(Rnd.chance(rateFriends))
			{
				L2Skill r_skill = _buff[Rnd.get(_buff.length)];
				double bestDistance = 1000000;
				L2NpcInstance target = null;
				for(L2NpcInstance npc : actor.ActiveFriendTargets(true, true))
					if(npc != null && npc.getEffectList().getEffectsBySkill(r_skill) == null)
					{
						double distance = actor.getDistance(npc);
						if(target == null || bestDistance > distance)
						{
							target = npc;
							bestDistance = distance;
						}

					}

				if(target != null)
				{
					// Добавить новое задание
					Task task = new Task();
					task.type = TaskType.CAST;
					task.target = target;
					task.skill = r_skill;
					_task_list.add(task);
					_def_think = true;
					return true;
				}
			}
		}

		return false;
	}

	protected boolean defaultFightTask()
	{
		clearTasks();
		L2Character target;
		if((target = prepareTarget()) == null)
			return false;

		L2NpcInstance actor = getActor();
		if(actor == null || actor.isDead())
			return false;

		double distance = actor.getDistance(target);

		if(actor.isAMuted() || Rnd.chance(getRatePHYS()))
			return chooseTaskAndTargets(null, target, distance);

		FastMap<L2Skill, Integer> d_skill = new FastMap<L2Skill, Integer>(); //TODO class field ? 

		double target_hp_precent = target.getCurrentHpPercents();
		int rnd_per = Rnd.get(100);

		if(rnd_per < getRateDOT() && target_hp_precent > 25)
			addDesiredSkill(d_skill, target, distance, _dot_skills);

		if(rnd_per < getRateDEBUFF() && target_hp_precent > 25)
			addDesiredSkill(d_skill, target, distance, _debuff_skills);

		if(rnd_per < getRateDAM())
			addDesiredSkill(d_skill, target, distance, _dam_skills);

		if(rnd_per < getRateSTUN())
			addDesiredSkill(d_skill, target, distance, _stun);

		if(rnd_per < getRateHEAL() || d_skill.size() == 0)
			addDesiredHeal(d_skill, _heal);

		// Выбрать 1 скилл из полученных
		L2Skill r_skill = selectTopSkill(d_skill);

		// Смена цели для HEAL скиллов
		if(r_skill != null && r_skill.getSkillType() == SkillType.HEAL)
			target = actor;

		return chooseTaskAndTargets(r_skill, target, distance);
	}

	public int getRatePHYS()
	{
		return 100;
	}

	public int getRateDOT()
	{
		return 0;
	}

	public int getRateDEBUFF()
	{
		return 0;
	}

	public int getRateDAM()
	{
		return 0;
	}

	public int getRateSTUN()
	{
		return 0;
	}

	public int getRateHEAL()
	{
		return 0;
	}

	public boolean getIsMobile()
	{
		return true;
	}

	protected int getMaxPursueRange()
	{
		return MAX_PURSUE_RANGE;
	}

	public int getMaxPathfindFails()
	{
		return 10;
	}

	public int getMaxAttackTimeout()
	{
		return 20000;
	}

	public int getTeleportTimeout()
	{
		return 10000;
	}
}