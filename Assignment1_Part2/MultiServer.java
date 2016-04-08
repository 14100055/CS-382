import java.io.*;
import java.net.*;
import java.util.*;

public class MultiServer
{
	static BufferedReader reader;
	static DatagramSocket sSocket;
	static InetAddress sIP;
	static String defaultHost;
	static int defaultPort;

	static int sendPort;
	static InetAddress sendIP;
	static String contact;
	static String typeInfo;

	static ArrayList<ClientInfo> cInfos;
	static ArrayList<Integer> pingTimes;

	public static void main(String args[]) throws Exception
	{
		if(args.length < 2)
		{
			System.out.println("Usage: java MultiServer <host> <portNumber>\n");
			return;
		}

		defaultHost = args[0];
		defaultPort = Integer.parseInt(args[1]);

		reader = new BufferedReader(new InputStreamReader(System.in));
		sSocket = new DatagramSocket(defaultPort);
		sIP = InetAddress.getByName(defaultHost);

		cInfos = new ArrayList<ClientInfo>();
		pingTimes = new ArrayList<Integer>();

		ServerComponents sListenSend = new ServerComponents();
		ServerComponents sPing = new ServerComponents();

		sListenSend.listeningSending = true;
		sPing.pinging = true;

		sListenSend.start();
		sPing.start();
	}
}

class ServerComponents extends Thread
{
	public boolean listeningSending = false;
	public boolean pinging = false;
	int messageLength;

