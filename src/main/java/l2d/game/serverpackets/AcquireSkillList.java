package l2d.game.serverpackets;

import javolution.util.FastList;

/**
 * sample
 *
 * a3
 * 05000000
 * 03000000 03000000 06000000 3c000000 00000000 	power strike
 * 10000000 02000000 06000000 3c000000 00000000 	mortal blow
 * 38000000 04000000 06000000 36010000 00000000 	power shot
 * 4d000000 01000000 01000000 98030000 01000000 	ATTACK aura  920sp
 * 8e000000 03000000 03000000 cc010000 00000000     Armor Mastery
 *
 * format   d (ddddd)
 * skillid, level, maxlevel?,
 */
public class AcquireSkillList extends L2GameServerPacket
{
	private final FastList<Skill> _skills;
	private int _skillsType;
	public static final int USUAL = 0;
	public static final int FISHING = 1;
	public static final int CLAN = 2;

	class Skill
	{
		public int id;
		public int nextLevel;
		public int maxLevel;
		public int spCost;
		public int requirements;

		Skill(int id, int nextLevel, int maxLevel, int spCost, int requirements)
		{
			this.id = id;
			this.nextLevel = nextLevel;
			this.maxLevel = maxLevel;
			this.spCost = spCost;
			this.requirements = requirements;
		}
	}

	public AcquireSkillList(int type)
	{
		_skills = new FastList<Skill>();
		_skillsType = type;
	}

	public void addSkill(int id, int nextLevel, int maxLevel, int Cost, int requirements)
	{
		_skills.add(new Skill(id, nextLevel, maxLevel, Cost, requirements));
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x8a);
		writeD(_skillsType); // Kamael: 0: standart, 1: fishing, 2: clans, 3:
		writeD(_skills.size());

		for(Skill temp : _skills)
		{
			writeD(temp.id);
			writeD(temp.nextLevel);
			writeD(temp.maxLevel);
			writeD(temp.spCost);
			writeD(temp.requirements);
		}
	}
}