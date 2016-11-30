package org.svearike.jeller.api;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.svearike.jeller.logic.DeviceLogic;
import org.svearike.jeller.object.Device;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

@SuppressWarnings("serial")
public class GetDeviceServlet extends APIServlet
{
	@Override
	protected void handle(HttpServletRequest req, HttpServletResponse resp) throws Exception
	{
		Device device = DeviceLogic.getInstance().getDevice(new Device(Integer.parseInt(req.getPathInfo().split("/")[1])));
		DeviceLogic.getInstance().updateStatus(Arrays.asList(device));

		JsonObject root = new JsonObject();
		root.add("device", new Gson().toJsonTree(device));
		root.addProperty("status", "ok");
		sendResponse(req, resp, root);
	}
}
