package l2d.game.instancemanager;

import java.util.HashMap;
import java.util.logging.Logger;

import javolution.util.FastList;
import l2d.game.model.L2Clan;
import l2d.game.model.L2Zone;
import l2d.game.model.L2Zone.ZoneType;
import l2d.game.model.entity.residence.ClanHall;
import l2d.game.model.instances.L2DoorInstance;
import l2d.game.tables.DoorTable;

public class ClanHallManager
{
	protected static Logger _log = Logger.getLogger(ClanHallManager.class.getName());

	private static ClanHallManager _instance;

	public static ClanHallManager getInstance()
	{
		if(_instance == null)
		{
			_log.info("Initializing ClanHallManager.");
			_instance = new ClanHallManager();
			_instance.load();
		}
		return _instance;
	}

	private HashMap<Integer, ClanHall> _ClanHalls;

	public void reload()
	{
		getClanHalls().clear();
		load();
	}

	private void load()
	{
		FastList<L2Zone> zones = ZoneManager.getInstance().getZoneByType(ZoneType.ClanHall);
		if(zones.size() == 0)
			_log.info("Not found zones for ClanHalls!!!");
		else
			for(L2Zone zone : zones)
			{
				ClanHall clanhall = new ClanHall(zone.getIndex());
				clanhall.init();
				getClanHalls().put(zone.getIndex(), clanhall);
			}
		_log.info("Loaded: " + getClanHalls().size() + " ClanHalls.");
		int door_count = 0;
		for(L2DoorInstance door : DoorTable.getInstance().getDoors())
		{
			ClanHall clanhall = ClanHallManager.getInstance().findNearestClanHall(door.getX(), door.getY(), 1500);
			if(clanhall != null)
			{
				clanhall.getDoors().add(door);
				door.setSiegeUnit(clanhall);
				door_count++;
			}
		}
		_log.info(door_count + " doors attached to ClanHalls");
	}

	public final ClanHall getClanHall(int index)
	{
		return getClanHalls().get(index);
	}

	public final ClanHall findNearestClanHall(int x, int y, int offset)
	{
		int index = findNearestClanHallIndex(x, y, offset);
		if(index > 0)
			return getClanHalls().get(index);
		return null;
	}

	public final ClanHall getClanHallByOwner(L2Clan clan)
	{
		if(clan == null)
			return null;
		for(ClanHall clanhall : getClanHalls().values())
			if(clan.getClanId() == clanhall.getOwnerId())
				return clanhall;
		return null;
	}

	public int getClanHallIndex(int x, int y)
	{
		for(ClanHall clanHall : getClanHalls().values())
			if(clanHall != null && clanHall.checkIfInZone(x, y))
				return clanHall.getId();
		return -1;
	}

	public final int findNearestClanHallIndex(int x, int y, int offset)
	{
		int index = getClanHallIndex(x, y);
		if(index < 0)
		{
			double closestDistance = offset;
			double distance;
			for(ClanHall clanHall : getClanHalls().values())
			{
				if(clanHall == null)
					continue;
				distance = clanHall.getZone().findDistanceToZone(x, y, 0, false);
				if(closestDistance > distance)
				{
					closestDistance = distance;
					index = clanHall.getId();
				}
			}
		}
		return index;
	}

	public final HashMap<Integer, ClanHall> getClanHalls()
	{
		if(_ClanHalls == null)
			_ClanHalls = new HashMap<Integer, ClanHall>();
		return _ClanHalls;
	}
}