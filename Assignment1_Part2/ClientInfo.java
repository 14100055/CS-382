import java.net.*;

public class ClientInfo
{
	private String name;
	private String password;
	private int port;
	private InetAddress ip;
	private String status;

	public ClientInfo(String clientName, String clientPass, int clientPort, InetAddress clientIP)
	{
		this.name = clientName;
		this.password = clientPass;
		this.port = clientPort;
		this.ip = clientIP;
		this.status = "online";
	}

	public boolean equalsName(String inputName)
	{
		return name.equals(inputName);
	}

	public boolean equalsPass(String inputPass)
	{
		return password.equals(inputPass);
	}

	public boolean equalsStatus(String inputStat)
	{
		return status.equals(inputStat);
	}

	public boolean equalsPort(int inputPort)
	{
		if(port == inputPort)
		{
			return true;
		}
		return false;
	}

	public boolean equalsIP(InetAddress inputIP)
	{
		return ip.equals(inputIP);
	}

	public String getInfo()
	{
		String ret = String.valueOf(port) + " " + ip.toString() + " " + name;
		return ret;
	}

	public void setStatus(String stat)
	{
		status = stat;
	}

	public String getName()
	{
		return name;
	}

	public String getStatus()
	{
		return status;
	}

	public int getPort()
	{
		return port;
	}

	public InetAddress getIP()
	{
		return ip;
	}
}