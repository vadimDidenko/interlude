package ai;

import l2d.ext.listeners.DayNightChangeListener;
import l2d.ext.listeners.L2ZoneEnterLeaveListener;
import l2d.ext.listeners.PropertyCollection;
import l2d.game.GameTimeController;
import l2d.game.ai.Fighter;
import l2d.game.instancemanager.ZoneManager;
import l2d.game.model.L2Character;
import l2d.game.model.L2Object;
import l2d.game.model.L2Zone;
import l2d.game.model.L2Zone.ZoneType;
import l2d.game.model.instances.L2NpcInstance;

/**
 * Индивидуальное АИ эпик боса Zaken.<BR> - неуязвим ночью<BR> - получает 25% пенальти на регены солнечной комнате - зона zaken_sunlight_room, id: 1335
 */
public class Zaken extends Fighter implements PropertyCollection
{
	private static final L2Zone _zone = ZoneManager.getInstance().getZoneById(ZoneType.no_restart, 1335, true);
	private ZoneListener _zoneListener = new ZoneListener();
	private final float _baseHpReg;
	private final float _baseMpReg;

	public Zaken(L2Character actor)
	{
		super(actor);
		GameTimeController.getInstance().getListenerEngine().addPropertyChangeListener(GameTimeControllerDayNightChange, new NightInvulDayNightListener());
		_zone.getListenerEngine().addMethodInvokedListener(_zoneListener);
		_baseHpReg = actor.getTemplate().baseHpReg;
		_baseMpReg = actor.getTemplate().baseMpReg;
	}

	private class NightInvulDayNightListener extends DayNightChangeListener
	{
		private NightInvulDayNightListener()
		{
			if(GameTimeController.getInstance().isNowNight())
				switchToNight();
			else
				switchToDay();
		}

		/**
		 * Вызывается, когда на сервере наступает ночь
		 */
		@Override
		public void switchToNight()
		{
			L2NpcInstance actor = getActor();
			if(actor != null)
				actor.setIsInvul(true);
		}

		/**
		 * Вызывается, когда на сервере наступает день
		 */
		@Override
		public void switchToDay()
		{
			L2NpcInstance actor = getActor();
			if(actor != null)
				actor.setIsInvul(false);
		}
	}

	private class ZoneListener extends L2ZoneEnterLeaveListener
	{
		@Override
		public void objectEntered(L2Zone zone, L2Object object)
		{
			L2NpcInstance actor = getActor();
			if(actor == null)
				return;
			// Регены снижаем в любое время суток, все равно закен ночью бессмертен
			// что бы лишний раз не дергать GameTimeController
			actor.getTemplate().baseHpReg = (float) (_baseHpReg * 0.75);
			actor.getTemplate().baseMpReg = (float) (_baseMpReg * 0.75);
		}

		@Override
		public void objectLeaved(L2Zone zone, L2Object object)
		{
			L2NpcInstance actor = getActor();
			if(actor == null)
				return;
			actor.getTemplate().baseHpReg = _baseHpReg;
			actor.getTemplate().baseMpReg = _baseMpReg;
		}
	}

	@Override
	protected void onEvtDead()
	{
		_zone.getListenerEngine().removeMethodInvokedListener(_zoneListener);
		super.onEvtDead();
	}
}