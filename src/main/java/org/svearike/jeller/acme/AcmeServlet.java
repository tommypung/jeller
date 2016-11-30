package org.svearike.jeller.acme;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class AcmeServlet extends HttpServlet
{
	private static final Logger LOG = Logger.getLogger(AcmeServlet.class.getName());
	private static Map<String, String> sChallenges = new HashMap<>();

	public static void addChallenge(String token, String content)
	{
		synchronized(sChallenges) {
			sChallenges.put(token, content);
			LOG.info("Adding challenge(" + token + ") = " + content);
		}
	}

	public static void removeChallenge(String token)
	{
		synchronized (sChallenges) {
			sChallenges.remove(token);
			LOG.info("Removing challenge(" + token + ")");
		}
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		String token = req.getPathInfo().split("/")[1];
		LOG.info("Serving token(" + token + ")");

		resp.setContentType("text/plain");
		String content = "Not found";
		synchronized(sChallenges) {
			if (!sChallenges.containsKey(token))
				resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			else
				content = sChallenges.get(token);
		}

		resp.getWriter().write(content);
	}
}
