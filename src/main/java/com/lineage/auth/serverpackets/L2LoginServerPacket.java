package com.lineage.auth.serverpackets;

import com.lineage.auth.L2LoginClient;
import com.lineage.ext.network.SendablePacket;

/**
 * @author KenM
 */
public abstract class L2LoginServerPacket extends SendablePacket<L2LoginClient>
{
	@Override
	protected int getHeaderSize()
	{
		return 2;
	}

	@Override
	protected void writeHeader(int dataSize)
	{
		writeH(dataSize + getHeaderSize());
	}
}