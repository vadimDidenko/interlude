package l2d.gsregistering;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.math.BigInteger;
import java.util.Map;

import l2d.Config;
import l2d.Server;
import l2d.auth.GameServerTable;
import l2d.util.Util;

public class GameServerRegister
{
	private static GameServerTable gsTable;
	private static boolean _choiseOk;

	public static void main(String[] args) throws IOException
	{
		Server.SERVER_MODE = Server.MODE_LOGINSERVER;
		Config.loadAllConfigs();
		try
		{
			gsTable = new GameServerTable();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		System.out.println("Welcome to L2impulse GameServer Registering");
		System.out.println("Enter The id of the server you want to register or type help to get a list of ids:");
		LineNumberReader _in = new LineNumberReader(new InputStreamReader(System.in));
		while(!_choiseOk)
		{
			System.out.println("Your choice:");
			String _choice = _in.readLine();
			if(_choice.equalsIgnoreCase("help") || _choice.equalsIgnoreCase("/help") || _choice.equalsIgnoreCase("h") || _choice.equalsIgnoreCase("/h") || _choice.equalsIgnoreCase("/?"))
			{
				for(Map.Entry<Integer, String> entry : gsTable.getServerNames().entrySet())
					System.out.println("Server: id:" + entry.getKey() + " - " + entry.getValue());
				System.out.println("You can also see servername.xml");
			}
			else
				try
				{
					int id = Integer.parseInt(_choice);
					if(id >= gsTable.getServerNames().size())
					{
						System.out.println("ID is too high (max is " + gsTable.getServerNames().size() + ")");
						continue;
					}
					if(id < 0)
					{
						System.out.println("ID must be positive number");
						continue;
					}
					if(!gsTable.hasRegisteredGameServerOnId(id))
					{
						byte[] hex = Util.generateHex(16);
						gsTable.registerServerOnDB(hex, id, "");
						Config.saveHexid(new BigInteger(hex).toString(16), "hexid(server " + id + ").txt");
						System.out.println("Server Registered hexid saved to 'hexid(server " + id + ").txt'");
						System.out.println("Put this file in the /config folder of your gameserver and rename it to 'hexid.txt'");
						return;
					}
					System.out.println("This id is not free");
				}
				catch(NumberFormatException nfe)
				{
					System.out.println("Please, type a number or 'help' or '/help' or 'h' or '/h' or '/?'");
				}
		}
	}
}