package org.svearike.jeller.logic;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.svearike.jeller.object.Device;
import org.svearike.jeller.object.DeviceScheduleEntry;

public class DatabaseLogic
{
	private static DatabaseLogic sInstance = new DatabaseLogic();
	private List<Connection> pool = new LinkedList<>();

	private DatabaseLogic()
	{
		try {
			Class.forName("org.sqlite.JDBC");
			try {
				populate();
			} catch(Exception e) {
				e.printStackTrace();
				System.err.println("Could not populate db - already exists?");
			}
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(10);
		}
	}

	private Connection openNewConnection() throws SQLException
	{
		System.out.println("Opening db connection");
		return DriverManager.getConnection("jdbc:sqlite:jeller.db");
	}

	private Connection getConnection() throws SQLException
	{
		synchronized(pool)
		{
			if (!pool.isEmpty())
				return pool.remove(0);
		}

		return openNewConnection();
	}

	private void returnConnection(Connection connection)
	{
		synchronized(pool) {
			pool.add(connection);
			System.out.println("Returning connection, pool size = " + pool.size());
		}
	}

	private void populate() throws SQLException, IOException
	{
		System.out.println("Running initial populate.sql");
		Connection c = getConnection();
		try {
			Statement stmt = getConnection().createStatement();
			stmt.executeUpdate(getSQL("populate"));
			stmt.close();
		} finally {
			returnConnection(c);
		}
	}

	private String getSQL(String name) throws IOException
	{
		return IOUtils.toString(DatabaseLogic.class.getResourceAsStream("/sql/" + name + ".sql"));
	}

	public static DatabaseLogic getInstance()
	{
		return sInstance;
	}

	public void updateDevice(Device device) throws SQLException
	{
		Connection c = getConnection();
		PreparedStatement stmt = null;
		try {
			stmt = c.prepareStatement("UPDATE device SET name=?,room=?,floor=?,model=?,protocol=?,house=?,unit=?,icon=?,hwretries=?,swretries=? WHERE id=?");
			stmt.setString(1, device.getName());
			stmt.setString(2, device.getRoom());
			stmt.setString(3, device.getFloor());
			stmt.setString(4, device.getModel());
			stmt.setString(5, device.getProtocol());
			stmt.setString(6, device.getHouse());
			stmt.setString(7, device.getUnit());
			stmt.setString(8, device.getIcon());
			stmt.setInt(9, device.getHardwareRetries());
			stmt.setInt(10, device.getSoftwareRetries());
			stmt.setInt(11, device.getId());
			stmt.executeUpdate();
		} finally {
			if (stmt != null)
				stmt.close();
			returnConnection(c);
		}
	}
	
	public void addDevice(Device device) throws SQLException
	{
		Connection c = getConnection();
		PreparedStatement stmt = null;
		try {
			stmt = c.prepareStatement("INSERT INTO device(id,name,room,floor,model,protocol,house,unit,icon,hwretries,swretries) VALUES(?,?,?,?,?,?,?,?,?,?,?)");
			stmt.setInt(1, device.getId());
			stmt.setString(2, device.getName());
			stmt.setString(3, device.getRoom());
			stmt.setString(4, device.getFloor());
			stmt.setString(5, device.getModel());
			stmt.setString(6, device.getProtocol());
			stmt.setString(7, device.getHouse());
			stmt.setString(8, device.getUnit());
			stmt.setString(9, device.getIcon());
			stmt.setInt(10, device.getHardwareRetries());
			stmt.setInt(11, device.getSoftwareRetries());
			stmt.executeUpdate();
		} finally {
			if (stmt != null)
				stmt.close();
			returnConnection(c);
		}
	}

	public void addEntry(DeviceScheduleEntry entry) throws SQLException
	{
		Connection c = getConnection();
		PreparedStatement stmt = null;
		try {
			stmt = c.prepareStatement("INSERT INTO device_schedule_entry(device,description,enforceDelay,resendInterval,startHour,startMinute,tdValue,command) VALUES(?,?,?,?,?,?,?,?)");
			stmt.setInt(1, entry.getDevice().getId());
			stmt.setString(2, entry.getDescription());
			stmt.setInt(3, entry.getEnforceDelay());
			stmt.setInt(4, entry.getResendInterval());
			stmt.setInt(5, entry.getStartHour());
			stmt.setInt(6, entry.getStartMinute());
			stmt.setInt(7, entry.getValue());
			stmt.setInt(8, entry.getCommand());
			stmt.executeUpdate();
		} finally {
			stmt.close();
			returnConnection(c);
		}
	}

