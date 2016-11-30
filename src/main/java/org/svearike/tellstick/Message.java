package org.svearike.tellstick;

public class Message
{
	private String message = "";

	public Message()
	{
	}

	public static Message createFromCommand(String command)
	{
		Message message = new Message();
		message.addArgument(command);
		return message;
	}

	public static Message createFromData(String data)
	{
		Message message = new Message();
		message.message = data;
		return message;
	}

	public Message addArgument(String functionName)
	{
		message += functionName.length() + ":" + functionName;
		return this;
	}

	public Message addArgument(int value)
	{
		message += "i" + value + "s";
		return this;
	}

	public static boolean nextIsInt(String message)
	{
		if (message.isEmpty())
			return false;

		if (message.charAt(0) == 'i')
			return true;

		return false;
	}

	public static boolean nextIsString(String message)
	{
		if (message.isEmpty())
			return false;

		return Character.isDigit(message.charAt(0));
	}

	public String takeString() throws IllegalStateException
	{
		if (message.isEmpty())
			return "";

		if (!nextIsString(message))
			throw new IllegalStateException("Next is not a string, was: " + message);

		int index = message.indexOf(":");
		int length = Integer.parseInt(message.substring(0, index));
		String str = message.substring(index + 1, index + 1 + length);
		message = message.substring(index + 1 + length);
		return str;
	}

	@Override
	public String toString()
	{
		return message;
	}

	public int takeInt() throws IllegalStateException
	{
		if (!nextIsInt(message))
			throw new IllegalStateException("Next is not int");

		int index = message.indexOf("s");
		int value = Integer.parseInt(message.substring(1, index));
		message = message.substring(index + 1);
		return value;
	}

	public void clear()
	{
		this.message = "";
	}
}
