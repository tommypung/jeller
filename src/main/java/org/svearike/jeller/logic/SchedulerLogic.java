package org.svearike.jeller.logic;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.svearike.jeller.object.DeviceScheduleEntry;
import org.svearike.tellstick.Client;

public class SchedulerLogic implements Runnable
{
	public static Logger LOG = Logger.getLogger(SchedulerLogic.class.getName());
	public static final long RELOAD_FROM_DB_INTERVAL = 60 * 60 * 1000L;
	public static final long RELOAD_ACTIVE_ENTRIES_FROM_ORGANIZER_INTERVAL = 60 * 1000L;
	private static SchedulerLogic sInstance = null;
	private boolean mStarted = false;
	private volatile boolean mShouldStop = false;
	private DeviceScheduleOrganizer mSchedules;
	private long cLastDBreload = 0;
	private Client mTellerClient;

	public static void create(Client client)
	{
		sInstance = new SchedulerLogic(client);
	}

	private SchedulerLogic(Client client)
	{
		this.mTellerClient = client;
	}

	public static SchedulerLogic getInstance()
	{
		return sInstance;
	}

	public void start()
	{
		if (this.mStarted)
			return;

		this.mShouldStop = false;
		this.mStarted = true;
		new Thread(this).start();
	}

	public void reloadSchedules() throws SQLException
	{
		cLastDBreload = System.currentTimeMillis();
		LOG.info("Reloading schedules");
		List<DeviceScheduleEntry> schedules = DeviceLogic.getInstance().getScheduleEntries();
		LOG.info("Found " + schedules.size() + " schedule entries");
		synchronized(this) {
			if (this.mSchedules == null)
				this.mSchedules = new DeviceScheduleOrganizer(schedules);
			else
				this.mSchedules.changeEntries(schedules);
		}
	}

	public void stop()
	{
		this.mShouldStop = true;
	}

	@Override
	public void run()
	{
		try {
			Collection<DeviceScheduleEntry> activeEntries = null;
			long lastActiveEntriesReload = 0;

			while(!mShouldStop)
			{
				synchronized(this)
				{
					if (this.mSchedules == null || cLastDBreload + RELOAD_FROM_DB_INTERVAL < System.currentTimeMillis())
					{
						try {
							reloadSchedules();
						} catch(Exception e) {
							LOG.log(Level.SEVERE, "Could not reload schedules", e);
						}
					}

					if (this.mSchedules != null)
						if (activeEntries == null || this.mSchedules.getUpdateTime() > lastActiveEntriesReload || lastActiveEntriesReload + RELOAD_ACTIVE_ENTRIES_FROM_ORGANIZER_INTERVAL < System.currentTimeMillis())
						{
							activeEntries = this.mSchedules.getActiveEntries().values();
							lastActiveEntriesReload = System.currentTimeMillis();
						}
				}

				if (activeEntries != null)
				{
					//LOG.info("Found " + activeEntries.size() + " active entries");
					for(DeviceScheduleEntry entry : activeEntries)
					{
						//LOG.info("Active entry for device " + entry.getDevice().getId() + " - " + entry.getStartHour() + ":" + entry.getStartMinute() + " - " + entry.hasBeenTriggered());
						try {
							if (!entry.hasBeenTriggered())
								trigger(entry);
						} catch(Exception e) {
							LOG.log(Level.SEVERE, "Could not trigger entry: " + entry, e);
						}
					}
				}

				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} finally {
			this.mStarted = false;
		}
	}

	private void trigger(DeviceScheduleEntry entry) throws IllegalStateException, IOException
	{
		LOG.info("Triggering schedule entry" + entry);

		switch(entry.getCommand())
		{
		case 1:
			if (entry.getValue() == 0)
				mTellerClient.tdTurnOff(entry.getDevice().getId());
			else
				mTellerClient.tdTurnOn(entry.getDevice().getId());
			break;
		default:
			LOG.severe("Unknown command: " + entry.getCommand());
		}

		entry.setHasBeenTriggered(true);
	}
}
