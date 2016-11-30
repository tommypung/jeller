package org.svearike.jeller.object;

import java.io.Serializable;
import java.util.Date;

@SuppressWarnings("serial")
public class DeviceScheduleEntry implements Serializable
{
	private long id;
	private String description;
	private Device device;
	private int startHour;
	private int startMinute;
	private int enforceDelay;
	private int resendInterval;
	private int command;
	private int value;
	private Date lastSent;
	private transient boolean hasBeenTriggered;

	/**
	 * @return the description
	 */
	public String getDescription()
	{
		return description;
	}
	/**
	 * @param description the description to set
	 */
	public void setDescription(String description)
	{
		this.description = description;
	}
	/**
	 * @return the device
	 */
	public Device getDevice()
	{
		return device;
	}
	/**
	 * @param device the device to set
	 */
	public void setDevice(Device device)
	{
		this.device = device;
	}
	/**
	 * @return the startHour
	 */
	public int getStartHour()
	{
		return startHour;
	}
	/**
	 * @param startHour the startHour to set
	 */
	public void setStartHour(int startHour)
	{
		this.startHour = startHour;
	}
	/**
	 * @return the startMinute
	 */
	public int getStartMinute()
	{
		return startMinute;
	}
	/**
	 * @param startMinute the startMinute to set
	 */
	public void setStartMinute(int startMinute)
	{
		this.startMinute = startMinute;
	}
	/**
	 * @return the enforceDelay
	 */
	public int getEnforceDelay()
	{
		return enforceDelay;
	}
	/**
	 * @param enforceDelay the enforceDelay to set
	 */
	public void setEnforceDelay(int enforceDelay)
	{
		this.enforceDelay = enforceDelay;
	}
	/**
	 * @return the resendInterval
	 */
	public int getResendInterval()
	{
		return resendInterval;
	}
	/**
	 * @param resendInterval the resendInterval to set
	 */
	public void setResendInterval(int resendInterval)
	{
		this.resendInterval = resendInterval;
	}
	/**
	 * @return the value
	 */
	public int getValue()
	{
		return value;
	}
	/**
	 * @param value the value to set
	 */
	public void setValue(int value)
	{
		this.value = value;
	}
	/**
	 * @return the lastSent
	 */
	public Date getLastSent()
	{
		return lastSent;
	}
	/**
	 * @param lastSent the lastSent to set
	 */
	public void setLastSent(Date lastSent)
	{
		this.lastSent = lastSent;
	}
	public long getId()
	{
		return id;
	}

	public void setId(long id)
	{
		this.id = id;
	}
	/**
	 * @return the hasBeenTriggered
	 */
	public boolean hasBeenTriggered()
	{
		return hasBeenTriggered;
	}
	/**
	 * @param hasBeenTriggered the hasBeenTriggered to set
	 */
	public void setHasBeenTriggered(boolean hasBeenTriggered)
	{
		this.hasBeenTriggered = hasBeenTriggered;
	}
	/**
	 * @return the command
	 */
	public int getCommand()
	{
		return command;
	}
	/**
	 * @param command the command to set
	 */
	public void setCommand(int command)
	{
		this.command = command;
	}
}
