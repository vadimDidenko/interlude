package l2d.ext.listeners;

import l2d.ext.listeners.events.MethodEvent;
import l2d.ext.listeners.events.L2Zone.L2ZoneEnterLeaveEvent;
import l2d.game.model.L2Object;
import l2d.game.model.L2Zone;

/**
 * @Author: Death
 * @Date: 18/9/2007
 * @Time: 9:34:56
 */
public abstract class L2ZoneEnterLeaveListener implements MethodInvokeListener, MethodCollection
{
	/**
	 * Вызывается при входе/выходе из зоны. После необходимых операций вызывает нужный
	 * абстрактный метод.
	 * @param e объект класса L2ZoneEnterLeaveEvent
	 * @see l2d.ext.listeners.events.L2Zone.L2ZoneEnterLeaveEvent
	 * @see l2d.game.model.L2Zone#doEnter(l2d.game.model.L2Object)
	 * @see l2d.game.model.L2Zone#doLeave(l2d.game.model.L2Object, boolean)
	 */
	@Override
	public final void methodInvoked(MethodEvent e)
	{
		L2ZoneEnterLeaveEvent event = (L2ZoneEnterLeaveEvent) e;
		L2Zone owner = event.getOwner();
		L2Object actor = event.getArgs()[0];
		if(e.getMethodName().equals(L2ZoneObjectEnter))
			objectEntered(owner, actor);
		else
			objectLeaved(owner, actor);
	}

	@Override
	public final boolean accept(MethodEvent event)
	{
		String method = event.getMethodName();
		return event instanceof L2ZoneEnterLeaveEvent && (method.equals(L2ZoneObjectEnter) || method.equals(L2ZoneObjectLeave));
	}

	/**
	 * Метод вызывается когда объект входит в зону
	 * @param zone зона в которую вошли
	 * @param object вошедший объект
	 * @see l2d.game.model.L2Zone#doEnter(l2d.game.model.L2Object)
	 */
	public abstract void objectEntered(L2Zone zone, L2Object object);

	/**
	 * Метод вызывается когда объект выходит с зоны
	 * @param zone зона с которой вышли
	 * @param object вышедший объект
	 * @see l2d.game.model.L2Zone#doLeave(l2d.game.model.L2Object, boolean)
	 */
	public abstract void objectLeaved(L2Zone zone, L2Object object);
}
