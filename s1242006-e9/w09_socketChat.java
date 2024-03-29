import java.net.*;
import java.io.*;
import java.util.Vector;

public class w09_socketChat
{
	static final int port = 12345;
	
	static BufferedReader getReader(InputStream is)
	{
		return new BufferedReader(new InputStreamReader(is));
	}
	
	static BufferedWriter getWriter(OutputStream os)
	{
		return new BufferedWriter(new OutputStreamWriter(os));
	}

	static void writeString(String str, BufferedWriter out) throws Exception
	{
		out.write(str, 0, str.length());
		out.newLine();
		out.flush();
	}

	static class AllSockets
	{
		static private Vector<Socket> allSockets = new Vector<Socket>();

		synchronized static public void Add(Socket client) { allSockets.add(client); }

		synchronized static public void Remove(Socket client) { allSockets.remove(client); }
		
		synchronized static public void Broadcast(Socket from, String str) throws Exception
		{
			for(Socket s : allSockets)	// send str to everybody except from
				if(s != from)
					writeString(str, getWriter(s.getOutputStream()));
		}
	}
	
	static class EchoIncoming implements Runnable
    {
		BufferedReader reader;

        public EchoIncoming(BufferedReader r) { reader = r; } 

        public void run()
        {
			try
			{
				for(;;)
					System.out.println(reader.readLine());
			}
			catch(Exception e) {}
		}
    }

	static void Client(String host, String name) throws Exception
	{
		System.out.println(name + " is connecting to " + host);
		Socket s = new Socket(host, port);
		
		new Thread(new EchoIncoming(getReader(s.getInputStream()))).start();
		
		BufferedReader in = getReader(System.in);

		BufferedWriter out = getWriter(s.getOutputStream());
		
		if(in.equals("*quit")){
			s.close();
		}
		for(;;)
			writeString(name + "> " + in.readLine(), out);
	}
	
	static class Broadcast implements Runnable
	{
		Socket client;
		
		public Broadcast(Socket c) { client = c; }
		
		public void run()
		{
			try
			{
				BufferedReader in = getReader(client.getInputStream());
				
				for(;;)
				{
					String msg = in.readLine();	// get a new message from the client
					String [] tmp = msg.split(" ");
					if(tmp[1].equals("*quit")) {
						msg = (tmp[0].replace(">","")).concat(" is leaving the chat.");
						AllSockets.Broadcast(client,msg);
						AllSockets.Remove(client);
						client.close();
						return;
					}
					AllSockets.Broadcast(client, msg);

 				}
			}
			catch(Exception e) 
			{
			}
		}
	}
		
	static void Server() throws Exception
	{
		ServerSocket s = new ServerSocket(port);
		
		for(;;)
		{
			System.out.println("listening");
			Socket client = s.accept();
			AllSockets.Add(client);
			new Thread(new Broadcast(client)).start();
		}
	}
		
	// try running several clients and one server:
	
	// java SocketChat server 
	// java SocketChat client 127.0.0.1 
	// java SocketChat client 127.0.0.1 
	// java SocketChat client 127.0.0.1 

	public static void main(String args[])
	{
		try
		{
			if(args[0].equals("client"))
				Client(args[1], args[2]);
			else
				Server();
		}
		catch(Exception e)
		{
			System.out.println("Connection error!");
		}
	}
}
