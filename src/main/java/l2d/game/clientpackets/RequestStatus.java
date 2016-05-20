package l2d.game.clientpackets;

import java.util.logging.Logger;

import l2d.game.serverpackets.SendStatus;

public final class RequestStatus extends L2GameClientPacket
{
	static Logger _log = Logger.getLogger(RequestStatus.class.getName());

	@Override
	protected void readImpl()
	{}

	@Override
	protected void runImpl()
	{
		getClient().close(new SendStatus());
	}
}