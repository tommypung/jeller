package org.svearike.jeller.api;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.svearike.jeller.logic.DeviceLogic;
import org.svearike.jeller.object.Device;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

@SuppressWarnings("serial")
public class GetDevicesServlet extends APIServlet
{
	@Override
	protected void handle(HttpServletRequest req, HttpServletResponse resp) throws Exception
	{
		List<Device> devices = DeviceLogic.getInstance().getDevices();
		DeviceLogic.getInstance().updateStatus(devices);

		JsonObject root = new JsonObject();
		root.add("devices", new Gson().toJsonTree(devices));
		root.addProperty("status", "ok");
		sendResponse(req, resp, root);
	}
}
