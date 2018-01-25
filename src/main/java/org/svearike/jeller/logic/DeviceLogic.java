package org.svearike.jeller.logic;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.svearike.jeller.object.Device;
import org.svearike.jeller.object.DeviceScheduleEntry;
import org.svearike.tellstick.Client;

public class DeviceLogic
{
	private static DeviceLogic sInstance = create();
	private DatabaseLogic db = DatabaseLogic.getInstance();
	private Client tellerClient = Client.getInstance();
	private List<Device> lastUsedDevices = new LinkedList<>();

	private DeviceLogic()
	{
	}

	private static DeviceLogic create()
	{
		return new DeviceLogic();
	}

	public static DeviceLogic getInstance()
	{
		return sInstance;
	}

	public void addDevice(Device device) throws SQLException, IOException
	{
		if (!device.isIdSet())
			device.setId(tellerClient.tdAddDevice());
		db.addDevice(device);

		tellerClient.tdSetModel(device.getId(), device.getModel());
		tellerClient.tdSetProtocol(device.getId(), device.getProtocol());
		tellerClient.tdSetName(device.getId(), device.getName());
		tellerClient.tdSetDeviceParameter(device.getId(), "house", device.getHouse());
		tellerClient.tdSetDeviceParameter(device.getId(), "unit", device.getUnit());
		tellerClient.tdSetDeviceParameter(device.getId(), "hwretries", "" + device.getHardwareRetries());
		tellerClient.tdSetDeviceParameter(device.getId(), "softretries", "" + device.getSoftwareRetries());
	}

	public void updateDevice(Device device) throws SQLException, IllegalStateException, IOException
	{
		if (!device.isIdSet())
			throw new InvalidParameterException("Id not set");

		db.updateDevice(device);
		tellerClient.tdSetModel(device.getId(), device.getModel());
		tellerClient.tdSetProtocol(device.getId(), device.getProtocol());
		tellerClient.tdSetName(device.getId(), device.getName());
		tellerClient.tdSetDeviceParameter(device.getId(), "house", device.getHouse());
		tellerClient.tdSetDeviceParameter(device.getId(), "unit", device.getUnit());
		tellerClient.tdSetDeviceParameter(device.getId(), "hwretries", "" + device.getHardwareRetries());
		tellerClient.tdSetDeviceParameter(device.getId(), "softretries", "" + device.getSoftwareRetries());
	}

	public List<DeviceScheduleEntry> getScheduleEntries(Device device) throws SQLException
	{
		return db.getScheduleEntries(device);
	}

	public void addEntry(DeviceScheduleEntry entry) throws SQLException
	{
		db.addEntry(entry);
		SchedulerLogic.getInstance().reloadSchedules();
	}

	public List<DeviceScheduleEntry> getScheduleEntries() throws SQLException
	{
		return db.getScheduleEntries();
	}

	public List<Device> getDevices() throws SQLException
	{
		return db.getDevices();
	}

	public void updateStatus(List<Device> devices) throws IllegalStateException, IOException
	{
		if (devices == null)
			return;

		for(Device device : devices)
		{
			switch(tellerClient.tdLastSentCommand(device.getId(), Client.TELLSTICK_TURNOFF | Client.TELLSTICK_TURNON))
			{
			default:
			case Client.TELLSTICK_TURNOFF:
				device.setPowered(false);
				break;
			case Client.TELLSTICK_TURNON:
				device.setPowered(true);
				break;
			}
		}
	}

	public Device getDevice(Device device) throws SQLException
	{
		db.loadDevice(device);
		return device;
	}

	public List<Device> getLastUsedDevices()
	{
		return lastUsedDevices;
	}

	public void addDeviceUsage(Device device)
	{
		lastUsedDevices.add(0, device);
		if (lastUsedDevices.size() > 30)
			lastUsedDevices.remove(30);
	}

	public void learnDevice(Device device) throws IllegalStateException, IOException
	{
		tellerClient.tdLearn(device.getId());
	}
}
