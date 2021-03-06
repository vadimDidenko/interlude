package commands.admin;

import javolution.util.FastList;
import l2d.ext.multilang.CustomMessage;
import l2d.ext.scripts.ScriptFile;
import l2d.game.handler.AdminCommandHandler;
import l2d.game.handler.IAdminCommandHandler;
import l2d.game.loginservercon.LSConnection;
import l2d.game.loginservercon.gspackets.BanIP;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;
import l2d.game.serverpackets.NpcHtmlMessage;
import l2d.util.BannedIp;
import l2d.util.Log;
import l2d.util.Util;

/**
 * This class handles following admin commands:- ipbanlist - ipban, ipblock -
 * ipunban, ipunblock - ipcharban
 */
public class AdminIP implements IAdminCommandHandler, ScriptFile
{
	private enum Commands
	{
		admin_ipbanlist,
		admin_ipban,
		admin_ipblock,
		admin_ipunban,
		admin_ipunblock,
		admin_ipcharban,
		admin_ipchar,
		admin_charip
	}

	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, L2Player activeChar)
	{
		Commands command = (Commands) comm;

		if(!activeChar.getPlayerAccess().CanBan)
			return false;

		switch(command)
		{
			case admin_ipbanlist:
				try
				{
					FastList<BannedIp> baniplist = LSConnection.getInstance().getBannedIpList();
					if(baniplist != null && baniplist.size() > 0)
					{
						NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
						StringBuffer replyMSG = new StringBuffer("<html><body>");

						replyMSG.append("<center>Ban IP List</center><br>");
						replyMSG.append("<center><table width=300><tr><td>");
						replyMSG.append("<center>IP</center></td><td>Banned by</td></tr>");
						for(BannedIp temp : baniplist)
							replyMSG.append("<tr><td>" + temp.ip + "</td><td>" + temp.admin + "</td><td><button value=\"Unban IP\" action=\"bypass -h admin_unbanip " + temp.ip + "\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
						replyMSG.append("</table></center>");
						replyMSG.append("</body></html>");

						adminReply.setHtml(replyMSG.toString());
						activeChar.sendPacket(adminReply);
					}
				}
				catch(StringIndexOutOfBoundsException e)
				{
					activeChar.sendMessage(new CustomMessage("common.Error", activeChar));
				}
				break;
			case admin_ipban:
			case admin_ipblock:
				if(wordList.length != 2)
				{
					activeChar.sendMessage("Command syntax: //ipban <ip>");
					break;
				}

				if(!validateIP(wordList[1]))
				{
					activeChar.sendMessage("Error: Invalid IP adress: " + wordList[1]);
					break;
				}

				LSConnection.getInstance().sendPacket(new BanIP(wordList[1], activeChar.getName()));
				Log.add("IP " + wordList[1] + ", ban attempt", "gm_ext_actions", activeChar);
				break;
			case admin_ipcharban:
				if(wordList.length != 2)
				{
					activeChar.sendMessage("Command syntax: //ipcharban <char_name>");
					break;
				}

				L2Player plr = L2World.getPlayer(wordList[1]);

				if(plr == null)
				{
					activeChar.sendMessage("Character " + wordList[1] + " not found.");
					break;
				}

				String ip = plr.getIP();
				// Проверку на валидность ip пропускаем, ибо верим серверу

				if(ip.equalsIgnoreCase("<not connected>"))
				{
					activeChar.sendMessage("Character " + wordList[1] + " not found.");
					break;
				}

				LSConnection.getInstance().sendPacket(new BanIP(ip, activeChar.getName()));
				Log.add("IP " + ip + ", ban attempt", "gm_ext_actions", activeChar);
				break;
			case admin_ipchar:
			case admin_charip:
				if(wordList.length != 2)
				{
					activeChar.sendMessage("Command syntax: //charip <char_name>");
					activeChar.sendMessage(" Gets character's IP.");
					break;
				}

				L2Player pl = L2World.getPlayer(wordList[1]);

				if(pl == null)
				{
					activeChar.sendMessage("Character " + wordList[1] + " not found.");
					break;
				}

				String ip_adr = pl.getIP();
				if(ip_adr.equalsIgnoreCase("<not connected>"))
				{
					activeChar.sendMessage("Character " + wordList[1] + " not found.");
					break;
				}

				activeChar.sendMessage("Character's IP: " + ip_adr);
				break;
			case admin_ipunban:
			case admin_ipunblock:
				if(wordList.length != 2)
				{
					activeChar.sendMessage("Command syntax: //ipunban <ip>");
					break;
				}

				if(!validateIP(wordList[1]))
				{
					activeChar.sendMessage("Error: Invalid IP adress: " + wordList[1]);
					break;
				}

				LSConnection.getInstance().sendPacket(new BanIP(wordList[1], activeChar.getName()));
				Log.add("IP " + wordList[1] + ", ban attempt", "gm_ext_actions", activeChar);
				break;
		}
		return true;
	}

	public boolean validateIP(String IP)
	{
		if(!Util.isMatchingRegexp(IP, "[0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}"))
			return false;

		// Split by dot
		IP = IP.replace(".", ",");
		String[] IP_octets = IP.split(",");

		for(String element : IP_octets)
			if(Integer.parseInt(element) > 255)
				return false;

		return true;
	}

	public Enum[] getAdminCommandEnum()
	{
		return Commands.values();
	}

	public void onLoad()
	{
		AdminCommandHandler.getInstance().registerAdminCommandHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}