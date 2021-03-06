/*
 * ChannelWrongKeyError.java
 *
 *   Copyright (C) 2000, 2001, 2002, 2003 Ben Damm
 *
 *   This library is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU Lesser General Public
 *   License as published by the Free Software Foundation; either
 *   version 2.1 of the License, or (at your option) any later version.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *   Lesser General Public License for more details.
 *
 *   See: http://www.fsf.org/copyleft/lesser.txt
 */
package f00f.net.irc.martyr.errors;

import f00f.net.irc.martyr.InCommand;

/**
 * Code: 475 ERR_BADCHANNELKEY
 * &lt;channel&gt; :Cannot join channel (+k)
 * @author <a href="mailto:martyr@mog.se">Morgan Christiansson</a>
 * @version $Id: ChannelWrongKeyError.java 85 2007-08-02 18:26:59Z jadestorm $
 * TODO: Should we rename to BadChannelKeyError to match others?
 */
public class ChannelWrongKeyError extends GenericJoinError
{
	public ChannelWrongKeyError() 
	{
		super();
	}

	protected ChannelWrongKeyError(String chan, String comment) 
	{
		super(chan, comment);
	}

	public String getIrcIdentifier() 
	{
		return "475";
	}

	protected InCommand create(String channel, String comment) {
		return new ChannelWrongKeyError(channel, comment);
	}

}
