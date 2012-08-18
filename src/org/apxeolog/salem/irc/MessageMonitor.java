package org.apxeolog.salem.irc;

import f00f.net.irc.martyr.GenericAutoService;
import f00f.net.irc.martyr.InCommand;
import f00f.net.irc.martyr.State;
import f00f.net.irc.martyr.commands.MessageCommand;

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
		if(command instanceof MessageCommand) {
			MessageCommand msg = (MessageCommand)command;
			provider.incomingMessage(msg);
		}
	}

	@Override
	protected void updateState(State state) {
		if (state == State.REGISTERED) {
			provider.onRegistered();
		}
	}
}
