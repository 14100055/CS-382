import java.io.*;
import java.net.*;
import java.util.*;
import java.security.*;

enum ToDo
{
	listening,
	sending
}

class Node extends Thread
{
////Node's own data////
	public int nPort;
	public InetAddress nIP;
	public DatagramSocket nSocket;
	public String nHash;
	public int num;

////predecessor node////
	public int prePort;
	public InetAddress preIP;

////successor node////
	public int sucPort;
	public InetAddress sucIP;

////data store////
	ArrayList<String> fileNames;
	ArrayList<String> hashValues;

////Deciding the thread////
	public ToDo func;
	
	private Thread listenThread;
	private Thread sendThread;

////Copy Constructor////
	public Node(int[] portList, InetAddress[] ipList, String hash, DatagramSocket sock, Thread l, Thread s) throws Exception
	{
		nPort = portList[0];
		nIP = ipList[0];
		prePort = portList[1];
		preIP = ipList[1];
		sucPort = portList[2];
		sucIP = ipList[2];
		nSocket = sock;
		nHash = hash;
		listenThread = l;
		sendThread = s;
	}

////Constructor////
	public Node(String portNum, String strIP) throws Exception
	{
		nPort = Integer.parseInt(portNum);
		nIP = InetAddress.getByName(strIP);
		nSocket = new DatagramSocket(nPort);
		nHash = hashString(strIP + ":" + nPort);
		fileNames = new ArrayList<String>();
		hashValues = new ArrayList<String>();
		listenThread = null;
		sendThread = null;
	}

////Default Constructor////
	public Node() throws Exception
	{
		Random rand = new Random();
		nPort = rand.nextInt(2500) + 10000;
		nIP = InetAddress.getByName("localhost"); 
		nSocket = new DatagramSocket(nPort);
		nHash = hashString(nIP.getHostAddress() + ":" + nPort);
		prePort = 0;
		sucPort = 0;
		fileNames = new ArrayList<String>();
		hashValues = new ArrayList<String>();
		listenThread = null;
		sendThread = null;
	}

////Main function running////
	public static void main(String[] args) throws Exception
	{
		System.out.println("Enter command:");
		System.out.println("1) Create (create a DHT)");
		System.out.println("2) Join (join the DHT)");
		Scanner scanner = new Scanner(System.in);
		int option = scanner.nextInt();

		Node newNode = new Node();
		newNode.func = ToDo.listening;
		newNode.start();

		if(option == 2)
		{
			newNode.joinDHT();
		}
		else
		{
			int[] portList = {newNode.nPort, newNode.prePort, newNode.sucPort};
			InetAddress[] ipList = {newNode.nIP, newNode.preIP, newNode.sucIP};

			Node newerNode = new Node(portList, ipList, newNode.nHash, newNode.nSocket, newNode.listenThread, newNode.sendThread);
			newerNode.func = ToDo.sending;
			newerNode.start();
		}

	}

////Listening////
	private void listenPart()
	{
		listenThread = currentThread();
		while(true)
		{
			try
			{
				printInfo();

				byte[] recvData = new byte[1024];
				DatagramPacket recvPkt = new DatagramPacket(recvData, 1024);
				nSocket.receive(recvPkt);
				int portNum = recvPkt.getPort();
				String message = new String(recvPkt.getData());
				message = message.trim();

				StringTokenizer strtok = new StringTokenizer(message);
				String msg = strtok.nextToken();

				if(msg.compareTo("Join") == 0)
				{
					String strIP = strtok.nextToken();
					String strPort = strtok.nextToken();
					int nodePort = Integer.parseInt(strPort);
					joinCall(message, nodePort, strIP);
				}
				else if(msg.compareTo("Joining") == 0)
				{
					joiningCall(strtok, recvPkt);
				}
				else if(msg.compareTo("GiveToNewNode") == 0)
				{
					String strFiles = "";
					String strIP = strtok.nextToken();
					String strPort = strtok.nextToken();
					String hashVal = hashString(strIP + ":" + strPort);

					for(int i=0; i<fileNames.size(); i++)
					{
						if( hashVal.compareTo(hashValues.get(i)) > 0)
						{
							strFiles = strFiles + fileNames.get(i) + " ";
							fileNames.remove(i);
							hashValues.remove(i);
							i--;
						}

					}
					sendingMessage("FilesUpdate " + strFiles, prePort, preIP);
				}
				else if(msg.compareTo("Leave") == 0)
				{
					String preMsg = "PreUpdate " + preIP.getHostAddress() + " " + prePort;
					sendingMessage(preMsg, sucPort, sucIP);
					String sucMsg = "SucUpdate " + sucIP.getHostAddress() + " " + sucPort;
					sendingMessage(sucMsg, prePort, preIP);

					String strFiles = "";
					for(int i=0; i<fileNames.size(); i++)
					{
						System.out.println(fileNames.size());
						strFiles = strFiles + fileNames.get(i) + " ";
					}
					sendingMessage("FilesUpdate " + strFiles, sucPort, sucIP);
					break;
				}
				else if(msg.compareTo("PreUpdate") == 0)
				{
					String strIP = strtok.nextToken();
					preIP = InetAddress.getByName(strIP);
					String strPort = strtok.nextToken();
					prePort = Integer.parseInt(strPort);
				}
				else if(msg.compareTo("SucUpdate") == 0)
				{
					String strIP = strtok.nextToken();
					sucIP = InetAddress.getByName(strIP);
					String strPort = strtok.nextToken();
					sucPort = Integer.parseInt(strPort);				
				}
				else if(msg.compareTo("FilesUpdate") == 0)
				{
					String strFiles = strtok.nextToken();

					while(strtok.hasMoreTokens())
					{
						String fileName = strtok.nextToken();
						fileNames.add(fileName);
						hashValues.add(hashString(fileName));
					}
				}
				else if(msg.compareTo("Put") == 0)
				{
					String hash = strtok.nextToken();
					String value = strtok.nextToken();
					putCall(hash, value);
				}
				else if(msg.compareTo("Get") == 0)
				{
					String hash = strtok.nextToken();
					String strIP = strtok.nextToken();
					String strPort = strtok.nextToken();
					int nodePort = Integer.parseInt(strPort);
					getCall(hash, nodePort, strIP);
				}
				else if(msg.compareTo("GotValue") == 0)
				{
					System.out.println("\nFile at - " + recvPkt.getAddress() + ":" + recvPkt.getPort());
					System.out.println("Value - " + strtok.nextToken() + "\n");
				}
				printInfo();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

////Sending////
	private void sendPart()
	{
		sendThread = currentThread();
		while(true)
		{
			try
			{
				Scanner scanner = new Scanner(System.in);
				System.out.println("Please select an option number: ");
				System.out.println("1) Leave (leave the DHT)");
				System.out.println("2) Get <key> (get the value with <key> input)");
				System.out.println("3) Put <key> <value> (put the key, value pair in the DHT)");

				int option = scanner.nextInt();

				if(option == 1)
				{
					try
					{
						sendingMessage("Leave", nPort, nIP);
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
				}
				else if(option == 2)
				{
					System.out.println("Enter the file that you wish to GET from the DHT: ");
					Scanner getScanner = new Scanner(System.in);
					String key = getScanner.nextLine();
					String hash = hashString(key);
					gettingCall(hash);
				}
				else if(option == 3)
				{
					System.out.println("Enter the file that you wish to PUT into the DHT: ");
					Scanner putScanner = new Scanner(System.in);
					String val = putScanner.nextLine();
					String hash = hashString(val);
					puttingCall(hash, val);
				}
				else
				{
					System.out.println("Command NOT Recognized!");
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	private void printInfo()
	{
		System.out.println("\nI am: " + nIP.getHostAddress() + " " + nPort);
		if(prePort == 0)
		{
			System.out.println("Predecessor: Does not exist!");
		}
		else
		{
			System.out.println("Predecessor: " + preIP.getHostAddress() + " " + prePort);
		}
		if(sucPort == 0)
		{
			System.out.println("Successor: Does not exist!\n");
		}
		else
		{
			System.out.println("Successor: " + sucIP.getHostAddress() + " " + sucPort + "\n");
		}
	}

	public void sendingMessage(String message, int msgPort, InetAddress msgIP) throws Exception
	{
		byte[] sendData = new byte[1024];
		sendData = message.getBytes();
		int msgLength = sendData.length;
		DatagramPacket sendPkt = new DatagramPacket(sendData, msgLength, msgIP, msgPort);
		nSocket.send(sendPkt);
	}

////JoinDHT - to start process of joining, sent to first node////
	private void joinDHT() throws Exception
	{
		try
		{
			Scanner scanner = new Scanner(System.in);
			System.out.println("Enter the IP of node in DHT: ");
			String strIP = scanner.nextLine();
			System.out.println("Enter the port number of node in DHT: ");
			int nodePort = scanner.nextInt();

			InetAddress nodeIP = InetAddress.getByName(strIP);

			String message = "Join " + nodeIP.getHostAddress() + " " + nPort;
			sendingMessage(message, nodePort, nodeIP);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

////Join call - propogates node/data join request forward////
	private void joinCall(String message, int nodePort, String nodeIP) throws Exception
	{
		String toInsertHash = hashString(nodeIP + ":" + nodePort);
		String sucHash = "";

		if(sucPort != 0)
		{
			sucHash = hashString(sucIP.getHostAddress() + ":" + sucPort);
		}
		String msgToInsert = message;
		InetAddress toInsertIP = InetAddress.getByName(nodeIP);
		int toInsertPort = nodePort;
		boolean joinRequest = false;

		int curWithInsertee = nHash.compareTo(toInsertHash);
		int nextWithInsertee = sucHash.compareTo(toInsertHash);
		int curWithNext = nHash.compareTo(sucHash);

		if(sucPort == 0 || prePort == 0)
		{
			prePort = toInsertPort;
			preIP = toInsertIP;
			sucPort = toInsertPort;
			sucIP = toInsertIP;
			message = "Joining 1";
		}
		else if(curWithInsertee < 0)	//curNode's hash less than nodeToInsert's hash
		{
			if(nextWithInsertee < 0)		//successor's hash is less than nodeToInsert's hash
			{
				if(curWithNext > 0)				//my hash is greater than successor's hash - reached end of DHT
				{
					joinRequest = true;
				}
			}
			else							//general case - successor's hash is greater than nodeToInsert's hash
			{
				joinRequest = true;
			}
		}
		else if( (curWithInsertee > 0) && (nextWithInsertee > 0) && (curWithNext > 0) )
		{
			joinRequest = true;
		}

		if(joinRequest)
		{
			String preMsg = "PreUpdate " + toInsertIP.getHostAddress() + " " + toInsertPort;
			sendingMessage(preMsg, sucPort, sucIP);

			String sucMsg = "SucUpdate " + toInsertIP.getHostAddress() + " " + toInsertPort;
			sendingMessage(sucMsg, nPort, nIP);

			message = "Joining " + sucIP.getHostAddress() + " " + sucPort;
		}
		else
		{
			toInsertPort = sucPort;
			toInsertIP = sucIP;
		}
		sendingMessage(message, toInsertPort, toInsertIP);
	}

////Joining call - actually connects the node////
	private void joiningCall(StringTokenizer strTok, DatagramPacket pkt) throws Exception
	{
		String strIP = strTok.nextToken();
		if(strIP.compareTo("1") == 0)
		{
			prePort = pkt.getPort();
			sucPort = prePort;
			preIP = pkt.getAddress();
			sucIP = preIP;
		}
		else
		{
			prePort = pkt.getPort();
			sucPort = Integer.parseInt(strTok.nextToken());
			preIP = pkt.getAddress();
			sucIP = InetAddress.getByName(strIP);
		}

		String updateFiles = "GiveToNewNode " + " " + nIP.getHostAddress() + " " + nPort;
		sendingMessage(updateFiles, sucPort, sucIP);

		if(sendThread == null)
		{
			int[] portList = {nPort, prePort, sucPort};
			InetAddress[] ipList = {nIP, preIP, sucIP};

			Node newerNode = new Node(portList, ipList, nHash, nSocket, listenThread, sendThread);
			newerNode.func = ToDo.sending;
			newerNode.start();
		}
	}

////put method////
	private void putCall(String toInsertHash, String value) throws Exception
	{
		String preHash = hashString(preIP.getHostAddress() + ":" + prePort);
		boolean shouldPut = false;

		int curWithInsertee = nHash.compareTo(toInsertHash);
		int prevWithInsertee = preHash.compareTo(toInsertHash);
		int curWithPrev = nHash.compareTo(preHash);

		if(curWithInsertee < 0)			//current is smaller than toInsertHash
		{
			if(prevWithInsertee < 0)		//my prev is smaller than toInsertHash
			{
				if(curWithPrev < 0)				//current is smaller than prev - that means end of DHT
				{
					shouldPut = true;
				}
			}
		}
		else if( (curWithInsertee > 0) && (prevWithInsertee < 0) && (curWithPrev > 0) )		//general case
		{
			shouldPut = true;
		}

		if(shouldPut)
		{
			fileNames.add(value);
			hashValues.add(toInsertHash);
			System.out.println("\nFile added at - " + nIP.getHostAddress() + " " + nPort);
		}
		else
		{
			String message = "Put " + toInsertHash + " " + value;
			sendingMessage(message, sucPort, sucIP);
		}
	}

	private void puttingCall(String key, String value) throws Exception
	{
		String message = "Put " + key + " " + value;
		sendingMessage(message, nPort, nIP);
	}

////get method////
	private void getCall(String toCheckHash, int nodePort, String strIP) throws Exception
	{
		String preHash = hashString(preIP.getHostAddress() + ":" + prePort);
		boolean existsHere = false;

		int curWithCheckee = nHash.compareTo(toCheckHash);
		int prevWithCheckee = preHash.compareTo(toCheckHash);
		int curWithPrev = nHash.compareTo(preHash);

		if(curWithCheckee < 0)			//current is smaller than toInsertHash
		{
			if(prevWithCheckee < 0)		//my prev is smaller than toInsertHash
			{
				if(curWithPrev < 0)				//current is smaller than prev - that means end of DHT
				{
					existsHere = true;
				}
			}
		}
		else if( (curWithCheckee > 0) && (prevWithCheckee < 0) && (curWithPrev > 0) )		//general case
		{
			existsHere = true;
		}

		if(existsHere)
		{
			for(int i=0; i<hashValues.size(); i++)
			{
				if( toCheckHash.compareTo( hashValues.get(i) ) == 0)
				{
					String message = "GotValue " + " " + fileNames.get(i) + " " + hashValues.get(i);
					sendingMessage(message, nodePort, InetAddress.getByName(strIP));
				}
			}
		}
		else
		{
			String message = "Get " + toCheckHash + " " + strIP + " " + nodePort;
			sendingMessage(message, sucPort, sucIP);
		}
	}

	private void gettingCall(String key) throws Exception
	{
		String message = "Get " + key + " " + nIP.getHostAddress() + " " + nPort;
		sendingMessage(message, nPort, nIP);
	}

////run for threads////
	public void run()
	{
		if(func == ToDo.listening)
		{
			listenPart();
		}
		else if(func == ToDo.sending)
		{
			sendPart();
		}
	}

////Hash function////
	public static String hashString(String str) throws Exception
	{
		try
		{
			byte[] bytesOfMessage = str.getBytes("UTF8");
			final MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] theDigest = md.digest(bytesOfMessage);
			StringBuffer sb = new StringBuffer();
			for(int i=0; i<theDigest.length; i++)
			{
				sb.append(Integer.toHexString((theDigest[i] & 0xFF) | 0x100).substring(1,3) );
			}
			return sb.toString();
		}
		catch(NoSuchAlgorithmException e)
		{
			e.printStackTrace();
			return null;
		}
	}
}