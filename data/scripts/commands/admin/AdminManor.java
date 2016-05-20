package commands.admin;

import java.util.StringTokenizer;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import l2d.Config;
import l2d.ext.scripts.ScriptFile;
import l2d.game.handler.AdminCommandHandler;
import l2d.game.handler.IAdminCommandHandler;
import l2d.game.instancemanager.CastleManager;
import l2d.game.instancemanager.CastleManorManager;
import l2d.game.instancemanager.ServerVariables;
import l2d.game.instancemanager.CastleManorManager.CropProcure;
import l2d.game.instancemanager.CastleManorManager.SeedProduction;
import l2d.game.model.L2Player;
import l2d.game.model.entity.residence.Castle;
import l2d.game.serverpackets.NpcHtmlMessage;

/**
 * Admin comand handler for Manor System
 * This class handles following admin commands:
 * - manor_info = shows info about current manor state
 * - manor_approve = approves settings for the next manor period
 * - manor_setnext = changes manor settings to the next day's
 * - manor_reset castle = resets all manor data for specified castle (or all)
 * - manor_setmaintenance = sets manor system under maintenance mode
 * - manor_save = saves all manor data into database
 * - manor_disable = disables manor system
 */
@SuppressWarnings("unused")
public class AdminManor implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_manor,
		admin_manor_reset,
		admin_manor_save,
		admin_manor_disable
	}

	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, L2Player activeChar)
	{
		Commands command = (Commands) comm;

		if(!activeChar.getPlayerAccess().Menu)
			return false;

		StringTokenizer st = new StringTokenizer(fullString);
		fullString = st.nextToken();

		if(fullString.equals("admin_manor"))
			showMainPage(activeChar);
		else if(fullString.equals("admin_manor_reset"))
		{
			int castleId = 0;
			try
			{
				castleId = Integer.parseInt(st.nextToken());
			}
			catch(Exception e)
			{}

			if(castleId > 0)
			{
				Castle castle = CastleManager.getInstance().getCastleByIndex(castleId);
				castle.setCropProcure(new FastList<CropProcure>(), CastleManorManager.PERIOD_CURRENT);
				castle.setCropProcure(new FastList<CropProcure>(), CastleManorManager.PERIOD_NEXT);
				castle.setSeedProduction(new FastList<SeedProduction>(), CastleManorManager.PERIOD_CURRENT);
				castle.setSeedProduction(new FastList<SeedProduction>(), CastleManorManager.PERIOD_NEXT);
				if(Config.MANOR_SAVE_ALL_ACTIONS)
				{
					castle.saveCropData();
					castle.saveSeedData();
				}
				activeChar.sendMessage("Manor data for " + castle.getName() + " was nulled");
			}
			else
			{
				for(Castle castle : CastleManager.getInstance().getCastles().values())
				{
					castle.setCropProcure(new FastList<CropProcure>(), CastleManorManager.PERIOD_CURRENT);
					castle.setCropProcure(new FastList<CropProcure>(), CastleManorManager.PERIOD_NEXT);
					castle.setSeedProduction(new FastList<SeedProduction>(), CastleManorManager.PERIOD_CURRENT);
					castle.setSeedProduction(new FastList<SeedProduction>(), CastleManorManager.PERIOD_NEXT);
					if(Config.MANOR_SAVE_ALL_ACTIONS)
					{
						castle.saveCropData();
						castle.saveSeedData();
					}
				}
				activeChar.sendMessage("Manor data was nulled");
			}
			showMainPage(activeChar);
		}
		else if(fullString.equals("admin_manor_save"))
		{
			CastleManorManager.getInstance().save();
			activeChar.sendMessage("Manor System: all data saved");
			showMainPage(activeChar);
		}
		else if(fullString.equals("admin_manor_disable"))
		{
			boolean mode = CastleManorManager.getInstance().isDisabled();
			CastleManorManager.getInstance().setDisabled(!mode);
			if(mode)
				activeChar.sendMessage("Manor System: enabled");
			else
				activeChar.sendMessage("Manor System: disabled");
			showMainPage(activeChar);
		}

		return true;
	}

	public Enum[] getAdminCommandEnum()
	{
		return Commands.values();
	}

	private void showMainPage(L2Player activeChar)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		TextBuilder replyMSG = new TextBuilder("<html><body>");

		replyMSG.append("<center><font color=\"LEVEL\"> [Manor System] </font></center><br>");
		replyMSG.append("<table width=\"100%\">");
		replyMSG.append("<tr><td>Disabled: " + (CastleManorManager.getInstance().isDisabled() ? "yes" : "no") + "</td>");
		replyMSG.append("<td>Under Maintenance: " + (CastleManorManager.getInstance().isUnderMaintenance() ? "yes" : "no") + "</td></tr>");
		replyMSG.append("<tr><td>Approved: " + (ServerVariables.getBool("ManorApproved") ? "yes" : "no") + "</td></tr>");
		replyMSG.append("</table>");

		replyMSG.append("<center><table>");
		replyMSG.append("<tr><td><button value=\"" + (CastleManorManager.getInstance().isDisabled() ? "Enable" : "Disable") + "\" action=\"bypass -h admin_manor_disable\" width=110 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td><button value=\"Reset\" action=\"bypass -h admin_manor_reset\" width=110 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
		replyMSG.append("<tr><td><button value=\"Refresh\" action=\"bypass -h admin_manor\" width=110 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td><button value=\"Back\" action=\"bypass -h admin_admin\" width=110 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
		replyMSG.append("</table></center>");

		replyMSG.append("<br><center>Castle Information:<table width=\"100%\">");
		replyMSG.append("<tr><td></td><td>Current Period</td><td>Next Period</td></tr>");

		for(Castle c : CastleManager.getInstance().getCastles().values())
			replyMSG.append("<tr><td>" + c.getName() + "</td>" + "<td>" + c.getManorCost(CastleManorManager.PERIOD_CURRENT) + "a</td>" + "<td>" + c.getManorCost(CastleManorManager.PERIOD_NEXT) + "a</td>" + "</tr>");

		replyMSG.append("</table><br>");

		replyMSG.append("</body></html>");

		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
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