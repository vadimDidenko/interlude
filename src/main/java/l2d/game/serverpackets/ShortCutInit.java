package l2d.game.serverpackets;

import java.util.Collection;

import l2d.game.model.L2Player;
import l2d.game.model.L2ShortCut;

/**
 * sample
 *
 * 45
 * 0d 00 00 00
 * 03 00 00 00  00 00 00 00  02 00 00 00  01 00 00 00
 * 01 00 00 00  01 00 00 00  46 28 91 40  01 00 00 00
 * 02 00 00 00  02 00 00 00  03 00 00 00  06 00 00 00  01 00 00 00
 * 02 00 00 00  03 00 00 00  38 00 00 00  06 00 00 00  01 00 00 00
 * 01 00 00 00  04 00 00 00  5f 37 32 43  01 00 00 00
 * 03 00 00 00  05 00 00 00  05 00 00 00  01 00 00 00
 * 01 00 00 00  06 00 00 00  3a df c3 41  01 00 00 00
 * 01 00 00 00  07 00 00 00  5d 69 d1 41  01 00 00 00
 * 01 00 00 00  08 00 00 00  7b 86 73 42  01 00 00 00
 * 03 00 00 00  09 00 00 00  00 00 00 00  01 00 00 00
 * 02 00 00 00  0a 00 00 00  4d 00 00 00  01 00 00 00  01 00 00 00
 * 02 00 00 00  0b 00 00 00  5b 00 00 00  01 00 00 00  01 00 00 00
 * 01 00 00 00  0c 00 00 00  5f 37 32 43  01 00 00 00

 * format   d *(1dddd)/(2ddddd)/(3dddd)
 */
public class ShortCutInit extends L2GameServerPacket
{
	private Collection<L2ShortCut> _shortCuts;

	public ShortCutInit(L2Player pl)
	{
		_shortCuts = pl.getAllShortCuts();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x45);
		writeD(_shortCuts.size());

		for(final L2ShortCut sc : _shortCuts)
		{
			writeD(sc.type);
			writeD(sc.slot + sc.page * 12);

			switch(sc.type)
			{
				case L2ShortCut.TYPE_ITEM:
					writeD(sc.id);
					writeD(0x01);
					writeD(-1);
					writeD(0x00);
					writeD(0x00);
					writeD(0x00);
					break;
				case L2ShortCut.TYPE_SKILL:
					writeD(sc.id);
					writeD(sc.level);
					writeC(0x00);
					writeD(0x01);
					break;
				case L2ShortCut.TYPE_ACTION:
				case L2ShortCut.TYPE_MACRO:
				case L2ShortCut.TYPE_RECIPE:
					writeD(sc.id);
					writeD(0x01);
			}
		}

	}
}