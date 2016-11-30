package org.svearike.tellstick;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class Client implements Runnable
{
	public static final int TELLSTICK_TURNON = 1;
	public static final int TELLSTICK_TURNOFF = 2;
	public static final int TELLSTICK_BELL = 4;
	public static final int TELLSTICK_TOGGLE = 8;
	public static final int TELLSTICK_DIM = 16;
	public static final int TELLSTICK_LEARN = 32;
	public static final int TELLSTICK_EXECUTE = 64;
	public static final int TELLSTICK_UP = 128;
	public static final int TELLSTICK_DOWN = 256;
	public static final int TELLSTICK_STOP = 512;

	private static Client sInstance;
	private static final int TELLSTICK_SUCCESS = 0;
	private Collection<RawDeviceEventCallback> rawDeviceCallbacks = new LinkedList<>();
	Socket socket;
	public String temperature = "Not set";

	public interface RawDeviceEventCallback
	{
		void rawDeviceEventCallback(Map<String, String> parameters);
	}

	static {
		try {
			sInstance = new Client();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(11);
		}
	}

	private Client() throws IOException
	{
		socket = new Socket(new File("/tmp/TelldusEvents"), true);
		new Thread(this).start();
	}

	public void registerCallback(RawDeviceEventCallback callback)
	{
		this.rawDeviceCallbacks.add(callback);
	}

	public void parseIncomingEvents()
	{
		Message incoming = Message.createFromData(socket.read());
		boolean cont = true;

		while(cont)
		{
			try {
				String takenString = incoming.takeString();
				if (takenString == null || takenString.isEmpty())
					cont = false;
				else
				{
					System.out.println("takenString = " + takenString);
					switch(takenString)
					{
					case "TDRawDeviceEvent":
						handleRawDeviceEvent(incoming);
						break;
					case "TDSensorEvent":
						handleSensorEvent(incoming);
						break;
					default:
						System.out.println("Unhandled event: " + takenString);
						incoming.clear();
					}
				}
			} catch(IllegalStateException e) {
				e.printStackTrace();
			}
		}
	}

	//	16:TDRawDeviceEvent93:class:command;protocol:arctech;model:selflearning;house:5574039;unit:9;group:0;method:turnon;i1s

	private void handleSensorEvent(Message incoming) throws IllegalStateException
	{
		Map<String, String> parameters = new HashMap<>();
		parameters.put("protocol", incoming.takeString());
		parameters.put("model", incoming.takeString());
		parameters.put("id", "" + incoming.takeInt());
		parameters.put("dataType", "" + incoming.takeInt());
		parameters.put("value", "" + incoming.takeString());
		parameters.put("timestamp", "" + incoming.takeInt());
		System.out.println("SensorEvent, parameters = " + parameters);
	}

	private void handleRawDeviceEvent(Message incoming) throws IllegalStateException
	{
		String[] parameterArray = incoming.takeString().split(";");
		Map<String, String> parameters = new HashMap<>();
		for(int i=0;i<parameterArray.length;i++)
		{
			String[] paramValue = parameterArray[i].split(":");
			parameters.put(paramValue[0], paramValue[1]);
		}

		System.out.println("parameters = " + parameters);
		switch(parameters.get("model"))
		{
		case "temperature":
			this.temperature = parameters.get("temp");
			break;
		case "selflearning":
			if ("8626".equals(parameters.get("house"))
					&& "4".equals(parameters.get("unit"))
					&& "turnoff".equals(parameters.get("method")))
			{
				System.out.println("Doorbell");
				try {
					Runtime.getRuntime().exec("aplay /home/tommy/doorbell.wav");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			break;
		}

		for(RawDeviceEventCallback cb : rawDeviceCallbacks)
			cb.rawDeviceEventCallback(parameters);

		System.out.println("Controller id = " + incoming.takeInt());
	}

	public int tdTurnOn(int deviceId) throws IllegalStateException, IOException
	{
		return getIntegerFromService(Message.createFromCommand("tdTurnOn").addArgument(deviceId));
	}

	public int tdTurnOff(int deviceId) throws IllegalStateException, IOException
	{
		return getIntegerFromService(Message.createFromCommand("tdTurnOff").addArgument(deviceId));
	}

	public int tdBell(int deviceId) throws IllegalStateException, IOException
	{
		return getIntegerFromService(Message.createFromCommand("tdBell").addArgument(deviceId));
	}

	public int tdDim(int deviceId, int level) throws IllegalStateException, IOException
	{
		return getIntegerFromService(Message.createFromCommand("tdDim").addArgument(deviceId).addArgument(level));
	}

	public int tdExecute(int deviceId) throws IllegalStateException, IOException
	{
		return getIntegerFromService(Message.createFromCommand("tdExecute").addArgument(deviceId));
	}

	public int tdUp(int deviceId) throws IllegalStateException, IOException
	{
		return getIntegerFromService(Message.createFromCommand("tdUp").addArgument(deviceId));
	}

	public int tdDown(int deviceId) throws IllegalStateException, IOException
	{
		return getIntegerFromService(Message.createFromCommand("tdDown").addArgument(deviceId));
	}

	public int tdStop(int deviceId) throws IllegalStateException, IOException
	{
		return getIntegerFromService(Message.createFromCommand("tdStop").addArgument(deviceId));
	}

	public int tdLearn(int deviceId) throws IllegalStateException, IOException
	{
		return getIntegerFromService(Message.createFromCommand("tdLearn").addArgument(deviceId));
	}

	public int tdLastSentCommand(int deviceId, int methodsSupported) throws IllegalStateException, IOException
	{
		return getIntegerFromService(Message.createFromCommand("tdLastSentCommand").addArgument(deviceId).addArgument(methodsSupported));
	}

	public String tdLastSentValue(int deviceId) throws IllegalStateException, IOException
	{
		return getStringFromService(Message.createFromCommand("tdLastSentValue").addArgument(deviceId));
	}

	public int tdGetNumberOfDevices() throws IllegalStateException, IOException
	{
		return getIntegerFromService(Message.createFromCommand("tdGetNumberOfDevices"));
	}

	public int tdGetDeviceId(int deviceIndex) throws IllegalStateException, IOException
	{
		return getIntegerFromService(Message.createFromCommand("tdGetDeviceId").addArgument(deviceIndex));
	}

	public int tdGetDeviceType(int deviceId) throws IllegalStateException, IOException
	{
		return getIntegerFromService(Message.createFromCommand("tdGetDeviceType").addArgument(deviceId));
	}

	public String tdGetName(int deviceId) throws IllegalStateException, IOException
	{
		return getStringFromService(Message.createFromCommand("tdGetName").addArgument(deviceId));
	}

	public boolean tdSetName(int deviceId, String newName) throws IllegalStateException, IOException
	{
		return getBooleanFromService(Message.createFromCommand("tdSetName").addArgument(deviceId).addArgument(newName));
	}

	public String tdGetProtocol(int deviceId) throws IllegalStateException, IOException
	{
		return getStringFromService(Message.createFromCommand("tdGetProtocol").addArgument(deviceId));
	}

	public boolean tdSetProtocol(int deviceId, String newProtocol) throws IllegalStateException, IOException
	{
		return getBooleanFromService(Message.createFromCommand("tdSetProtocol").addArgument(deviceId).addArgument(newProtocol));
	}

	public String tdGetModel(int deviceId) throws IllegalStateException, IOException
	{
		return getStringFromService(Message.createFromCommand("tdGetModel").addArgument(deviceId));
	}

	public boolean tdSetModel(int deviceId, String newModel) throws IllegalStateException, IOException
	{
		return getBooleanFromService(Message.createFromCommand("tdSetModel").addArgument(deviceId).addArgument(newModel));
	}

	public boolean tdSetDeviceParameter(int deviceId, String name, String value) throws IllegalStateException, IOException
	{
		return getBooleanFromService(Message.createFromCommand("tdSetDeviceParameter").addArgument(deviceId).addArgument(name).addArgument(value));
	}

	public String tdGetDeviceParameter(int deviceId, String name, String defaultValue) throws IllegalStateException, IOException
	{
		return getStringFromService(Message.createFromCommand("tdGetDeviceParameter").addArgument(deviceId).addArgument(name).addArgument(defaultValue));
	}

	public int tdAddDevice() throws IllegalStateException, IOException
	{
		return getIntegerFromService(Message.createFromCommand("tdAddDevice"));
	}

	public boolean tdRemoveDevice(int deviceId) throws IllegalStateException, IOException
	{
		return getBooleanFromService(Message.createFromCommand("tdRemoveDevice").addArgument(deviceId));
	}

	public int tdMethods(int deviceId, int methods) throws IllegalStateException, IOException
	{
		return getIntegerFromService(Message.createFromCommand("tdMethods").addArgument(deviceId).addArgument(methods));
	}

	public String tdGetErrorString(int errno)
	{
		final String[] responses = {
				"Success",
				"TellStick not found",
				"Permission denied",
				"Device not found",
				"The method you tried to use is not supported by the device",
				"An error occurred while communicating with TellStick",
				"Could not connect to the Telldus Service",
				"Received an unknown response",
				"Syntax error",
				"Broken pipe",
				"An error occurred while communicating with the Telldus Service",
				"Syntax error in the configuration file"
		};
		errno = Math.abs(errno);
		if (errno >= responses.length)
			return "Unknown error";
		return responses[errno];
	}

	public int tdSendRawCommand(String command, int reserved) throws IllegalStateException, IOException
	{
		return getIntegerFromService(Message.createFromCommand("tdSendRawCommand").addArgument(command).addArgument(reserved));
	}

	public String tdConnectTellstickController(int vid, int pid, String serial) throws IllegalStateException, IOException
	{
		return getStringFromService(Message.createFromCommand("tdConnectTellStickController").addArgument(vid).addArgument(pid).addArgument(serial));
	}

	public String tdDisconnectTellstickController(int vid, int pid, String serial) throws IllegalStateException, IOException
	{
		return getStringFromService(Message.createFromCommand("tdDisconnectTellStickController").addArgument(vid).addArgument(pid).addArgument(serial));
	}

	public boolean getBooleanFromService(Message msg) throws IOException, IllegalStateException
	{
		return sendToService(msg).takeInt() == TELLSTICK_SUCCESS;
	}
	
	public String getStringFromService(Message msg) throws IOException, IllegalStateException
	{
		return sendToService(msg).takeString();
	}

	public int getIntegerFromService(Message msg) throws IOException, IllegalStateException
	{
		return sendToService(msg).takeInt();
	}

	private synchronized Message sendToService(Message m) throws IOException
	{
		Socket s = new Socket(new File("/tmp/TelldusClient"), false);
		s.write(m.toString());
		Message response = Message.createFromData(s.read());
		System.out.println("response = " + response);
		s.close();
		return response;
	}

	public Socket getSocket()
	{
		return socket;
	}

	@Override
	public void run()
	{
		while(true)
		{
			parseIncomingEvents();
			try {
				Thread.sleep(1000);
			} catch(Exception e) {
			}
		}
	}

	public static Client getInstance()
	{
		return sInstance;
	}
}
