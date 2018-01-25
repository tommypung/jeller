package org.svearike.jeller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.ssl.SslConnector;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.svearike.jeller.acme.AcmeServlet;
import org.svearike.jeller.api.AddNewDeviceScheduleEntryServlet;
import org.svearike.jeller.api.AddNewDeviceServlet;
import org.svearike.jeller.api.GetDeviceServlet;
import org.svearike.jeller.api.GetDevicesServlet;
import org.svearike.jeller.api.GetLastUsedDevicesServlet;
import org.svearike.jeller.api.LearnDeviceServlet;
import org.svearike.jeller.api.ToggleDeviceServlet;
import org.svearike.jeller.api.UpdateDeviceServlet;
import org.svearike.jeller.logic.DatabaseLogic;
import org.svearike.jeller.logic.DeviceLogic;
import org.svearike.jeller.logic.SchedulerLogic;
import org.svearike.jeller.object.Device;
import org.svearike.jeller.object.DeviceScheduleEntry;
import org.svearike.tellstick.Client;
import org.svearike.tellstick.Client.RawDeviceEventCallback;

public class Main
{
	private static Client tellerClient;
	private static String KEYSTORE_PATH = "jeller.jks";
	private static String KEYSTORE_PASS = "fiskolja88";

	public static void main(String[] args) throws Exception
	{
		tellerClient = Client.getInstance();
		tellerClient.registerCallback(new RawDeviceEventCallback() {
			@Override public void rawDeviceEventCallback(Map<String, String> parameters) {
				Device d = new Device();
				String model = parameters.get("model");
				if (model.equals("selflearning"))
					model = "selflearning-switch";
				d.setModel(model);
				d.setProtocol(parameters.get("protocol"));
				d.setHouse(parameters.get("house"));
				d.setUnit(parameters.get("unit"));
				DeviceLogic.getInstance().addDeviceUsage(d);
			}
		});

		Server server = new Server();
		SslContextFactory sslFactory = new SslContextFactory();
		sslFactory.setCertAlias("jetty");
		sslFactory.setKeyStorePath(KEYSTORE_PATH);
		sslFactory.setKeyStorePassword(KEYSTORE_PASS);
		sslFactory.setTrustStore(KEYSTORE_PATH);
		sslFactory.setTrustStorePassword(KEYSTORE_PASS);

		SslConnector sslConnector = new SslSocketConnector(sslFactory);
		sslConnector.setPort(8888);
		server.addConnector(sslConnector);

		SocketConnector stdConnect = new SocketConnector();
		stdConnect.setPort(8889);
		server.addConnector(stdConnect);

		DatabaseLogic.getInstance();
		SchedulerLogic.create(tellerClient);
		SchedulerLogic.getInstance().start();

		ResourceHandler resourceHandler = new ResourceHandler();
		resourceHandler.setDirectoriesListed(false);
		resourceHandler.setWelcomeFiles(new String[] { "index.html" });
		resourceHandler.setResourceBase(Main.class.getResource("/webapp").toExternalForm());

		ServletHandler handler = new ServletHandler();
		handler.addServletWithMapping(HelloServlet.class, "/*");
		handler.addServletWithMapping(AddNewDeviceServlet.class, "/api/addNewDevice");
		handler.addServletWithMapping(UpdateDeviceServlet.class, "/api/updateDevice/*");
		handler.addServletWithMapping(ToggleDeviceServlet.class, "/api/toggleDevice/*");
		handler.addServletWithMapping(GetDevicesServlet.class, "/api/getDevices");
		handler.addServletWithMapping(LearnDeviceServlet.class, "/api/learnDevice");
		handler.addServletWithMapping(GetDeviceServlet.class, "/api/getDevice/*");
		handler.addServletWithMapping(AcmeServlet.class, "/.well-known/acme-challenge/*");
		handler.addServletWithMapping(GetLastUsedDevicesServlet.class, "/api/getLastUsedDevices/*");
		handler.addServletWithMapping(AddNewDeviceScheduleEntryServlet.class, "/api/addNewDeviceScheduleEntry");

		HandlerList handlers = new HandlerList();
		handlers.addHandler(resourceHandler);
		handlers.addHandler(handler);

		server.setHandler(handlers);
		server.start();

//		LetsEncrypt enc = new LetsEncrypt();
//		enc.fetchCertificate(KEYSTORE_PATH, KEYSTORE_PASS, Arrays.asList("jeller2.blunda.org"));
		server.join();
	}

	@SuppressWarnings("serial")
	public static class HelloServlet extends HttpServlet
	{
		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
		{
			String[] pathInfo = req.getPathInfo().split("/");
			System.out.println("pathinfo = " + req.getPathInfo());
			resp.setContentType("text/html");
			PrintWriter writer = resp.getWriter();
			writer.println("Hello world<br>The temperature right now is:\n" + tellerClient.temperature);
			int numDevices = tellerClient.tdGetNumberOfDevices();
			if (numDevices < 0)
				writer.println("<br/>error from num_devices = " + tellerClient.tdGetErrorString(numDevices) + "<br/>");

			writer.println("<br/>num devices: " + numDevices);
			for(int i=0;i<numDevices;i++)
			{
				int deviceId = tellerClient.tdGetDeviceId(i);
				writer.println("<br/>#" + i + " - " + deviceId);
				writer.println(", name = " + tellerClient.tdGetName(deviceId));
				writer.println(", type = " + tellerClient.tdGetDeviceType(deviceId));
				writer.println(", model = " + tellerClient.tdGetModel(deviceId));
				writer.println(", protocol = " + tellerClient.tdGetProtocol(deviceId));
				writer.println("<a href=\"/" + deviceId + "/0\">off</a>");
				writer.println("<a href=\"/" + deviceId + "/1\">on</a>");
				if (pathInfo.length >= 3)
					if (deviceId == Long.parseLong(pathInfo[1]))
					{
						if (Integer.parseInt(pathInfo[2]) == 0)
							tellerClient.tdTurnOff(deviceId);
						else
							tellerClient.tdTurnOn(deviceId);
					}
				try {
					for(DeviceScheduleEntry entry : DeviceLogic.getInstance().getScheduleEntries(new Device(deviceId)))
					{
						writer.println("<br/>&nbsp;&nbsp;&nbsp;#" + entry.getId() + " - " + entry.getStartHour() + ":" + entry.getStartMinute() + " - " + entry.getDescription() + " - " + entry.getValue());
					}
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
