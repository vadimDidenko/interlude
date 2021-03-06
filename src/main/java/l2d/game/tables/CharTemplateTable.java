package l2d.game.tables;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Logger;

import l2d.db.DatabaseUtils;
import l2d.db.FiltredPreparedStatement;
import l2d.db.L2DatabaseFactory;
import l2d.db.ThreadConnection;
import l2d.game.model.base.ClassId;
import l2d.game.templates.L2PlayerTemplate;
import l2d.game.templates.StatsSet;

@SuppressWarnings({ "nls", "unqualified-field-access", "boxing" })
public class CharTemplateTable
{
	private static final Logger _log = Logger.getLogger(CharTemplateTable.class.getName());

	private static CharTemplateTable _instance;

	private HashMap<Integer, L2PlayerTemplate> _templates;

	public static final String[] charClasses = {
			"Human Fighter",
			"Warrior",
			"Gladiator",
			"Warlord",
			"Human Knight",
			"Paladin",
			"Dark Avenger",
			"Rogue",
			"Treasure Hunter",
			"Hawkeye",
			"Human Mystic",
			"Human Wizard",
			"Sorceror",
			"Necromancer",
			"Warlock",
			"Cleric",
			"Bishop",
			"Prophet",
			"Elven Fighter",
			"Elven Knight",
			"Temple Knight",
			"Swordsinger",
			"Elven Scout",
			"Plainswalker",
			"Silver Ranger",
			"Elven Mystic",
			"Elven Wizard",
			"Spellsinger",
			"Elemental Summoner",
			"Elven Oracle",
			"Elven Elder",
			"Dark Fighter",
			"Palus Knight",
			"Shillien Knight",
			"Bladedancer",
			"Assassin",
			"Abyss Walker",
			"Phantom Ranger",
			"Dark Elven Mystic",
			"Dark Elven Wizard",
			"Spellhowler",
			"Phantom Summoner",
			"Shillien Oracle",
			"Shillien Elder",
			"Orc Fighter",
			"Orc Raider",
			"Destroyer",
			"Orc Monk",
			"Tyrant",
			"Orc Mystic",
			"Orc Shaman",
			"Overlord",
			"Warcryer",
			"Dwarven Fighter",
			"Dwarven Scavenger",
			"Bounty Hunter",
			"Dwarven Artisan",
			"Warsmith",
			"dummyEntry1",
			"dummyEntry2",
			"dummyEntry3",
			"dummyEntry4",
			"dummyEntry5",
			"dummyEntry6",
			"dummyEntry7",
			"dummyEntry8",
			"dummyEntry9",
			"dummyEntry10",
			"dummyEntry11",
			"dummyEntry12",
			"dummyEntry13",
			"dummyEntry14",
			"dummyEntry15",
			"dummyEntry16",
			"dummyEntry17",
			"dummyEntry18",
			"dummyEntry19",
			"dummyEntry20",
			"dummyEntry21",
			"dummyEntry22",
			"dummyEntry23",
			"dummyEntry24",
			"dummyEntry25",
			"dummyEntry26",
			"dummyEntry27",
			"dummyEntry28",
			"dummyEntry29",
			"dummyEntry30",
			"Duelist",
			"DreadNought",
			"Phoenix Knight",
			"Hell Knight",
			"Sagittarius",
			"Adventurer",
			"Archmage",
			"Soultaker",
			"Arcana Lord",
			"Cardinal",
			"Hierophant",
			"Eva Templar",
			"Sword Muse",
			"Wind Rider",
			"Moonlight Sentinel",
			"Mystic Muse",
			"Elemental Master",
			"Eva's Saint",
			"Shillien Templar",
			"Spectral Dancer",
			"Ghost Hunter",
			"Ghost Sentinel",
			"Storm Screamer",
			"Spectral Master",
			"Shillien Saint",
			"Titan",
			"Grand Khauatari",
			"Dominator",
			"Doomcryer",
			"Fortune Seeker",
			"Maestro" };

	public static CharTemplateTable getInstance()
	{
		if(_instance == null)
			_instance = new CharTemplateTable();
		return _instance;
	}