	public void listenSend()
	{
		String message = "";
		String recvdData;
		byte[] sendData;
		byte[] recvData;
		String[] recvdParts;
		boolean exist = false;
		boolean found1 = false;
		boolean found2 = false;

		while(true)
		{
			try
			{
				//recving data
				recvData = new byte[1024];
				DatagramPacket recvPacket = new DatagramPacket(recvData, 1024);
				MultiServer.sSocket.receive(recvPacket);
				recvdData = new String(recvPacket.getData());

				//find out IP and port to send back info to
				MultiServer.sendPort = recvPacket.getPort();
				MultiServer.sendIP = recvPacket.getAddress();
				String inter = MultiServer.sendIP.getHostName();

				//do if checks to find out type of data to send back
				recvdParts = recvdData.split(" ");
				int numClients = MultiServer.cInfos.size();
				recvdParts[0] = recvdParts[0].trim();

////////////////////////////////////////////////////////////////////////////////////////////////////////////
				if(recvdParts[0].equals("EXIT"))
				{
					recvdParts[1] = recvdParts[1].trim();
					int k=0;
					for(ClientInfo cInfo : MultiServer.cInfos)
					{
						if( (cInfo.equalsPort(MultiServer.sendPort)) && (cInfo.equalsIP(MultiServer.sendIP)) )
						{
							cInfo.setStatus("online");
							MultiServer.cInfos.set(k, cInfo);
						}
						if( cInfo.equalsName(recvdParts[1]) )
						{
							cInfo.setStatus("online");
							MultiServer.cInfos.set(k, cInfo);
						}
						k++;
					}
					continue;					
				}
				else if(recvdParts[0].equals("ping"))
				{
					for(int z=0; z<numClients; z++)
					{
						if( (MultiServer.cInfos.get(z).equalsPort(MultiServer.sendPort))  && (MultiServer.cInfos.get(z).equalsIP(MultiServer.sendIP)) )
						{
							MultiServer.pingTimes.set(z, 0);
						}
					}
					continue;
				}
				///////////////////////// Register //////////////////////////////				
				else if(recvdParts[0].equals("Register"))
				{
					recvdParts[1] = recvdParts[1].trim();
					recvdParts[2] = recvdParts[2].trim();
					recvdParts[3] = recvdParts[3].trim();

					System.out.println(recvdParts[1] + " sent a Register request.");
					for (ClientInfo cInfo : MultiServer.cInfos)
					{
						if( cInfo.equalsName(recvdParts[1]) )
						{
							message = "Username already taken!";
							exist = true;
							break;
						}
					}
					if(!exist)
					{
						message = "You are now registered!";
						MultiServer.pingTimes.add(0);
						ClientInfo newClientInfo = new ClientInfo(recvdParts[1], recvdParts[2], MultiServer.sendPort, MultiServer.sendIP);
						MultiServer.cInfos.add(newClientInfo);
					}
					exist = false;
				}
				/////////////////////////// Login ////////////////////////////////
				else if(recvdParts[0].equals("Login"))
				{
					recvdParts[1] = recvdParts[1].trim();
					recvdParts[2] = recvdParts[2].trim();

					System.out.println(recvdParts[1] + " sent a Login request.");
					int k=0;
					for (ClientInfo cInfo : MultiServer.cInfos)
					{
						if( cInfo.equalsName(recvdParts[1]) )  //if client exists in database
						{
							found1 = true;
							if( cInfo.equalsStatus("offline") )	//user is offline (check password)
							{
								if( cInfo.equalsPass(recvdParts[2]) )
								{
									cInfo.setStatus("online");
									MultiServer.cInfos.set(k, cInfo);
									message = "You are now logged in!";
								}
								else
								{
									message = "Invalid password!";
								}
							}
							else									//user is not offline thus no login
							{
								message = "Username is already logged in!";
							}
							break;
						}
						k++;
					}

					if(!found1)										//client didn't exist in database
					{
						message = "You are not registered, please register first!";
					}
					found1 = false;
				}
				////////////////////////// List All //////////////////////////////
				else if(recvdParts[0].equals("List"))
				{
					System.out.println("Requested all clients.");
					message = "\n";
					for(ClientInfo cInfo : MultiServer.cInfos)
					{
						message = message + cInfo.getName() + "\t" + cInfo.getStatus() + "\n";
					} 
				}
				////////////////////////// Query for a client /////////////////////////
				else if(recvdParts[0].equals("Chat"))
				{
					recvdParts[1] = recvdParts[1].trim();
					System.out.println("Sent a Chat request.");
					int k=0;
					for(ClientInfo cInfo : MultiServer.cInfos)
					{
						if( cInfo.equalsName(recvdParts[1]) )  //if client exists in database
						{
							found2 = true;
							if( cInfo.equalsStatus("online") )	//user is online (initiate connection)
							{
								cInfo.setStatus("busy");
								MultiServer.cInfos.set(k, cInfo);
								
								int r=0;	
								for( ClientInfo cRequester : MultiServer.cInfos )
								{
									if( cRequester.equalsPort(MultiServer.sendPort) && cRequester.equalsIP(MultiServer.sendIP) )
									{
										cRequester.setStatus("busy");
										MultiServer.cInfos.set(r, cRequester);
										message = cRequester.getInfo();
										System.out.println("message to be sent is: " + message);

										sendData = new byte[1024];
										sendData = message.getBytes();
										messageLength = sendData.length;
										DatagramPacket sendPacket = new DatagramPacket(sendData, messageLength, cInfo.getIP(), cInfo.getPort());
										MultiServer.sSocket.send(sendPacket);
									}
									r++;
								}
								message = cInfo.getInfo();
							}
							else									//user is not free thus no login
							{
								message = recvdParts[1] + " is not available!";
							}
							break;
						}
						k++;
					}

					if(!found2)
					{
						System.out.println("End point not found!");
						message = recvdParts[1] + " does not exist!";
					}
					found2 = false;
				}
/////////////////////////////////////////////////////////////////////////////////////////////////
				else
				{
					message = "Invalid command!";
				}
				//sending the message back
				sendData = new byte[1024];
				sendData = message.getBytes();
				messageLength = sendData.length;
				DatagramPacket sendPacket = new DatagramPacket(sendData, messageLength, MultiServer.sendIP, MultiServer.sendPort);
				MultiServer.sSocket.send(sendPacket);

			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public void pingPart()
	{
		ClientInfo cI;
		String stat;
		int index = 0;

		while(true)
		{
			try
			{
				index = 0;
				for(Integer i : MultiServer.pingTimes)
				{
					cI = MultiServer.cInfos.get(index);
					stat = cI.getStatus();
					if(!(stat.equals("offline")))			//should only update ping times if it is not offline already
					{
						i++;
						MultiServer.pingTimes.set(index, i);
						if(i.intValue() > 150)							//not pinged since 15sec, setting it to offline and ping = 0
						{
							cI.setStatus("offline");
							MultiServer.cInfos.set(index, cI);
							i = 0;
						}
					}
					index++;
				}
				Thread.sleep(100);
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}

		}
	}

	public void run()
	{
		if(listeningSending)
		{
			listenSend();
		}
		else if(pinging)
		{
			pingPart();
		}
	}
}