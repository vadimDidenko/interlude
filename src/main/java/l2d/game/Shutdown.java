package l2d.game;

import java.util.logging.Logger;

import l2d.Config;
import l2d.auth.AuthWebStatus;
import l2d.db.L2DatabaseFactory;
import l2d.debug.HeapDumper;
import l2d.ext.scripts.Scripts;
import l2d.game.cache.InfoCache;
import l2d.game.geodata.GeoEngine;
import l2d.game.idfactory.IdFactory;
import l2d.game.instancemanager.CoupleManager;
import l2d.game.instancemanager.CursedWeaponsManager;
import l2d.game.loginservercon.LSConnection;
import l2d.game.model.L2Multisell;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;
import l2d.game.model.entity.SevenSigns;
import l2d.game.model.entity.SevenSignsFestival.SevenSignsFestival;
import l2d.game.model.entity.olympiad.OlympiadDatabase;
import l2d.game.tables.ArmorSetsTable;
import l2d.game.tables.ItemTable;
import l2d.game.tables.NpcTable;
import l2d.game.tables.PetDataTable;
import l2d.game.tables.SkillSpellbookTable;
import l2d.game.tables.SkillTable;
import l2d.game.tables.SkillTreeTable;
import l2d.game.tables.SpawnTable;
import l2d.game.tables.TerritoryTable;
import l2d.util.Log;
import l2d.util.Util;

@SuppressWarnings({ "nls", "unqualified-field-access", "boxing" })
public class Shutdown extends Thread
{
	private static final Logger _log = Logger.getLogger(Shutdown.class.getName());

	private static Shutdown _instance;
	private static Shutdown _counterInstance = null;

	private int secondsShut;
	private int shutdownMode;

	public static final int SIGTERM = 0;
	public static final int GM_SHUTDOWN = 1;
	public static final int GM_RESTART = 2;
	public static final int ABORT = 3;
	private static String[] _modeText_e = { "brought down", "turned off", "restart", "abort" };
	private static String[] _modeText_log = { "brought down", "brought down", "Restart", "aborting" };

	public int getSeconds()
	{
		if(_counterInstance != null)
			return _counterInstance.secondsShut;
		return -1;
	}

	public int getMode()
	{
		if(_counterInstance != null)
			return _counterInstance.shutdownMode;
		return -1;
	}

	private void announce(String text)
	{
		Announcements.getInstance().announceToAll(text);
	}

	/**
	 * This function starts a shutdown countdown from Telnet (Copied from Function startShutdown())
	 *
	 * @param IP		    IP Which Issued shutdown command
	 * @param seconds	   seconds untill shutdown
	 * @param restart	   true if the server will restart after shutdown
	 */
	public void startTelnetShutdown(String IP, int seconds, boolean restart)
	{
		_log.warning("IP: " + IP + " issued shutdown command. " + _modeText_log[shutdownMode] + " in " + seconds + " seconds!");
		announce("Attention! Server will " + _modeText_e[shutdownMode] + " in " + seconds + " seconds!");

		if(_counterInstance != null)
			_counterInstance._abort();
		_counterInstance = new Shutdown(seconds, restart);
		_counterInstance.start();
	}

	public void setAutoRestart(int seconds)
	{
		_log.warning("AutoRestart scheduled through " + Util.formatTime(seconds));
		if(_counterInstance != null)
			_counterInstance._abort();
		_counterInstance = new Shutdown(seconds, true);
		_counterInstance.start();
	}

	/**
	 * This function aborts a running countdown
	 *
	 * @param IP		    IP Which Issued shutdown command
	 */
	public void Telnetabort(String IP)
	{
		_log.warning("IP: " + IP + " issued shutdown ABORT. " + _modeText_log[shutdownMode] + " has been stopped!");
		announce("Attention! Server will not " + _modeText_e[shutdownMode] + " and continues to run normall!");

		if(_counterInstance != null)
			_counterInstance._abort();
	}

	/**
	 * Default constucter is only used internal to create the shutdown-hook instance
	 *
	 */
	public Shutdown()
	{
		secondsShut = -1;
		shutdownMode = SIGTERM;
	}

	/**
	 * This creates a countdown instance of Shutdown.
	 *
	 * @param seconds	how many seconds until shutdown
	 * @param restart	true is the server shall restart after shutdown
	 *
	 */
	public Shutdown(int seconds, boolean restart)
	{
		if(seconds < 0)
			seconds = 0;
		secondsShut = seconds;
		if(restart)
			shutdownMode = GM_RESTART;
		else
			shutdownMode = GM_SHUTDOWN;
	}