	public List<DeviceScheduleEntry> getScheduleEntries(Device device) throws SQLException
	{
		Connection c = getConnection();
		try {
			PreparedStatement stmt = null;
			stmt = c.prepareStatement("SELECT id,description,device,startHour,startMinute,enforceDelay,resendInterval,tdValue,lastSent,command FROM device_schedule_entry WHERE device=?");
			stmt.setInt(1, device.getId());
			ResultSet set = stmt.executeQuery();
			try {
				List<DeviceScheduleEntry> entries = new LinkedList<>();
				while (set.next())
				{
					DeviceScheduleEntry entry = new DeviceScheduleEntry();
					entry.setDevice(device);
					write(set, entry);
					entries.add(entry);
				}
				return entries;
			} finally {
				set.close();
				stmt.close();
			}
		} finally {
			returnConnection(c);
		}
	}

	public List<DeviceScheduleEntry> getScheduleEntries() throws SQLException
	{
		Connection c = getConnection();
		try {
			PreparedStatement stmt = c.prepareStatement("SELECT id,description,device,startHour,startMinute,enforceDelay,resendInterval,tdValue,lastSent,command FROM device_schedule_entry");
			ResultSet set = stmt.executeQuery();
			try {
				List<DeviceScheduleEntry> entries = new LinkedList<>();
				while (set.next())
				{
					DeviceScheduleEntry entry = new DeviceScheduleEntry();
					write(set, entry);
					entries.add(entry);
				}
				return entries;
			} finally {
				set.close();
				stmt.close();
			}
		} finally {
			returnConnection(c);
		}
	}

	private void write(ResultSet set, DeviceScheduleEntry entry) throws SQLException
	{
		if (entry.getDevice() == null)
			entry.setDevice(new Device(set.getInt("device")));

		entry.setId(set.getLong("id"));
		entry.setDescription(set.getString("description"));
		entry.setLastSent(new Date(set.getLong("lastSent")));
		entry.setEnforceDelay(set.getInt("enforceDelay"));
		entry.setResendInterval(set.getInt("resendInterval"));
		entry.setStartHour(set.getInt("startHour"));
		entry.setStartMinute(set.getInt("startMinute"));
		entry.setValue(set.getInt("tdValue"));
		entry.setCommand(set.getInt("command"));
	}

	public List<Device> getDevices() throws SQLException
	{
		Connection c = getConnection();
		try {
			PreparedStatement stmt = c.prepareStatement("SELECT id,name,room,floor,model,protocol,house,unit,icon,hwretries,swretries FROM device");
			ResultSet set = stmt.executeQuery();
			try {
				List<Device> devices = new LinkedList<>();
				while (set.next())
				{
					Device device = new Device();
					write(set, device);
					devices.add(device);
				}
				return devices;
			} finally {
				set.close();
				stmt.close();
			}
		} finally {
			returnConnection(c);
		}
	}

	private void write(ResultSet set, Device device) throws SQLException
	{
		device.setId(set.getInt("id"));
		device.setFloor(set.getString("floor"));
		device.setRoom(set.getString("room"));
		device.setName(set.getString("name"));
		device.setProtocol(set.getString("protocol"));
		device.setModel(set.getString("model"));
		device.setHouse(set.getString("house"));
		device.setUnit(set.getString("unit"));
		device.setIcon(set.getString("icon"));
		device.setHardwareRetries(set.getInt("hwretries"));
		device.setSoftwareRetries(set.getInt("swretries"));
	}

	public void loadDevice(Device device) throws SQLException
	{
		Connection c = getConnection();
		try {
			PreparedStatement stmt = c.prepareStatement("SELECT id,name,room,floor,model,protocol,house,unit,icon,hwretries,swretries FROM device WHERE id=?");
			stmt.setInt(1, device.getId());
			ResultSet set = stmt.executeQuery();
			try {
				if(set.next())
					write(set, device);
				else
					throw new InvalidParameterException("device.id(" + device.getId() + ") not found");
			} finally {
				set.close();
				stmt.close();
			}
		} finally {
			returnConnection(c);
		}
	}
}
