import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

public class Server {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new Server();
	}
	
	public static Socket socketOfDevice;
	public static Map<String,String> map;
	public static ThreadForDevice threadForDevice;
	
	private static final int PORT=30000;
	private ServerSocket serverSocket;
	private InputStreamReader inputStreamReader;
	private char[] chars;
	private JSONObject json;
	
	public Server() {
		map=new HashMap();
		map.put("1", "1");
		map.put("number2", "number2");
		map.put("number3", "number3");
		map.put("number4", "number4");
		map.put("number5", "number5");
		map.put("number6", "number6");
		try {
			serverSocket=new ServerSocket(PORT);
			while(true) {
				chars=new char[200];
				Socket socket=serverSocket.accept();
				inputStreamReader=new InputStreamReader(socket.getInputStream());
				inputStreamReader.read(chars);
				System.out.println(new String(chars));
				json=JSONObject.fromObject(new String(chars));
				String label=json.getString("label");
				switch(label) {
					case"client":
						new ThreadForClient(socket,json).start();
						break;
					case"device":	
						socketOfDevice=socket;
						threadForDevice=new ThreadForDevice();
						threadForDevice.start();
						break;
					default:
						break;
				}
			}
		}catch(IOException  e) {
			e.printStackTrace();
		}
	}
}

class ThreadForClient extends Thread{
	private Socket socket;
	private JSONObject json;
	public ThreadForClient(Socket socket,JSONObject json) {
		this.socket=socket;
		this.json=json;
	}
	public void run() {
		String name=json.getString("name");
		String password=json.getString("password");
		//判断是否认证成功
		if(Server.map.containsKey(name)&&password.equals(Server.map.get(name))) {		
			//判断电子版是否可用
			if(Server.socketOfDevice==null||Server.socketOfDevice.isClosed()) {
				try {
					OutputStreamWriter outputStreamWriter=new OutputStreamWriter(socket.getOutputStream());
					json=new JSONObject();
					json.put("result", "failure");
					json.put("reason", "the board cannot be used");
					outputStreamWriter.write(json.toString());
					outputStreamWriter.flush();
					socket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else {
				try {
					Server.threadForDevice.name=json.getString("name");
					Server.threadForDevice.content=json.getString("content");
					
					OutputStreamWriter outputStreamWriter=new OutputStreamWriter(socket.getOutputStream());
					json=new JSONObject();
					json.put("result", "success");
					outputStreamWriter.write(json.toString());
					outputStreamWriter.flush();
					socket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}else {
			try {
				OutputStreamWriter outputStreamWriter=new OutputStreamWriter(socket.getOutputStream());
				json=new JSONObject();
				json.put("result", "failure");
				json.put("reason", "name or password is invalid");
				outputStreamWriter.write(json.toString());
				outputStreamWriter.flush();
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}

class ThreadForDevice extends Thread{
	
	public static String name;
	public static String content;
	
	private InputStreamReader inputStreamReader;
	private OutputStreamWriter outputStreamWriter;
	private Socket socket;
	private JSONObject json;
	
	public void run() {
		socket=Server.socketOfDevice;
		char[] chars=new char[1024];
		String newstring,oldstring="";
		boolean flag=true;
		
		try {
			inputStreamReader=new InputStreamReader(socket.getInputStream());
			outputStreamWriter=new OutputStreamWriter(socket.getOutputStream());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		while(flag){
			try {
				socket.setSoTimeout(3*1000);
				inputStreamReader.read(chars);
				newstring=String.valueOf(chars);
				newstring=(newstring.split("}"))[0]+"}";
				System.out.println(newstring);
				if(newstring.equals(oldstring)) {
					continue;
				}else {
					System.out.println(newstring);
					json=JSONObject.fromObject(newstring);
					if(json.getString("intent").equals("check")) {
						System.out.println("check");
						newstring="";
					}
					oldstring=newstring;
				}
				
				if(name!=null && content != null) {
					json=new JSONObject();
					json.put("intent", "show");
					json.put("name", name);
					json.put("content", content);
					outputStreamWriter.write(json.toString());
					outputStreamWriter.flush();
					name=null;
					content=null;
				}else {
					json=new JSONObject();
					json.put("intent", "check");
					outputStreamWriter.write(json.toString());
					outputStreamWriter.flush();
					name=null;
					content=null;
				}
				
			}catch(SocketTimeoutException e1) {
				System.out.println("Time out");
				try {
					socket.close();
					Server.socketOfDevice=null;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			}catch (IOException e2) {
				// TODO Auto-generated catch block
				System.out.println("Connection is cut off!");
				try {
					socket.close();
					Server.socketOfDevice=null;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			}
			
		}
	}
}