	/**
	 * get the shutdown-hook instance
	 * the shutdown-hook instance is created by the first call of this function,
	 * but it has to be registrered externaly.
	 *
	 * @return instance of Shutdown, to be used as shutdown hook
	 */
	public static Shutdown getInstance()
	{
		if(_instance == null)
			_instance = new Shutdown();
		return _instance;
	}

	/**
	 * this function is called, when a new thread starts
	 *
	 * if this thread is the thread of getInstance, then this is the shutdown hook
	 * and we save all data and disconnect all clients.
	 *
	 * after this thread ends, the server will completely exit
	 *
	 * if this is not the thread of getInstance, then this is a countdown thread.
	 * we start the countdown, and when we finished it, and it was not aborted,
	 * we tell the shutdown-hook why we call exit, and then call exit
	 *
	 * when the exit status of the server is 1, startServer.sh / startServer.bat
	 * will restart the server.
	 *
	 * Логгинг в этом методе не работает!!!
	 */
	@Override
	public void run()
	{
		if(this == _instance)
		{
			LSConnection.getInstance().shutdown();
			System.out.println("Shutting down scripts.");
			// Вызвать выключение у скриптов
			Scripts.getInstance().shutdown();

			// ensure all services are stopped
			// stop all scheduled tasks
			saveData();
			try
			{
				ThreadPoolManager.getInstance().shutdown();
			}
			catch(Throwable t)
			{
				t.printStackTrace();
			}
			// last byebye, save all data and quit this server
			// logging doesn't works here :(
			LSConnection.getInstance().shutdown();
			// saveData sends messages to exit players, so shutdown selector after it
			try
			{
				System.out.println("Shutting down selector.");
				GameServer.gameServer.getSelectorThread().shutdown();
			}
			catch(Throwable t)
			{
				t.printStackTrace();
			}
			// commit data, last chance
			try
			{
				System.out.println("Shutting down database communication.");
				L2DatabaseFactory.getInstance().shutdown();
			}
			catch(Throwable t)
			{
				t.printStackTrace();
			}

			if(Config.DUMP_MEMORY_ON_SHUTDOWN)
				try
				{
					System.out.println("Prepearing to memory snapshot - unloading static data...");
					GeoEngine.unload();
					IdFactory.unload();
					L2Multisell.unload();
					InfoCache.unload();

					ArmorSetsTable.unload();
					//TODO ClanTable ??
					ItemTable.unload();
					NpcTable.unload();
					PetDataTable.unload();
					SkillSpellbookTable.unload();
					SkillTable.unload();
					SkillTreeTable.unload();
					SpawnTable.unload();
					TerritoryTable.unload();

					System.out.println("Memory snapshot saved: " + HeapDumper.dumpHeap(Config.SNAPSHOTS_DIRECTORY, true));
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}

			// server will quit, when this function ends.
			Runtime.getRuntime().halt(_instance.shutdownMode == GM_RESTART ? 2 : 0);
		}
		else
		{
			// gm shutdown: send warnings and then call exit to start shutdown sequence
			countdown();
			// last point where logging is operational :(
			System.out.println("GM shutdown countdown is over. " + _modeText_log[shutdownMode] + " NOW!");
			switch(shutdownMode)
			{
				case GM_SHUTDOWN:
					_instance.setMode(GM_SHUTDOWN);
					System.exit(0);
					WebStatusUpdate.getInstance().onShutDown();
					break;
				case GM_RESTART:
					_instance.setMode(GM_RESTART);
					System.exit(2);
					break;
			}
		}
	}

	/**
	 * This functions starts a shutdown countdown
	 *
	 * @param activeChar	GM who issued the shutdown command
	 * @param seconds		seconds until shutdown
	 * @param restart		true if the server will restart after shutdown
	 */
	public void startShutdown(L2Player activeChar, int seconds, boolean restart)
	{
		_log.warning("GM: " + activeChar.getName() + "(" + activeChar.getObjectId() + ") issued shutdown command. " + _modeText_log[shutdownMode] + " in " + seconds + " seconds!");
		if(shutdownMode > 0)
			announce("Attention! Server will " + _modeText_e[shutdownMode] + " in " + seconds + " seconds");
		if(_counterInstance != null)
			_counterInstance._abort();

		//the main instance should only run for shutdown hook, so we start a new instance
		_counterInstance = new Shutdown(seconds, restart);
		_counterInstance.start();
	}

	/**
	 * This function aborts a running countdown
	 *
	 * @param activeChar	GM who issued the abort command
	 */
	public void abort(L2Player activeChar)
	{
		_log.warning("GM: " + activeChar.getName() + "(" + activeChar.getObjectId() + ") issued shutdown ABORT. " + _modeText_log[shutdownMode] + " has been stopped!");
		announce("Attention! Server will not " + _modeText_e[shutdownMode] + " and continues to run normally!");

		if(_counterInstance != null)
			_counterInstance._abort();
	}

