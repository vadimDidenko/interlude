package com.lineage.ext.listeners.events.AbstractAI;

import com.lineage.ext.listeners.events.DefaultMethodInvokeEvent;
import com.lineage.game.ai.AbstractAI;

/**
 * @Author: Diamond
 * @Date: 08/11/2007
 * @Time: 7:17:24
 */
public class AbstractAISetIntention extends DefaultMethodInvokeEvent
{
	public AbstractAISetIntention(String methodName, AbstractAI owner, Object[] args)
	{
		super(methodName, owner, args);
	}

	@Override
	public AbstractAI getOwner()
	{
		return (AbstractAI) super.getOwner();
	}

	@Override
	public Object[] getArgs()
	{
		return super.getArgs();
	}
}