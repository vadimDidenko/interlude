package l2d.game.clientpackets;

import l2d.game.model.L2Macro;
import l2d.game.model.L2Macro.L2MacroCmd;
import l2d.game.model.L2Player;
import l2d.game.serverpackets.SystemMessage;

/**
 * packet type id 0xcd
 *
 * sample
 *
 * cd
 * d // id
 * S // macro name
 * S // unknown  desc
 * S // unknown  acronym
 * c // icon
 * c // count
 *
 * c // entry
 * c // type
 * d // skill id
 * c // shortcut id
 * S // command name
 *
 * format:		cdSSScc (ccdcS)
 */
public class RequestMakeMacro extends L2GameClientPacket
{
	private L2Macro _macro;

	@Override
	public void readImpl()
	{
		int _id = readD();
		String _name = readS();
		String _desc = readS();
		String _acronym = readS();
		int _icon = readC();
		int _count = readC();
		if(_count > 12)
			_count = 12;
		L2MacroCmd[] commands = new L2MacroCmd[_count];
		for(int i = 0; i < _count; i++)
		{
			int entry = readC();
			int type = readC(); // 1 = skill, 3 = action, 4 = shortcut
			int d1 = readD(); // skill or page number for shortcuts
			int d2 = readC();
			String command = readS().replace(";", "").replace(",", "");
			commands[i] = new L2MacroCmd(entry, type, d1, d2, command);
		}
		_macro = new L2Macro(_id, _icon, _name, _desc, _acronym, commands);
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar.getMacroses().getAllMacroses().length > 48)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_MAY_CREATE_UP_TO_48_MACROS));
			return;
		}

		if(_macro.name.length() == 0)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.ENTER_THE_NAME_OF_THE_MACRO));
			return;
		}

		if(_macro.descr.length() > 32)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.MACRO_DESCRIPTIONS_MAY_CONTAIN_UP_TO_32_CHARACTERS));
			return;
		}

		activeChar.registerMacro(_macro);
	}
}