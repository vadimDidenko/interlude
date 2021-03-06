package l2d.game.model.instances;

import java.text.SimpleDateFormat;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import l2d.Config;
import l2d.ext.multilang.CustomMessage;
import l2d.game.TradeController;
import l2d.game.cache.Msg;
import l2d.game.model.L2Clan;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.L2Skill.SkillType;
import l2d.game.model.L2TradeList;
import l2d.game.model.Warehouse.WarehouseType;
import l2d.game.model.entity.residence.Residence;
import l2d.game.model.entity.residence.ResidenceFunction;
import l2d.game.model.entity.residence.TeleportLocation;
import l2d.game.model.instances.L2ItemInstance.ItemClass;
import l2d.game.serverpackets.BuyList;
import l2d.game.serverpackets.NpcHtmlMessage;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.serverpackets.WareHouseDepositList;
import l2d.game.serverpackets.WareHouseWithdrawList;
import l2d.game.tables.SkillTable;
import l2d.game.templates.L2NpcTemplate;

public abstract class L2ResidenceManager extends L2NpcInstance
{
	private static Logger _log = Logger.getLogger(L2ResidenceManager.class.getName());

	public L2ResidenceManager(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	protected abstract Residence getResidence();

	protected abstract void broadcastDecoInfo();

	protected abstract int getPrivFunctions();

	protected abstract int getPrivDismiss();

	protected abstract int getPrivDoors();

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		player.sendActionFailed();
		if(player.isActionsDisabled() || !Config.ALLOW_TALK_WHILE_SITTING && player.isSitting() || player.getLastNpc().getDistance(player) > 300)
			return;

		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken();
		String val = "";
		if(st.countTokens() >= 1)
			val = st.nextToken();

		if(actualCommand.equalsIgnoreCase("banish"))
		{
			NpcHtmlMessage html = new NpcHtmlMessage(player, this);
			html.setFile("data/html/residence/Banish.htm");
			sendHtmlMessage(player, html);
		}
		else if(actualCommand.equalsIgnoreCase("banish_foreigner"))
		{
			if(!isHaveRigths(player, getPrivDismiss()))
			{
				player.sendMessage(new CustomMessage("l2d.game.model.instances.L2ResidenceManager.NotAuthorizedToDoThis", player));
				return;
			}
			getResidence().banishForeigner(player);
			return;
		}
		else if(actualCommand.equalsIgnoreCase("Buy"))
		{
			if(val.equals(""))
				return;
			showBuyWindow(player, Integer.valueOf(val));
		}
		else if(actualCommand.equalsIgnoreCase("manage_vault"))
		{
			if(val.equalsIgnoreCase("deposit"))
				showDepositWindowClan(player);
			else if(val.equalsIgnoreCase("withdraw"))
			{
				int value = Integer.valueOf(st.nextToken());
				if(value == 9)
				{
					NpcHtmlMessage html = new NpcHtmlMessage(player, this);
					html.setFile("data/html/residence/clan.htm");
					html.replace("%npcname%", getName());
					player.sendPacket(html);
				}
				else
					showWithdrawWindowClan(player, value);
			}
			else
			{
				NpcHtmlMessage html = new NpcHtmlMessage(player, this);
				html.setFile("data/html/residence/vault.htm");
				sendHtmlMessage(player, html);
			}
			return;
		}
		else if(actualCommand.equalsIgnoreCase("door"))
		{
			if(isHaveRigths(player, getPrivDoors()))
			{
				if(val.equalsIgnoreCase("open"))
					getResidence().openCloseDoors(player, true);
				else if(val.equalsIgnoreCase("close"))
					getResidence().openCloseDoors(player, false);
				NpcHtmlMessage html = new NpcHtmlMessage(player, this);
				html.setFile("data/html/residence/door.htm");
				sendHtmlMessage(player, html);
			}
			else
				player.sendMessage(new CustomMessage("l2d.game.model.instances.L2ResidenceManager.NotAuthorizedToDoThis", player));
		}
		else if(actualCommand.equalsIgnoreCase("functions"))
		{
			if(val.equalsIgnoreCase("tele"))
			{
				if(!getResidence().isFunctionActive(ResidenceFunction.TELEPORT))
				{
					NpcHtmlMessage html = new NpcHtmlMessage(player, this);
					html.setFile("data/html/residence/teleportNotActive.htm");
					sendHtmlMessage(player, html);
					return;
				}
				NpcHtmlMessage html = new NpcHtmlMessage(player, this);
				html.setFile("data/html/residence/teleport.htm");
				String template = "<a action=\"bypass -h scripts_Util:Gatekeeper %loc% %price% @811;%name%\">%name% - %price% Adena</a><br1>";
				String teleport_list = "";
				for(TeleportLocation loc : getResidence().getFunction(ResidenceFunction.TELEPORT).getTeleports())
					teleport_list += template.replaceAll("%loc%", loc._target).replaceAll("%price%", String.valueOf(loc._price)).replaceAll("%name%", loc._name);
				html.replace("%teleList%", teleport_list);
				sendHtmlMessage(player, html);
			}
			else if(val.equalsIgnoreCase("item_creation"))
			{
				if(!getResidence().isFunctionActive(ResidenceFunction.ITEM_CREATE))
				{
					NpcHtmlMessage html = new NpcHtmlMessage(player, this);
					html.setFile("data/html/residence/itemNotActive.htm");
					sendHtmlMessage(player, html);
					return;
				}
				NpcHtmlMessage html = new NpcHtmlMessage(player, this);
				html.setFile("data/html/residence/item.htm");
				String template = "<button value=\"Buy Item\" action=\"bypass -h npc_%objectId%_Buy %id%\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">";
				template = template.replaceAll("%id%", String.valueOf(getResidence().getFunction(ResidenceFunction.ITEM_CREATE).getBuylist()[1])).replace("%objectId%", String.valueOf(getObjectId()));
				html.replace("%itemList%", template);
				sendHtmlMessage(player, html);
			}
			else if(val.equalsIgnoreCase("support"))
			{
				if(!getResidence().isFunctionActive(ResidenceFunction.SUPPORT))
				{
					NpcHtmlMessage html = new NpcHtmlMessage(player, this);
					html.setFile("data/html/residence/supportNotActive.htm");
					sendHtmlMessage(player, html);
					return;
				}
				NpcHtmlMessage html = new NpcHtmlMessage(player, this);
				html.setFile("data/html/residence/support.htm");
				String template = "<a action=\"bypass -h npc_%objectId%_support %id% %level%\">%name%</a><br1>";
				String support_list = "";
				int i = 0;
				for(String[] buff : getResidence().getFunction(ResidenceFunction.SUPPORT).getBuffs())
				{
					support_list += template.replaceAll("%id%", buff[0]).replaceAll("%level%", buff[1]).replaceAll("%name%", buff[3]);
					if(++i % 5 == 0)
						support_list += "<br>";
				}
				html.replace("%magicList%", support_list);
				html.replace("%mp%", String.valueOf(Math.round(getCurrentMp())));
				html.replace("%all%", Config.ALT_CH_ALL_BUFFS ? "<a action=\"bypass -h npc_%objectId%_support all\">Give all</a><br1><a action=\"bypass -h npc_%objectId%_support allW\">Give warrior</a><br1><a action=\"bypass -h npc_%objectId%_support allM\">Give mystic</a><br>" : "");
				sendHtmlMessage(player, html);
			}
			else if(val.equalsIgnoreCase("back"))
				showChatWindow(player, 0);
			else
			{
				NpcHtmlMessage html = new NpcHtmlMessage(player, this);
				html.setFile("data/html/residence/functions.htm");
				if(getResidence().isFunctionActive(ResidenceFunction.RESTORE_EXP))
					html.replace("%xp_regen%", String.valueOf(getResidence().getFunction(ResidenceFunction.RESTORE_EXP).getLevel()) + "%");
				else
					html.replace("%xp_regen%", "0%");
				if(getResidence().isFunctionActive(ResidenceFunction.RESTORE_HP))
					html.replace("%hp_regen%", String.valueOf(getResidence().getFunction(ResidenceFunction.RESTORE_HP).getLevel()) + "%");
				else
					html.replace("%hp_regen%", "0%");
				if(getResidence().isFunctionActive(ResidenceFunction.RESTORE_MP))
					html.replace("%mp_regen%", String.valueOf(getResidence().getFunction(ResidenceFunction.RESTORE_MP).getLevel()) + "%");
				else
					html.replace("%mp_regen%", "0%");
				sendHtmlMessage(player, html);
			}
		}
		else if(actualCommand.equalsIgnoreCase("manage"))
		{
			if(!isHaveRigths(player, getPrivFunctions()))
			{
				player.sendMessage(new CustomMessage("l2d.game.model.instances.L2ResidenceManager.NotAuthorizedToDoThis", player));
				return;
			}

			if(val.equalsIgnoreCase("recovery"))
			{
				if(st.countTokens() >= 1)
				{
					val = st.nextToken();
					boolean success = true;
					if(val.equalsIgnoreCase("hp"))
						success = getResidence().updateFunctions(ResidenceFunction.RESTORE_HP, Integer.valueOf(st.nextToken()));
					else if(val.equalsIgnoreCase("mp"))
						success = getResidence().updateFunctions(ResidenceFunction.RESTORE_MP, Integer.valueOf(st.nextToken()));
					else if(val.equalsIgnoreCase("exp"))
						success = getResidence().updateFunctions(ResidenceFunction.RESTORE_EXP, Integer.valueOf(st.nextToken()));
					if(!success)
						player.sendPacket(new SystemMessage(SystemMessage.THERE_IS_NOT_ENOUGH_ADENA_IN_THE_CLAN_HALL_WAREHOUSE));
					else
						broadcastDecoInfo();
				}
				showManageRecovery(player);
			}
			else if(val.equalsIgnoreCase("other"))
			{
				if(st.countTokens() >= 1)
				{
					val = st.nextToken();
					boolean success = true;
					if(val.equalsIgnoreCase("item"))
						success = getResidence().updateFunctions(ResidenceFunction.ITEM_CREATE, Integer.valueOf(st.nextToken()));
					else if(val.equalsIgnoreCase("tele"))
						success = getResidence().updateFunctions(ResidenceFunction.TELEPORT, Integer.valueOf(st.nextToken()));
					else if(val.equalsIgnoreCase("support"))
						success = getResidence().updateFunctions(ResidenceFunction.SUPPORT, Integer.valueOf(st.nextToken()));
					if(!success)
						player.sendPacket(new SystemMessage(SystemMessage.THERE_IS_NOT_ENOUGH_ADENA_IN_THE_CLAN_HALL_WAREHOUSE));
					else
						broadcastDecoInfo();
				}
				showManageOther(player);
			}
			else if(val.equalsIgnoreCase("deco"))
			{
				if(st.countTokens() >= 1)
				{
					val = st.nextToken();
					boolean success = true;
					if(val.equalsIgnoreCase("platform"))
						success = getResidence().updateFunctions(ResidenceFunction.PLATFORM, Integer.valueOf(st.nextToken()));
					else if(val.equalsIgnoreCase("curtain"))
						success = getResidence().updateFunctions(ResidenceFunction.CURTAIN, Integer.valueOf(st.nextToken()));
					if(!success)
						player.sendPacket(new SystemMessage(SystemMessage.THERE_IS_NOT_ENOUGH_ADENA_IN_THE_CLAN_HALL_WAREHOUSE));
					else
						broadcastDecoInfo();
				}
				showManageDeco(player);
			}
			else if(val.equalsIgnoreCase("back"))
				showChatWindow(player, 0);
			else
			{
				NpcHtmlMessage html = new NpcHtmlMessage(player, this);
				html.setFile("data/html/residence/manage.htm");
				sendHtmlMessage(player, html);
			}
			return;
		}
		else if(actualCommand.equalsIgnoreCase("support"))
		{
			setTarget(player);
			if(val.equals(""))
				return;

			if(!getResidence().isFunctionActive(ResidenceFunction.SUPPORT))
				return;

			if(val.startsWith("all"))
				for(String[] buff : getResidence().getFunction(ResidenceFunction.SUPPORT).getBuffs())
				{
					if(val.equals("allM") && buff[2] == ResidenceFunction.W || val.equals("allW") && buff[2] == ResidenceFunction.M)
						continue;
					if(!useSkill(Integer.parseInt(buff[0]), Integer.parseInt(buff[1]), player))
						break;
				}
			else
			{
				int skill_id = Integer.parseInt(val);
				int skill_lvl = 0;
				if(st.countTokens() >= 1)
					skill_lvl = Integer.parseInt(st.nextToken());
				useSkill(skill_id, skill_lvl, player);
			}

			onBypassFeedback(player, "functions support");
			return;
		}
		super.onBypassFeedback(player, command);
	}

