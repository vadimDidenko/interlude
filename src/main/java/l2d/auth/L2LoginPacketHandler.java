package l2d.auth;

import java.nio.ByteBuffer;

import l2d.auth.L2LoginClient.LoginClientState;
import l2d.auth.clientpackets.AuthGameGuard;
import l2d.auth.clientpackets.RequestAuthLogin;
import l2d.auth.clientpackets.RequestServerList;
import l2d.auth.clientpackets.RequestServerLogin;
import l2d.ext.network.IPacketHandler;
import l2d.ext.network.ReceivablePacket;

/**
 * Handler for packets received by Login Server
 * 
 * @author KenM
 */
public final class L2LoginPacketHandler implements IPacketHandler<L2LoginClient>
{
	@Override
	public ReceivablePacket<L2LoginClient> handlePacket(ByteBuffer buf, L2LoginClient client)
	{
		int opcode = buf.get() & 0xFF;

		ReceivablePacket<L2LoginClient> packet = null;
		LoginClientState state = client.getState();

		switch(state)
		{
			case CONNECTED:
				if(opcode == 0x07)
					packet = new AuthGameGuard();
				else
					debugOpcode(opcode, state, client);
				break;
			case AUTHED_GG:
				if(opcode == 0x00)
					packet = new RequestAuthLogin();
				else if(opcode != 0x05) // на случай когда клиент зажимает ентер
					debugOpcode(opcode, state, client);
				break;
			case AUTHED_LOGIN:
				if(opcode == 0x05)
					packet = new RequestServerList();
				else if(opcode == 0x02)
					packet = new RequestServerLogin();
				else
					debugOpcode(opcode, state, client);
				break;
		}
		return packet;
	}

	private void debugOpcode(int opcode, LoginClientState state, L2LoginClient client)
	{
		System.out.println("Unknown Opcode: " + opcode + " for state: " + state.name() + " from IP: " + client);
	}
}
