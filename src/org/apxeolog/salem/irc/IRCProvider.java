package org.apxeolog.salem.irc;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import org.apxeolog.salem.ALS;
import org.apxeolog.salem.SChatWrapper;
import org.apxeolog.salem.config.XConfig;

import f00f.net.irc.martyr.IRCConnection;
import f00f.net.irc.martyr.State;
import f00f.net.irc.martyr.clientstate.Channel;
import f00f.net.irc.martyr.clientstate.ClientState;
import f00f.net.irc.martyr.clientstate.Member;
import f00f.net.irc.martyr.commands.ActionCtcp;
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
	private Channel mainChannel;
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
	public void printMembers() {
		// Print out a list of people in our channel.
		Enumeration members = mainChannel.getMembers();
		while (members.hasMoreElements()) {
			Member member = (Member) members.nextElement();
			System.out.println("Member: " + member.getNick() + " Ops: "
					+ member.hasOps() + " Voice: " + member.hasVoice());
		}
	}

	public void say(String channel, String msg) {
		connection.sendCommand(new MessageCommand(connection.getClientState().getChannel(channel).getName(), msg));
	}

	public void sayAction(String msg) {
		connection.sendCommand(new ActionCtcp(mainChannel.getName(), msg));
	}

	public void setRegisteredListener(RegisteredListener listener) {
		this.listener = listener;
	}

	public void onRegistered() {
		ALS.alDebugPrint(connection);
		if (listener != null) listener.onRegister();
	}

	public void incomingMessage(MessageCommand msg) {

		String sayCommand = "say ";
		String meCommand = "me ";
		String kickCommand = "kick ";
		String quitCommand = "quit ";
		String opCommand = "op";
		String setSecret = "setsecret";
		String showSecret = "showsecret";

		String message = msg.getMessage();
		SChatWrapper.IRCMessage(msg.getDest(), msg.getMessage());
		ALS.alDebugPrint("als", message, msg);
		ALS.alDebugPrint(msg.getDest(), msg.getIrcIdentifier(), msg.getSourceString());
		/*StringTokenizer tokenizer = new StringTokenizer(message);
		// First, remove the command.
		String commandStr = tokenizer.nextToken().toLowerCase();

		if (message.toLowerCase().startsWith("say")) {
			// We have a 'say' command
			String sayString = getParameter(message, sayCommand);
			SChatWrapper.IRCMessage(msg.getDest(), msg.getSource().getNick() + ": " + sayString);
		} else if (message.toLowerCase().startsWith(meCommand)) {
			// we have a 'me' command
			String sayString = getParameter(message, meCommand);
			sayAction(sayString);
		} else {
			// Umm..
			connection.sendCommand(new MessageCommand(msg.getSource(),
					"Bad syntax: " + message));
		}*/
	}

	private String getParameter(String raw, String command) {
		return raw.substring(command.length(), raw.length());
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
