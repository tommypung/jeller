package org.svearike.jeller.api;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.svearike.jeller.logic.DeviceLogic;
import org.svearike.jeller.object.Device;
import org.svearike.tellstick.Client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

@SuppressWarnings("serial")
public class ToggleDeviceServlet extends APIServlet
{
	@Override
	protected void handle(HttpServletRequest req, HttpServletResponse resp) throws Exception
	{
		String[] path = req.getPathInfo().split("/");
		Device device = new Device(Integer.parseInt(path[1]));
		DeviceLogic.getInstance().updateStatus(Arrays.asList(device));
		if (device.isPowered())
			Client.getInstance().tdTurnOff(device.getId());
		else
			Client.getInstance().tdTurnOn(device.getId());
		boolean newPoweredStatus = !device.isPowered();
		device = DeviceLogic.getInstance().getDevice(device);
		device.setPowered(newPoweredStatus);

		JsonObject root = new JsonObject();
		root.add("device", new Gson().toJsonTree(device));
		root.addProperty("status", "ok");
		sendResponse(req, resp, root);
	}
}
