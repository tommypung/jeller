package org.svearike.tellstick;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

public class Socket implements Runnable
{
	private byte[] buffer = new byte[512];
	AFUNIXSocket socket;
	private InputStream is;
	private OutputStream os;
	private StringBuilder readData = new StringBuilder();
	private boolean readPolling;

	public Socket(File file, boolean readPolling) throws IOException
	{
		this.readPolling = readPolling;
		socket = AFUNIXSocket.newInstance();
		socket.connect(new AFUNIXSocketAddress(file));
		is = socket.getInputStream();
		os = socket.getOutputStream();

		if (readPolling)
			new Thread(this).start();
	}

	private String readInternal()
	{
		try {
			Arrays.fill(buffer, (byte) 0);
			int read = is.read(buffer, 0, buffer.length);
			if (read > 0)
				return new String(buffer, 0, read);

			return null;
		} catch(IOException e) {
			System.out.println("Connection closed");
			e.printStackTrace();
			return null;
		}
		/*
		16:TDRawDeviceEvent67:class:sensor;protocol:fineoffset;id:216;model:temperature;temp:1.8;i1s
		13:TDSensorEvent10:fineoffset11:temperaturei216si1s
		3:1.8i1479592393s
		 */
	}

	public String read()
	{
		if (readPolling)
		{
			synchronized(readData) {
				String read = readData.toString();
				readData.setLength(0);
				return read;
			}
		}
		else
			return readInternal();
	}

	@Override
	public void run()
	{
		System.out.println("Starting poll");
		while(true)
		{
			String read = readInternal();
			if (read != null)
			{
				synchronized(readData) {					
					readData.append(read);
				}
			}

			System.out.println("readInternal: " + read);
		}
	}

	public void write(String string) throws UnsupportedEncodingException, IOException
	{
		System.out.println("sending: " + string);
		os.write(string.getBytes("UTF-8"));
	}

	public void close()
	{
		try { is.close(); } catch(Exception e) { }
		try { os.close(); } catch(Exception e) { }
		try { socket.close(); } catch(Exception e) { }
	}
}