	private CharTemplateTable()
	{
		_templates = new HashMap<Integer, L2PlayerTemplate>();
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM class_list, char_templates, lvlupgain WHERE class_list.id = char_templates.classId AND class_list.id = lvlupgain.classId ORDER BY class_list.id");
			rset = statement.executeQuery();

			while(rset.next())
			{
				StatsSet set = new StatsSet();
				ClassId classId = ClassId.values()[rset.getInt("class_list.id")];
				set.set("classId", rset.getInt("class_list.id"));
				set.set("className", rset.getString("char_templates.className"));
				set.set("raceId", rset.getByte("char_templates.RaceId"));
				set.set("baseSTR", rset.getByte("char_templates.STR"));
				set.set("baseCON", rset.getByte("char_templates.CON"));
				set.set("baseDEX", rset.getByte("char_templates.DEX"));
				set.set("baseINT", rset.getByte("char_templates._INT"));
				set.set("baseWIT", rset.getByte("char_templates.WIT"));
				set.set("baseMEN", rset.getByte("char_templates.MEN"));
				set.set("baseHpMax", rset.getFloat("lvlupgain.defaultHpBase"));
				set.set("lvlHpAdd", rset.getFloat("lvlupgain.defaultHpAdd"));
				set.set("lvlHpMod", rset.getFloat("lvlupgain.defaultHpMod"));
				set.set("baseMpMax", rset.getFloat("lvlupgain.defaultMpBase"));
				set.set("baseCpMax", rset.getFloat("lvlupgain.defaultCpBase"));
				set.set("lvlCpAdd", rset.getFloat("lvlupgain.defaultCpAdd"));
				set.set("lvlCpMod", rset.getFloat("lvlupgain.defaultCpMod"));
				set.set("lvlMpAdd", rset.getFloat("lvlupgain.defaultMpAdd"));
				set.set("lvlMpMod", rset.getFloat("lvlupgain.defaultMpMod"));
				set.set("baseHpReg", 0.01);
				set.set("baseCpReg", 0.01);
				set.set("baseMpReg", 0.01);
				set.set("basePAtk", rset.getInt("char_templates.p_atk"));
				set.set("basePDef", /* classId.isMage()? 77 : 129 */rset.getInt("char_templates.p_def"));
				set.set("baseMAtk", rset.getInt("char_templates.m_atk"));
				set.set("baseMDef", 41 /* rset.getInt("char_templates.m_def") */);
				set.set("classBaseLevel", rset.getInt("lvlupgain.class_lvl"));
				set.set("basePAtkSpd", rset.getInt("char_templates.p_spd"));
				set.set("baseMAtkSpd", classId.isMage() ? 166 : 333 /* rset.getInt("char_templates.m_spd") */);
				set.set("baseCritRate", rset.getInt("char_templates.critical"));
				set.set("baseWalkSpd", rset.getInt("char_templates.walk_spd"));
				set.set("baseRunSpd", rset.getInt("char_templates.run_spd"));
				set.set("baseShldDef", 0);
				set.set("baseShldRate", 0);
				set.set("baseAtkRange", 40);

				set.set("spawnX", rset.getInt("char_templates.x"));
				set.set("spawnY", rset.getInt("char_templates.y"));
				set.set("spawnZ", rset.getInt("char_templates.z"));

				L2PlayerTemplate ct;

				//
				// Male class
				//
				set.set("isMale", true);
				// set.setMUnk1(rset.getDouble(27));
				// set.setMUnk2(rset.getDouble(28));
				set.set("collision_radius", rset.getDouble("char_templates.m_col_r"));
				set.set("collision_height", rset.getDouble("char_templates.m_col_h"));
				ct = new L2PlayerTemplate(set);
				// 5items must go here
				for(int x = 1; x < 6; x++)
					if(rset.getInt("char_templates.items" + x) != 0)
						ct.addItem(rset.getInt("char_templates.items" + x));
				_templates.put(ct.classId.getId(), ct);

				//
				// Female class
				//
				set.set("isMale", false);
				// set.setFUnk1(rset.getDouble(31));
				// set.setFUnk2(rset.getDouble(32));
				set.set("collision_radius", rset.getDouble("char_templates.f_col_r"));
				set.set("collision_height", rset.getDouble("char_templates.f_col_h"));
				ct = new L2PlayerTemplate(set);
				// 5items must go here
				for(int x = 1; x < 6; x++)
					if(rset.getInt("char_templates.items" + x) != 0)
						ct.addItem(rset.getInt("char_templates.items" + x));
				_templates.put(ct.classId.getId() | 0x100, ct);
			}
		}
		catch(SQLException e)
		{
			_log.warning(" ~ Error while loading char templates " + e.getMessage());
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}

		_log.info(" [Char Template Table ]");
		_log.info(" ~ Loaded " + _templates.size() + " Character Templates.");
		_log.info(" [Char Template Table ]\n");
	}

	public L2PlayerTemplate getTemplate(ClassId classId, boolean female)
	{
		return getTemplate(classId.getId(), female);
	}

	public L2PlayerTemplate getTemplate(int classId, boolean female)
	{
		int key = classId;
		if(female)
			key |= 0x100;
		return _templates.get(key);
	}

	public static String getClassNameById(int classId)
	{
		return charClasses[classId];
	}

	public static int getClassIdByName(String className)
	{
		int currId = 1;

		for(String name : charClasses)
		{
			if(name.equalsIgnoreCase(className))
				break;

			currId++;
		}

		return currId;
	}
}
