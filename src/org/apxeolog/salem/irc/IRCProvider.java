package org.apxeolog.salem.irc;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import org.apxeolog.salem.ALS;
import org.apxeolog.salem.SChatWrapper;
import org.apxeolog.salem.config.XConfig;

import f00f.net.irc.martyr.IRCConnection;
import f00f.net.irc.martyr.OutCommand;
import f00f.net.irc.martyr.State;
import f00f.net.irc.martyr.clientstate.ClientState;
import f00f.net.irc.martyr.clientstate.Member;
import f00f.net.irc.martyr.commands.JoinCommand;
import f00f.net.irc.martyr.commands.MessageCommand;
import f00f.net.irc.martyr.services.AutoReconnect;
import f00f.net.irc.martyr.services.AutoRegister;
import f00f.net.irc.martyr.services.AutoResponder;

public class IRCProvider {
	private IRCConnection connection;
	private AutoReconnect autoReconnect;
	private String server;
	private int port;
	private ClientState clientState;
	private RegisteredListener listener;

	public IRCProvider(String server, int port, String name, String pwd) {
		this.server = server;
		this.port = port;
		clientState = new ClientState();
		connection = new IRCConnection(clientState);
		new AutoResponder(connection);
		new AutoRegister(connection, name, "BDSaleM", name, pwd);
		new MessageMonitor(this);
		autoReconnect = new AutoReconnect(connection);
	}

	public boolean isReady() {
		if (connection != null)
			return connection.getState() == State.REGISTERED;
		else return false;
	}

	public void connect() throws IOException, UnknownHostException {
		autoReconnect.go(server, port);
	}

	public void joinChannel(String channel, String key) {
		connection.sendCommand(new JoinCommand(channel, key));
	}

	@SuppressWarnings("rawtypes")
	public String getMembers(String channel) {
		// Print out a list of people in our channel.
		StringBuilder builder = new StringBuilder();
		Enumeration members = connection.getClientState().getChannel(channel).getMembers();
		while (members.hasMoreElements()) {
			Member member = (Member) members.nextElement();
			builder.append("Member: " + member.getNick() + "\n");
		}
		return builder.toString();
	}

	public void say(String channel, String msg) {
		MessageCommand mc = new MessageCommand(connection.getClientState().getChannel(channel).getName(), msg);
		connection.sendCommand(mc);
	}

	public void sendCommand(String channel, OutCommand command) {
		connection.sendCommand(command);
	}

	public void setRegisteredListener(RegisteredListener listener) {
		this.listener = listener;
	}

	public void onRegistered() {
		if (listener != null) listener.onRegister();
	}

	public String getNickName() {
		return clientState.getNick().getNick();
	}

	public void incomingMessage(MessageCommand msg) {
		SChatWrapper.ircMessageRecieved(msg.getDest(), msg.getSource().getNick() + ": " + msg.getMessage());
	}

	public void disconnect() {
		connection.disconnect();
	}

	public IRCConnection getConnection() {
		return connection;
	}

	public static IRCProvider ircConnect(RegisteredListener listener) {
		String pass = XConfig.mp_irc_password.isEmpty() ? null : XConfig.mp_irc_password;
		IRCProvider provider = new IRCProvider(XConfig.mp_irc_server,
				XConfig.mp_irc_port, XConfig.mp_irc_username, pass);
		provider.setRegisteredListener(listener);
		try {
			provider.connect();
			return provider;
		} catch (Exception ex) {
			ALS.alError("IRCProvider: Connection error", ex);
			return null;
		}
	}
}