	/**
	 * set the shutdown mode
	 * @param mode	what mode shall be set
	 */
	private void setMode(int mode)
	{
		shutdownMode = mode;
	}

	/**
	 * set shutdown mode to ABORT
	 */
	private void _abort()
	{
		shutdownMode = ABORT;
	}

	/**
	 * this counts the countdown and reports it to all players
	 * countdown is aborted if mode changes to ABORT
	 */
	private void countdown()
	{
		while(secondsShut > 0)
			try
			{
				switch(secondsShut)
				{
					case 1800:
						announce("Attention players! Server will " + _modeText_e[shutdownMode] + " after 30 minutes.");
						break;
					case 1200:
						announce("Attention players! Server will " + _modeText_e[shutdownMode] + " after 20 minutes.");
						break;
					case 600:
						announce("Attention players! Server will " + _modeText_e[shutdownMode] + " after 10 minutes.");
						break;
					case 300:
						announce("Attention players! Server will " + _modeText_e[shutdownMode] + " after 5 minutes.");
						break;
					case 240:
						announce("Attention players! Server will " + _modeText_e[shutdownMode] + " after 4 minutes.");
						break;
					case 180:
						announce("Attention players! Server will " + _modeText_e[shutdownMode] + " after 3 minutes.");
						break;
					case 120:
						announce("Attention players! Server will " + _modeText_e[shutdownMode] + " after 2 minutes.");
						break;
					case 60:
						announce("Attention players! Server will " + _modeText_e[shutdownMode] + " after 1 minutes.");
						if(!Config.DONTLOADSPAWN)
							try
							{
								L2World.deleteVisibleNpcSpawns();
							}
							catch(Throwable t)
							{
								System.out.println("Error while unspawn Npcs!");
								t.printStackTrace();
							}
						break;
					case 30:
						announce("Attention players! Server will " + _modeText_e[shutdownMode] + " now.");
						break;
					case 15:
						disconnectAllCharacters();
						break;
				}

				secondsShut--;

				int delay = 1000; //milliseconds
				Thread.sleep(delay);

				if(shutdownMode == ABORT)
					break;
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
	}

	/**
	 * this sends a last byebye, disconnects all players and saves data
	 *
	 */
	private void saveData()
	{
		switch(shutdownMode)
		{
			case SIGTERM:
				System.err.println("SIGTERM received. Shutting down NOW!");
				Log.LogServ(Log.GS_SIGTERM, 0, 0, 0, 0);
				break;
			case GM_SHUTDOWN:
				System.err.println("GM shutdown received. Shutting down NOW!");
				Log.LogServ(Log.GS_shutdown, 0, 0, 0, 0);
				break;
			case GM_RESTART:
				System.err.println("GM restart received. Restarting NOW!");
				Log.LogServ(Log.GS_restart, 0, 0, 0, 0);
				break;
		}

		disconnectAllCharacters();
		// Seven Signs data is now saved along with Festival data.
		if(!SevenSigns.getInstance().isSealValidationPeriod())
		{
			SevenSignsFestival.getInstance().saveFestivalData(false);
			System.out.println("Seven Signs Festival data saved.");
		}

		// Save Seven Signs data before closing. :)
		SevenSigns.getInstance().saveSevenSignsData(null, true);
		System.out.println("Seven Signs data saved.");

		if(Config.ENABLE_OLYMPIAD)
			try
			{
				OlympiadDatabase.save();
				System.out.println("Olympiad System: Data saved!");
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}

		if(Config.WEDDING_ALLOW_WEDDING)
		{
			CoupleManager.getInstance().store();
			System.out.println("Couples: Data saved!");
		}

		if(Config.ALLOW_CURSED_WEAPONS)
		{
			CursedWeaponsManager.getInstance().saveData();
			System.out.println("CursedWeaponsManager: Data saved!");
		}

		System.out.println("All Data saved. All players disconnected, shutting down.");
		try
		{
			int delay = 5000;
			Thread.sleep(delay);
		}
		catch(InterruptedException e)
		{
			//never happens :p
		}
	}

	/**
	 * this disconnects all clients from the server
	 *
	 */
	private void disconnectAllCharacters()
	{
		for(L2Player player : L2World.getAllPlayers())
			//Logout Character
			try
			{
				player.logout(true, false, false);
			}
			catch(Throwable t)
			{
				System.out.println("Error while disconnect char: " + player.getName());
				t.printStackTrace();
			}
		try
		{
			Thread.sleep(1000);
		}
		catch(Throwable t)
		{
			t.printStackTrace();
		}
	}
}