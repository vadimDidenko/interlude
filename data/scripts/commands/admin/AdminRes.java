package commands.admin;

import l2d.ext.scripts.ScriptFile;
import l2d.game.cache.Msg;
import l2d.game.handler.AdminCommandHandler;
import l2d.game.handler.IAdminCommandHandler;
import l2d.game.model.L2Character;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;
import l2d.game.serverpackets.Revive;
import l2d.game.serverpackets.SocialAction;
import l2d.game.taskmanager.DecayTaskManager;
import l2d.util.Log;

@SuppressWarnings("unused")
public class AdminRes implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_res
	}

	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, L2Player activeChar)
	{
		Commands command = (Commands) comm;

		if(!activeChar.getPlayerAccess().Res)
			return false;

		if(fullString.startsWith("admin_res "))
			handleRes(activeChar, wordList[1]);
		if(fullString.equals("admin_res"))
			handleRes(activeChar);

		return true;
	}

	public Enum[] getAdminCommandEnum()
	{
		return Commands.values();
	}

	private void handleRes(L2Player activeChar)
	{
		handleRes(activeChar, null);
	}

	private void handleRes(L2Player activeChar, String player)
	{
		L2Object obj = activeChar.getTarget();
		if(player != null)
		{
			L2Player plyr = L2World.getPlayer(player);
			if(plyr != null)
				obj = plyr;
			else
				try
				{
					int radius = Math.max(Integer.parseInt(player), 100);
					for(L2Character character : activeChar.getAroundCharacters(radius, 200))
					{
						character.setCurrentHpMp(character.getMaxHp(), character.getMaxMp(), true);
						character.setCurrentCp(character.getMaxCp());
						if(character.isPlayer())
							((L2Player) character).restoreExp();
						// If the target is an NPC, then abort it's auto decay and respawn.
						else
							DecayTaskManager.getInstance().cancelDecayTask(character);

						character.broadcastPacket(new SocialAction(character.getObjectId(), 15));
						character.broadcastPacket(new Revive(character));
						character.doRevive();
					}
					activeChar.sendMessage("Resurrected within " + radius + " unit radius.");
					return;
				}
				catch(NumberFormatException e)
				{
					activeChar.sendMessage("Enter valid player name or radius");
					return;
				}
		}

		if(obj == null)
			obj = activeChar;
		if(obj.isCharacter())
		{
			L2Character target = (L2Character) obj;
			if(!target.isDead())
				return;

			target.setCurrentHpMp(target.getMaxHp(), target.getMaxMp(), true);
			target.setCurrentCp(target.getMaxCp());
			// GM Resurrection will restore any lost exp
			if(target.isPlayer())
			{
				L2Player deadplayer = (L2Player) target;
				deadplayer.restoreExp();
			}
			target.broadcastPacket(new SocialAction(target.getObjectId(), 15));
			target.broadcastPacket(new Revive(target));
			target.doRevive();
			Log.add("resurrected character " + target.getObjectId() + " " + target.getName(), "gm_ext_actions", activeChar);
		}
		else
			activeChar.sendPacket(Msg.INVALID_TARGET);
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