	private boolean useSkill(int id, int level, L2Player player)
	{
		L2Skill skill = SkillTable.getInstance().getInfo(id, level);
		if(skill == null)
		{
			player.sendMessage("Invalid skill " + id);
			return true;
		}
		if(skill.getMpConsume() > getCurrentMp())
		{
			NpcHtmlMessage html = new NpcHtmlMessage(player, this);
			html.setFile("data/html/residence/NeedCoolTime.htm");
			html.replace("%mp%", String.valueOf(Math.round(getCurrentMp())));
			sendHtmlMessage(player, html);
			return false;
		}
		altUseSkill(skill, player);
		if(Config.ALT_BUFF_SUMMON && skill.getSkillType() != SkillType.SUMMON && player.getPet() != null && !player.getPet().isDead())
			altUseSkill(skill, player.getPet());
		return true;
	}

	private void sendHtmlMessage(L2Player player, NpcHtmlMessage html)
	{
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}

	private void showDepositWindowClan(L2Player player)
	{
		if(!player.getPlayerAccess().UseWarehouse)
			return;

		if(player.getClan() == null)
		{
			player.sendActionFailed();
			return;
		}

		if(player.getClan().getLevel() == 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.ONLY_CLANS_OF_CLAN_LEVEL_1_OR_HIGHER_CAN_USE_A_CLAN_WAREHOUSE));
			player.sendActionFailed();
			return;
		}

		player.setUsingWarehouseType(WarehouseType.CLAN);
		player.tempInvetoryDisable();

		if(!(player.isClanLeader() || player.getVarB("canWhWithdraw") && (player.getClanPrivileges() & L2Clan.CP_CL_VIEW_WAREHOUSE) == L2Clan.CP_CL_VIEW_WAREHOUSE))
			player.sendPacket(Msg.ITEMS_LEFT_AT_THE_CLAN_HALL_WAREHOUSE_CAN_ONLY_BE_RETRIEVED_BY_THE_CLAN_LEADER_DO_YOU_WANT_TO_CONTINUE);

		player.sendPacket(new WareHouseDepositList(player, WarehouseType.CLAN));
	}

	private void showWithdrawWindowClan(L2Player player, int val)
	{
		if(!player.getPlayerAccess().UseWarehouse)
			return;

		if(player.getClan() == null)
		{
			player.sendActionFailed();
			return;
		}

		L2Clan _clan = player.getClan();

		if(_clan.getLevel() == 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.ONLY_CLANS_OF_CLAN_LEVEL_1_OR_HIGHER_CAN_USE_A_CLAN_WAREHOUSE));
			player.sendActionFailed();
			return;
		}

		if(isHaveRigths(player, L2Clan.CP_CL_VIEW_WAREHOUSE))
		{
			player.setUsingWarehouseType(WarehouseType.CLAN);
			player.tempInvetoryDisable();
			player.sendPacket(new WareHouseWithdrawList(player, WarehouseType.CLAN, ItemClass.values()[val]));
		}
		else
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_THE_RIGHT_TO_USE_THE_CLAN_WAREHOUSE));
			player.sendActionFailed();
		}
	}

	private void replace(NpcHtmlMessage html, int type, String replace1, String replace2)
	{
		SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		boolean proc = type == ResidenceFunction.RESTORE_HP || type == ResidenceFunction.RESTORE_MP || type == ResidenceFunction.RESTORE_EXP;
		if(getResidence().isFunctionActive(type))
		{
			html.replace("%" + replace1 + "%", String.valueOf(getResidence().getFunction(type).getLevel()) + (proc ? "%" : ""));
			html.replace("%" + replace1 + "Price%", String.valueOf(getResidence().getFunction(type).getLease()));
			html.replace("%" + replace1 + "Date%", format.format(getResidence().getFunction(type).getEndTimeInMillis()));
		}
		else
		{
			html.replace("%" + replace1 + "%", "0");
			html.replace("%" + replace1 + "Price%", "0");
			html.replace("%" + replace1 + "Date%", "0");
		}
		if(getResidence().getFunction(type) != null && getResidence().getFunction(type).getLevels().size() > 0)
		{
			String out = "[<a action=\"bypass -h npc_%objectId%_manage " + replace2 + " " + replace1 + " 0\">Stop</a>]";
			for(int level : getResidence().getFunction(type).getLevels())
				out += "[<a action=\"bypass -h npc_%objectId%_manage " + replace2 + " " + replace1 + " " + level + "\">" + level + (proc ? "%" : "") + "</a>]";
			html.replace("%" + replace1 + "Manage%", out);
		}
		else
			html.replace("%" + replace1 + "Manage%", "Not Available");
	}

	private void showManageRecovery(L2Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(player, this);
		html.setFile("data/html/residence/edit_recovery.htm");

		replace(html, ResidenceFunction.RESTORE_EXP, "exp", "recovery");
		replace(html, ResidenceFunction.RESTORE_HP, "hp", "recovery");
		replace(html, ResidenceFunction.RESTORE_MP, "mp", "recovery");

		sendHtmlMessage(player, html);
	}

	private void showManageOther(L2Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(player, this);
		html.setFile("data/html/residence/edit_other.htm");

		replace(html, ResidenceFunction.TELEPORT, "tele", "other");
		replace(html, ResidenceFunction.SUPPORT, "support", "other");
		replace(html, ResidenceFunction.ITEM_CREATE, "item", "other");

		sendHtmlMessage(player, html);
	}

	private void showManageDeco(L2Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(player, this);
		html.setFile("data/html/residence/edit_deco.htm");

		replace(html, ResidenceFunction.CURTAIN, "curtain", "deco");
		replace(html, ResidenceFunction.PLATFORM, "platform", "deco");

		sendHtmlMessage(player, html);
	}

	protected void showBuyWindow(L2Player player, int val)
	{
		if(!player.getPlayerAccess().UseShop)
			return;
		double taxRate = 0;
		if(getCastle() != null)
			taxRate = getCastle().getTaxRate();
		player.tempInvetoryDisable();
		L2TradeList list = TradeController.getInstance().getBuyList(val);
		if(list != null && list.getNpcId().equals(String.valueOf(getNpcId())))
			player.sendPacket(new BuyList(list, player, taxRate));
		else
		{
			_log.warning("[L2ResidenceManager] possible client hacker: " + player.getName() + " attempting to buy from GM shop! < Ban him!");
			_log.warning("buylist id:" + val + " / list_npc = " + (list == null ? "nulllist" : list.getNpcId()) + " / npc = " + getNpcId());
		}
	}

	protected boolean isHaveRigths(L2Player player, int rigthsToCheck)
	{
		if(player.isGM())
			return true;
		return player.getClan() != null && (player.getClanPrivileges() & rigthsToCheck) == rigthsToCheck;
	}
}