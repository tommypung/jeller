package org.svearike.jeller.api;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.svearike.jeller.logic.DeviceLogic;
import org.svearike.jeller.object.DeviceScheduleEntry;

import com.google.gson.Gson;

@SuppressWarnings("serial")
public class AddNewDeviceScheduleEntryServlet extends APIServlet
{
	@Override
	protected void handle(HttpServletRequest req, HttpServletResponse resp) throws Exception
	{
		DeviceScheduleEntry entry = new Gson().fromJson(getJSON(req, resp), DeviceScheduleEntry.class);
		DeviceLogic.getInstance().addEntry(entry);

		sendObjectAsResponse(req, resp, entry);
	}
}
