package org.svearike.jeller.api;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.svearike.jeller.logic.DeviceLogic;
import org.svearike.jeller.object.Device;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

@SuppressWarnings("serial")
public class LearnDeviceServlet extends APIServlet
{
	@Override
	protected void handle(HttpServletRequest req, HttpServletResponse resp) throws Exception
	{
		JsonObject exception = null;

		Device device = new Gson().fromJson(getJSON(req, resp), Device.class);
		try {
			DeviceLogic.getInstance().learnDevice(device);
		} catch(Exception e) {
			e.printStackTrace();
			exception = getExceptionAsJson(e);
		}

		JsonElement elem = new Gson().toJsonTree(device);
		JsonObject root = elem.getAsJsonObject();
		if (exception == null)
			root.addProperty("status", "ok");
		else
		{
			root.addProperty("status", "error");
			root.add("exception", exception);
		}

		sendResponse(req, resp, root);
	}
}
