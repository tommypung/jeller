package org.svearike.jeller.object;

import java.io.Serializable;

//parameters = {unit=1, model=selflearning, protocol=arctech, class=command, method=turnoff, house=33696350, group=1}
//16:TDRawDeviceEvent92:class:command;protocol:arctech;model:selflearning;house:567674;unit:1;group:0;method:turnon;i1s
//16:TDRawDeviceEvent78:class:command;protocol:waveman;model:codeswitch;house:A;unit:10;method:turnon;i1s16:TDRawDeviceEvent88:class:command;protocol:everflourish;model:sarning;house:2711;unit:3;method:turnoff;i1s

@SuppressWarnings("serial")
public class Device implements Serializable
{
	private int id = 0;
	private String name;
	private String room;
	private String floor;
	private String model;
	private String protocol;
	private String house;
	private String unit;
	private boolean powered;
	private String icon;
	private int hwretries = 0;
	private int swretries = 0;

	public Device(int id)
	{
		this.id = id;
	}

	public Device()
	{
	}

	/**
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * @return the room
	 */
	public String getRoom()
	{
		return room;
	}

	/**
	 * @param room the room to set
	 */
	public void setRoom(String room)
	{
		this.room = room;
	}

	/**
	 * @return the floor
	 */
	public String getFloor()
	{
		return floor;
	}

	/**
	 * @param floor the floor to set
	 */
	public void setFloor(String floor)
	{
		this.floor = floor;
	}

	/**
	 * @return the model
	 */
	public String getModel()
	{
		return model;
	}

	/**
	 * @param model the model to set
	 */
	public void setModel(String model)
	{
		this.model = model;
	}

	/**
	 * @return the protocol
	 */
	public String getProtocol()
	{
		return protocol;
	}

	/**
	 * @param protocol the protocol to set
	 */
	public void setProtocol(String protocol)
	{
		this.protocol = protocol;
	}

	/**
	 * @return the house
	 */
	public String getHouse()
	{
		return house;
	}

	/**
	 * @param house the house to set
	 */
	public void setHouse(String house)
	{
		this.house = house;
	}

	/**
	 * @return the unit
	 */
	public String getUnit()
	{
		return unit;
	}

	/**
	 * @param unit the unit to set
	 */
	public void setUnit(String unit)
	{
		this.unit = unit;
	}

	/**
	 * @return the id
	 */
	public int getId()
	{
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(int id)
	{
		this.id = id;
	}

	public boolean isIdSet()
	{
		return getId() != 0;
	}

	@Override
	public int hashCode()
	{
		return getId();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof Device)
			return ((Device) obj).getId() == getId();
		if (obj instanceof Number)
			return ((Number) obj).intValue() == getId();
		return false;
	}

	public boolean isPowered()
	{
		return powered;
	}

	public void setPowered(boolean powered)
	{
		this.powered = powered;
	}

	public String getIcon()
	{
		return icon;
	}

	public void setIcon(String icon)
	{
		this.icon = icon;
	}

	public int getHardwareRetries()
	{
		return hwretries;
	}

	public int getSoftwareRetries()
	{
		return swretries;
	}

	public void setHardwareRetries(int hwretries)
	{
		this.hwretries = hwretries;
	}

	public void setSoftwareRetries(int swretries)
	{
		this.swretries = swretries;
	}
}
