package l2d.game.model.instances;

import l2d.ext.scripts.Events;
import l2d.game.ai.CtrlIntention;
import l2d.game.model.L2Character;
import l2d.game.model.L2Player;
import l2d.game.serverpackets.MyTargetSelected;
import l2d.game.serverpackets.NpcHtmlMessage;
import l2d.game.serverpackets.ShowTownMap;
import l2d.game.templates.L2NpcTemplate;

public class L2StaticObjectInstance extends L2NpcInstance
{
	private int _staticObjectId;
	private int _type = -1; // 0 - signs, 1 - throne, 2 - starter town map
	private String _filePath;
	private int _mapX;
	private int _mapY;

	public int getStaticObjectId()
	{
		return _staticObjectId;
	}

	public void setStaticObjectId(int StaticObjectId)
	{
		_staticObjectId = StaticObjectId;
	}

	public L2StaticObjectInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	public int getType()
	{
		return _type;
	}

	public void setType(int type)
	{
		_type = type;
	}

	public void setFilePath(String path)
	{
		_filePath = path;
	}

	public void setMapX(int x)
	{
		_mapX = x;
	}

	public void setMapY(int y)
	{
		_mapY = y;
	}

	@Override
	public void onAction(L2Player player)
	{
		if(Events.onAction(player, this))
			return;

		if(player.getTarget() != this)
		{
			player.setTarget(this);
			player.sendPacket(new MyTargetSelected(getObjectId(), 0));
			return;
		}

		MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
		player.sendPacket(my);

		if(!isInRange(player, INTERACTION_DISTANCE))
		{
			if(player.getAI().getIntention() != CtrlIntention.AI_INTENTION_INTERACT)
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this, null);
			return;
		}

		if(_type == 0) // Arena Board
			player.sendPacket(new NpcHtmlMessage(player, this, "data/html/newspaper/arena.htm", 0));
		else if(_type == 2) // Village map
		{
			player.sendPacket(new ShowTownMap(_filePath, _mapX, _mapY));
			player.sendActionFailed();
		}
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return false;
	}

	@Override
	public boolean isAttackable()
	{
		return false;
	}

	@Override
	public void doDie(L2Character killer)
	{}

	@Override
	public boolean isInvul()
	{
		return true;
	}

	@Override
	public boolean isFearImmune()
	{
		return true;
	}

	@Override
	public boolean isParalyzeImmune()
	{
		return true;
	}

	@Override
	public boolean isLethalImmune()
	{
		return true;
	}
}