package com.github.uberroot.ncjbot;

/**
 * The root class for all NCJBot-specific exceptions.
 * 
 * @author Carter Waxman
 *
 */
public class NCJBotException extends Exception {

	public NCJBotException(String message) {
		super(message);
	}

	public NCJBotException(Throwable cause) {
		super(cause);
	}

	public NCJBotException(String message, Throwable cause) {
		super(message, cause);
	}

	public NCJBotException(String message, Throwable cause, boolean enableSupression, boolean writableStackTrace) {
		super(message, cause, enableSupression, writableStackTrace);
	}

}
