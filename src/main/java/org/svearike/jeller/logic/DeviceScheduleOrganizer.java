package org.svearike.jeller.logic;

import java.io.Serializable;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.svearike.jeller.object.Device;
import org.svearike.jeller.object.DeviceScheduleEntry;

@SuppressWarnings("serial")
public class DeviceScheduleOrganizer implements Serializable
{
	private Map<Device, List<DeviceScheduleEntry>> mEntries;
	private Map<Device, DeviceScheduleEntry> mActiveEntries = new HashMap<>();
	private long mUpdateTime = System.currentTimeMillis();

	public void changeEntries(Collection<DeviceScheduleEntry> entries)
	{
		synchronized (this) {
			this.mUpdateTime = System.currentTimeMillis();
			this.mEntries = new HashMap<>();
			for(DeviceScheduleEntry entry : entries)
			{
				List<DeviceScheduleEntry> storedEntries = mEntries.get(entry.getDevice());
				if (storedEntries == null)
				{
					storedEntries = new LinkedList<>();
					mEntries.put(entry.getDevice(), storedEntries);
				}

				entry.setHasBeenTriggered(true);
				storedEntries.add(entry);
			}
		}
	}

	public DeviceScheduleOrganizer(Collection<DeviceScheduleEntry> entries)
	{
		changeEntries(entries);
	}

	public DeviceScheduleEntry getActiveEntry(Device device)
	{
		Collection<DeviceScheduleEntry> entries = mEntries.get(device);
		if (entries == null)
			return null;

		GregorianCalendar cal = new GregorianCalendar();
		DeviceScheduleEntry closestEntry = null;
		int smallestDistance = 0;
		int systemMinutes = cal.get(GregorianCalendar.HOUR_OF_DAY) * 60 + cal.get(GregorianCalendar.MINUTE);
		for(DeviceScheduleEntry entry : entries)
		{
			int entryMinutes = entry.getStartHour() * 60 + entry.getStartMinute();
			int entryDistance;
			if (systemMinutes >= entryMinutes)
				entryDistance = systemMinutes - entryMinutes;
			else
				entryDistance = ((25 * 60 - 1) - entryMinutes) + systemMinutes;

			if (closestEntry == null || entryDistance <= smallestDistance) {
				closestEntry = entry;
				smallestDistance = entryDistance;
			}
		}

		if (closestEntry == null)
			return null;

		if (mActiveEntries.get(device) == null || (mActiveEntries.get(device).getId() != closestEntry.getId()))
		{
			closestEntry.setHasBeenTriggered(false);
			mActiveEntries.put(device, closestEntry);
		}

		return closestEntry;
	}

	public Map<Device, DeviceScheduleEntry> getActiveEntries()
	{
		Map<Device, DeviceScheduleEntry> entries = new HashMap<>();
		for(Device device : mEntries.keySet())
		{
			DeviceScheduleEntry e = getActiveEntry(device);
			if (e != null)
				entries.put(device, e);
		}
		return entries;
	}

	public long getUpdateTime()
	{
		return mUpdateTime ;
	}
}
