package org.svearike.jeller.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

@SuppressWarnings("serial")
public abstract class APIServlet extends HttpServlet
{
	protected String getJSON(HttpServletRequest req, HttpServletResponse resp) throws JSONException, IOException
	{
		return IOUtils.toString(req.getInputStream());
	}

	protected abstract void handle(HttpServletRequest req, HttpServletResponse resp) throws Exception;

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		try {
			if (req.getMethod().equals("POST")) {
				handle(req, resp);
				return;
			}
			else if (req.getMethod().equals("GET")) {
				handle(req, resp);
				return;
			}
		} catch(Exception e) {
			JsonObject obj = new JsonObject();
			obj.addProperty("status", "error");
			obj.add("exception", getExceptionAsJson(e));
			sendResponse(req, resp, obj);
			return;
		}

		super.service(req, resp);
	}

	protected JsonObject getExceptionAsJson(Exception e)
	{
		JsonObject o = new JsonObject();
		StringWriter strWr = new StringWriter();
		PrintWriter s = new PrintWriter(strWr);
		e.printStackTrace(s);
		o.addProperty("class", e.getClass().getName());
		o.addProperty("message", e.getMessage());
		o.addProperty("stacktrace", strWr.toString());
		return o;
	}

	protected void sendResponse(HttpServletRequest req, HttpServletResponse resp, JSONObject json) throws IOException
	{
		sendResponse(req, resp, json.toString());
	}

	protected void sendResponse(HttpServletRequest req, HttpServletResponse resp, JsonObject json) throws IOException
	{
		sendResponse(req, resp, json.toString());
	}

	protected void sendObjectAsResponse(HttpServletRequest req, HttpServletResponse resp, Object object) throws IOException
	{
		JsonElement elem = new Gson().toJsonTree(object);
		JsonObject root = elem.getAsJsonObject();
		root.addProperty("status", "ok");
		sendResponse(req, resp, root);
	}

	protected void sendResponse(HttpServletRequest req, HttpServletResponse resp, String json) throws IOException
	{
		resp.setContentType("application/json; charset=UTF-8");
		resp.getWriter().write(json);
	}
}
