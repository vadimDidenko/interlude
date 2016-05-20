package l2d.game.templates;

public class L2CharTemplate
{
	private StatsSet _set;

	// BaseStats
	public final byte baseSTR;
	public final byte baseCON;
	public final byte baseDEX;
	public final byte baseINT;
	public final byte baseWIT;
	public final byte baseMEN;
	public float baseHpMax;
	public final float baseCpMax;
	public final float baseMpMax;

	/** HP Regen base */
	public float baseHpReg;

	/** MP Regen base */
	public float baseMpReg;

	/** CP Regen base */
	public final float baseCpReg;

	public int basePAtk;
	public int baseMAtk;
	public int basePDef;
	public final int baseMDef;
	public final int basePAtkSpd;
	public final int baseMAtkSpd;
	public final int baseShldDef;
	public final int baseAtkRange;
	public final int baseShldRate;
	public final int baseCritRate;
	public final int baseRunSpd;
	public final int baseWalkSpd;

	public float collisionRadius;
	public float collisionHeight;

	public L2CharTemplate(StatsSet set)
	{
		_set = set;

		// Base stats
		baseSTR = set.getByte("baseSTR");
		baseCON = set.getByte("baseCON");
		baseDEX = set.getByte("baseDEX");
		baseINT = set.getByte("baseINT");
		baseWIT = set.getByte("baseWIT");
		baseMEN = set.getByte("baseMEN");
		baseHpMax = set.getFloat("baseHpMax");
		baseCpMax = set.getFloat("baseCpMax");
		baseMpMax = set.getFloat("baseMpMax");
		baseHpReg = set.getFloat("baseHpReg");
		baseCpReg = set.getFloat("baseCpReg");
		baseMpReg = set.getFloat("baseMpReg");
		basePAtk = set.getInteger("basePAtk");
		baseMAtk = set.getInteger("baseMAtk");
		basePDef = set.getInteger("basePDef");
		baseMDef = set.getInteger("baseMDef");
		basePAtkSpd = set.getInteger("basePAtkSpd");
		baseMAtkSpd = set.getInteger("baseMAtkSpd");
		baseShldDef = set.getInteger("baseShldDef");
		baseAtkRange = set.getInteger("baseAtkRange");
		baseShldRate = set.getInteger("baseShldRate");
		baseCritRate = set.getInteger("baseCritRate");
		baseRunSpd = set.getInteger("baseRunSpd");
		baseWalkSpd = set.getInteger("baseWalkSpd");

		// Geometry
		collisionRadius = set.getFloat("collision_radius", 5);
		collisionHeight = set.getFloat("collision_height", 5);
	}

	public int getNpcId()
	{
		return 0;
	}

	public StatsSet getSet()
	{
		return _set;
	}

	public void setSet(StatsSet set)
	{
		_set = set;
	}
}