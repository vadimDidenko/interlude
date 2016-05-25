package com.lineage.ext.listeners;

import com.lineage.ext.listeners.events.PropertyEvent;

public interface PropertyChangeListener
{
	/**
	 * Вызывается при смене состояния
	 * @param event передаваемое событие
	 */
	public void propertyChanged(PropertyEvent event);

	/**
	 * Возвращает свойство даного листенера
	 * @return свойство
	 */
	public PropertyType getPropery();

	/**
	 * Простенький фильтр, если лень добавлят по группам.
	 *
	 * Фильтр применяется только в общем хранилеще, тоесть при:
	 *
	 * addPropertyChangeListener(слушатель) - обработается этот метод
	 *
	 * а при
	 *
	 * addPropertyChangeListener(параметр, слушатель) не обработает
	 *
	 *
	 * @param property свойство
	 * @return принимать ли
	 */
	public boolean accept(PropertyType property);
}
