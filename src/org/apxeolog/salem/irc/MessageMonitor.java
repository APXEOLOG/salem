package org.apxeolog.salem.irc;

import org.apxeolog.salem.ALS;

import f00f.net.irc.martyr.GenericAutoService;
import f00f.net.irc.martyr.InCommand;
import f00f.net.irc.martyr.State;
import f00f.net.irc.martyr.commands.MessageCommand;
import f00f.net.irc.martyr.replies.NamesEndReply;

public class MessageMonitor extends GenericAutoService
{
	private IRCProvider provider;

	public MessageMonitor(IRCProvider provider)	{
		super(provider.getConnection());
		this.provider = provider;
		enable();
	}

	@Override
	public void updateCommand(InCommand command) {
		ALS.alDebugPrint("com", command);
		if(command instanceof MessageCommand) {
			MessageCommand msg = (MessageCommand)command;
			provider.incomingMessage(msg);
		}
		else if(command instanceof NamesEndReply) {
			provider.printMembers();
		}
	}

	@Override
	protected void updateState(State state) {
		if (state == State.REGISTERED) {
			provider.onRegistered();
		}
	}
}
