package l2d.auth.gameservercon;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import l2d.auth.GameServerTable;
import l2d.auth.Watchdog;
import l2d.auth.gameservercon.gspackets.ServerStatus;
import l2d.game.loginservercon.AdvIP;

/**
 * @Author: Death
* @Date: 16/11/2007
* @Time: 12:09:53
*/
public class GameServerInfo
{
	private static Logger log = Logger.getLogger(GameServerInfo.class.getName());

	// auth
	private int _id;
	private byte[] _hexId;
	private boolean _isAuthed;

	// status
	private AttGS _gst;
	private int _status;

	// network
	private String _internalHost;
	private String _externalHost;
	private int _port;

	// config
	private boolean _isPvp = true;
	private boolean _isTestServer;
	private boolean _isShowingClock;
	private boolean _isShowingBrackets;
	private int _maxPlayers;
	private FastList<AdvIP> _ips;

	public GameServerInfo(int id, byte[] hexId, AttGS gameserver)
	{
		_id = id;
		_hexId = hexId;
		_gst = gameserver;
	}

	public GameServerInfo(int id, byte[] hexId)
	{
		this(id, hexId, null);
	}

	public void setId(int id)
	{
		_id = id;
	}

	public int getId()
	{
		return _id;
	}

	public byte[] getHexId()
	{
		return _hexId;
	}

	public void setAuthed(boolean isAuthed)
	{
		_isAuthed = isAuthed;
	}

	public boolean isAuthed()
	{
		return _isAuthed;
	}

	public void setGameServer(AttGS gameserver)
	{
		_gst = gameserver;
	}

	public AttGS getGameServer()
	{
		return _gst;
	}

	public void setStatus(int status)
	{
		_status = status;
	}

	public int getStatus()
	{
		return _status;
	}

	public int getCurrentPlayerCount()
	{
		if(_gst == null)
			return 0;
		return _gst.getPlayerCount();
	}

	public String getInternalHost()
	{
		return _internalHost;
	}

	public void setInternalHost(String internalHost)
	{
		_internalHost = internalHost;
	}

	public void setExternalHost(String externalHost)
	{
		_externalHost = externalHost;
	}

	public String getExternalHost()
	{
		return _externalHost;
	}

	public int getPort()
	{
		return _port;
	}

	public void setPort(int port)
	{
		_port = port;
	}

	public void setMaxPlayers(int maxPlayers)
	{
		_maxPlayers = maxPlayers;
	}

	public int getMaxPlayers()
	{
		return _maxPlayers;
	}

	public boolean isPvp()
	{
		return _isPvp;
	}

	public void setTestServer(boolean val)
	{
		_isTestServer = val;
	}

	public boolean isTestServer()
	{
		return _isTestServer;
	}

	public void setShowingClock(boolean clock)
	{
		_isShowingClock = clock;
	}

	public boolean isShowingClock()
	{
		return _isShowingClock;
	}

	public void setShowingBrackets(boolean val)
	{
		_isShowingBrackets = val;
	}

	public boolean isShowingBrackets()
	{
		return _isShowingBrackets;
	}

	public void setAdvIP(FastList<AdvIP> val)
	{
		_ips = val;
	}

	public FastList<AdvIP> getAdvIP()
	{
		return _ips;
	}

	public void setDown()
	{
		log.info("Setting GameServer down");
		setAuthed(false);
		setPort(0);
		setGameServer(null);
		_status = ServerStatus.STATUS_DOWN;
	}

	@Override
	public String toString()
	{
		TextBuilder tb = TextBuilder.newInstance();

		tb.append("GameServer: ");
		if(_gst != null)
		{
			tb.append(_gst.getName());
			tb.append(" id:");
			tb.append(_id);
			tb.append(" hex:");
			tb.append(_hexId);
			tb.append(" ip:");
			tb.append(_gst.getConnectionIpAddress());
			tb.append(":");
			tb.append(_port);
			tb.append(" status: ");
			tb.append(ServerStatus.statusString[_status]);
		}
		else
		{
			tb.append(GameServerTable.getInstance().getServerNames().get(_id));
			tb.append(" id:");
			tb.append(_id);
			tb.append(" hex:");
			tb.append(_hexId);
			tb.append(" status: ");
			tb.append(ServerStatus.statusString[_status]);
		}

		String ret = tb.toString();
		TextBuilder.recycle(tb);
		return ret;
	}

	public void setGameHosts(String gameExternalHost, String gameInternalHost, FastList<AdvIP> ips)
	{
		setExternalHost(gameExternalHost);
		setInternalHost(gameInternalHost);
		if(!getExternalHost().equals("*"))
			try
			{
				InetAddress.getByName(getExternalHost()).getHostAddress();
			}
			catch(UnknownHostException e)
			{
				e.printStackTrace();
			}

		if(!getInternalHost().equals("*"))
			try
			{
				InetAddress.getByName(getInternalHost()).getHostAddress();
			}
			catch(UnknownHostException e)
			{
				e.printStackTrace();
			}

		setAdvIP(ips);

		Watchdog.init();

		log.info("Updated Gameserver " + GameServerTable.getInstance().getServerNameById(getId()) + " Hostname's:");
		log.info("InternalHostname: " + getInternalHost());
		log.info("ExternalHostname: " + getExternalHost());
	}
}
