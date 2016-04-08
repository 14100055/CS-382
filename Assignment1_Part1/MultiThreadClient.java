import java.io.*;
import java.net.*;

public class MultiThreadClient
{

	static BufferedReader reader;
	static DatagramSocket cSocket;
	static InetAddress cIP;
	static String defaultHost;
	static int defaultPort;
	static String otherHost;
	static int otherPort;
	static InetAddress otherIP;

	public static void main(String args[]) throws Exception
	{


		if(args.length < 2)
		{
			System.out.println("Usage: java MultiThreadClient <host> <portNumber>\n");
			defaultHost = "localHost";
			return;
		}
		else
		{
			defaultHost = args[0];
			defaultPort = Integer.parseInt(args[1]);
		}

		reader = new BufferedReader(new InputStreamReader(System.in));
		cSocket = new DatagramSocket(defaultPort);
		cIP = InetAddress.getByName(defaultHost);

		System.out.println("Please enter other client's host (e.g. \"localhost\"):");
		otherHost = reader.readLine();
		otherIP = InetAddress.getByName(otherHost);

		String str;
		System.out.println("Please enter other client's port number (e.g. \"12345\"):");
		str = reader.readLine();
		otherPort = Integer.parseInt(str);

		System.out.println("\nYou can now chat\n");

		ListenSend cListen = new ListenSend();
		ListenSend cSend = new ListenSend();
		
		cListen.listening = true;
		cSend.sending = true;

		cListen.start();
		cSend.start();
	}
}

class ListenSend extends Thread
{
	public boolean listening = false;
	public boolean sending = false;
	int messageLength;

	private void listenPart()
	{
		while(true)
		{
			try
			{
				byte[] recvData = new byte[1024];
				DatagramPacket recvPacket = new DatagramPacket(recvData, 1024);
				MultiThreadClient.cSocket.receive(recvPacket);
				String recvdData = new String(recvPacket.getData());
				System.out.println("Other client: " + recvdData);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	private void sendPart()
	{
		while(true)
		{
			try
			{

				String message = MultiThreadClient.reader.readLine();
				byte[] sendData = new byte[1024];
				sendData = message.getBytes();
				messageLength = sendData.length;
				DatagramPacket sendPacket = new DatagramPacket(sendData, messageLength, MultiThreadClient.otherIP, MultiThreadClient.otherPort);
				MultiThreadClient.cSocket.send(sendPacket);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public void run()
	{
		if(listening)
		{
			listenPart();
		}
		else if(sending)
		{
			sendPart();
		}
	}
}