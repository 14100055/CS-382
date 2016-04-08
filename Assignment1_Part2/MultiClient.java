import java.io.*;
import java.net.*;

public class MultiClient
{
	static BufferedReader reader;
	static DatagramSocket cSocket;
	static InetAddress cIP;
	static String defaultHost;
	static int defaultPort;

	static int otherPort;
	static InetAddress otherIP;

	static int serverPort;
	static InetAddress serverIP;
	static String contact;

	static boolean registered = false;

	public static void main(String args[]) throws Exception
	{
		if(args.length < 2)
		{
			System.out.println("Usage: java MultiClient <host> <portNumber>\n");
			return;
		}

		defaultHost = args[0];
		defaultPort = Integer.parseInt(args[1]);

		reader = new BufferedReader(new InputStreamReader(System.in));
		cSocket = new DatagramSocket(defaultPort);
		cIP = InetAddress.getByName(defaultHost);

		try
		{
			System.out.println("Please enter server's host (e.g. \"localhost\"):");
			String str = reader.readLine();
			serverIP = InetAddress.getByName(str);

			System.out.println("Please enter server's port number (e.g. \"12345\"):");
			str = reader.readLine();
			serverPort = Integer.parseInt(str);

			otherIP = serverIP;
			otherPort = serverPort;
			contact = "SERVER";

			System.out.println("\nYou can now contact server\n");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		ClientComponents cListen = new ClientComponents();
		ClientComponents cSend = new ClientComponents();
		ClientComponents cPing = new ClientComponents();
		
		cListen.listening = true;
		cSend.sending = true;
		cPing.pinging = true;

		cListen.start();
		cSend.start();
		cPing.start();
	}
}

class ClientComponents extends Thread
{
	public boolean listening = false;
	public boolean sending = false;
	public boolean pinging = false;
	int messageLength;
	int pingLength;

	private void listenPart()
	{
		byte[] recvData;

		while(true)
		{
			try
			{
				recvData = new byte[1024];
				DatagramPacket recvPacket = new DatagramPacket(recvData, 1024);
				MultiClient.cSocket.receive(recvPacket);
				String recvdData = new String(recvPacket.getData());

				if(Character.isDigit(recvdData.charAt(0)))
				{
					String[] retval = recvdData.split(" ");
					retval[0] = retval[0].trim();
					retval[1] = retval[1].trim();
					retval[2] = retval[2].trim();

					MultiClient.contact = retval[2];

					MultiClient.otherPort = Integer.parseInt(retval[0]);
					MultiClient.otherIP = InetAddress.getByName(retval[1]);
					String inter = MultiClient.otherIP.getHostName();

					System.out.println("You are now chatting with " + MultiClient.contact);
					System.out.println("Type \"EXIT\" to quit chatting with " + MultiClient.contact);
				}
				else if(recvdData.trim().equals("EXIT"))
				{
					MultiClient.otherPort = MultiClient.serverPort;
					MultiClient.otherIP = MultiClient.serverIP;
					MultiClient.contact = "SERVER";
				}
				else
				{
					System.out.println(MultiClient.contact + ": " + recvdData);
					if(recvdData.trim().equals("You are now registered!"))
					{
						MultiClient.registered = true;
					}
					if(recvdData.trim().equals("You are now logged in!"))
					{
						MultiClient.registered = true;
					}
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	private void sendPart()
	{
		String message;

		System.out.println("Functions and their usage:");
		System.out.println("1) Register (Register \"username\" \"password\" \"port\")");
		System.out.println("2) Login (Login \"username\" \"password\")");
		System.out.println("3) List all clients (List)");
		System.out.println("4) Query client for chat (Chat \"clientName\")");

		while(true)
		{
			try
			{
				message = MultiClient.reader.readLine();

				if(message.equals("EXIT"))
				{
					String eMessage = message + " " + MultiClient.contact;
					byte[] eData = eMessage.getBytes();
					int eMessageLength = eData.length;
					DatagramPacket ePacket = new DatagramPacket(eData, eMessageLength, MultiClient.serverIP, MultiClient.serverPort);
					MultiClient.cSocket.send(ePacket);
				}

				byte[] sendData = new byte[1024];
				sendData = message.getBytes();
				messageLength = sendData.length;
				DatagramPacket sendPacket = new DatagramPacket(sendData, messageLength, MultiClient.otherIP, MultiClient.otherPort);
				MultiClient.cSocket.send(sendPacket);

				if(message.equals("EXIT"))
				{
					MultiClient.otherPort = MultiClient.serverPort;
					MultiClient.otherIP = MultiClient.serverIP;
					MultiClient.contact = "SERVER";
				}

			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	private void pingPart()
	{
		String pingStr = "ping";

		while(true)
		{		
			try
			{
				if(MultiClient.registered)
				{
					byte[] sendPing = new byte[1024];
					sendPing = pingStr.getBytes();
					messageLength = sendPing.length;
					DatagramPacket pingPacket = new DatagramPacket(sendPing, messageLength, MultiClient.serverIP, MultiClient.serverPort);
					MultiClient.cSocket.send(pingPacket);
				}
				Thread.sleep(100);
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
		else if(pinging)
		{
			pingPart();			
		}
	}
}