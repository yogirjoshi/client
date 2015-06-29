package rithm.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import java.util.ArrayList;
import java.util.Properties;

import rithm.commands.*;
import rithm.core.ProgState;
import rithm.defaultcore.DefaultProgramState;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.JsonAdapter;
public class RiTHMClient{
	
	final static Logger logger = Logger.getLogger(RiTHMClient.class);
	protected SSLSocket srvSock;
	protected String serverIPAddr;
	protected int portNo;
	protected String trustStorePath;
	protected String trustStorePass;
	protected boolean isSecure;
	protected RiTHMParameters commandParams;
	protected DataInputStream is;
	protected DataOutputStream os;
	protected Gson gson = new Gson();
	public RiTHMClient(String propFile)
	{
		super();
		InputStream is = null;
		try
		{
			is = new FileInputStream(propFile);
			Properties prop = new Properties();
			prop.load(is);
			serverIPAddr = prop.getProperty("server");
			portNo = Integer.parseInt(prop.getProperty("port"));
			isSecure=Boolean.getBoolean(prop.getProperty("secure"));
			commandParams = new RiTHMParameterValidator().validate(propFile);
			if(isSecure){
				trustStorePath = prop.getProperty("trust_store");
				trustStorePass = prop.getProperty("trust_store_password");
				setSSLParams();
			}
		}
		catch(IOException ie)
		{
			logger.fatal(ie.getMessage());
		}finally{
			if(is != null)
			{
				try {
					is.close();
				} catch (IOException ie) {
					// TODO: handle exception
					logger.fatal(ie.getMessage());
				}
			}
		}
	}
	public boolean readPropertyFile(String propFile)
	{
		commandParams = new RiTHMParameterValidator().validate(propFile);
		if(commandParams == null)
			return false;
		else
			return true;
	}
	public RiTHMCommand createConfigCommand()
	{
		RiTHMCommand rScommand = new RiTHMSetupCommand();
		rScommand.setCommandString("config");
		if(commandParams == null)
			return null;
		rScommand.setRiTHMParameters(commandParams);
		return rScommand;
	}
	public RiTHMCommand createDisconnectCommand()
	{
		RiTHMCommand rScommand = new RiTHMSetupCommand();
		rScommand.setCommandString("disconnect");
		return rScommand;
	}
	private void setSSLParams()
	{
		System.setProperty("javax.net.ssl.trustStore", trustStorePath); 
		System.setProperty("javax.net.ssl.trustStorePassword", trustStorePass);
	}
	public RiTHMClient(String ipAddr, int portNo,String trustStorePath, String trustStorePass)
	{
		super();
		this.serverIPAddr = ipAddr;
		this.portNo = portNo;
		this.isSecure = true;
		this.trustStorePass = trustStorePass;
		this.trustStorePath = trustStorePath;
		setSSLParams();
	}
	public RiTHMClient(String ipAddr, int portNo)
	{
		super();
		this.serverIPAddr = ipAddr;
		this.portNo = portNo;
		this.isSecure = false;

	}
	public boolean isConnected()
	{
		if(srvSock!=null)
			return srvSock.isConnected();
		else
			return false;
	}
	public String readReply() throws IOException
	{
		short size = is.readShort();
		byte[] buffer = new byte[size];is.readFully(buffer);
		logger.info(buffer);
		return new String(buffer);
	}
	public void sendCommand(char code, byte[] msg) throws IOException
	{
		os.writeChar(code);
		os.writeShort(msg.length);
		os.write(msg);
		logger.info(msg);
	}
	public final void sendProgStateJSON(String progStateStr) throws IOException
	{
		sendCommand('J', progStateStr.getBytes());
	}
	public final void sendProgState(ProgState dpState) throws IOException
	{
		sendCommand('J', gson.toJson(dpState).getBytes());
	}
	public boolean sendConfiJSON() throws IOException
	{
		RiTHMCommand rCommand = createConfigCommand();
		if(rCommand != null)
    	{
    		sendCommand('C', gson.toJson(rCommand).getBytes());
    		logger.info(gson.toJson(rCommand));
    		return true;
    	}
		return false;
	}
	public RiTHMReplyCommand processReply() throws JsonSyntaxException, IOException
	{
    	RiTHMReplyCommand reply = gson.fromJson(readReply(),RiTHMReplyCommand.class);
    	logger.info(reply.getCommandString());
    	return reply;
	}
	public void disConnect() throws IOException
	{
		srvSock.close();
	}
	public void connect() {
		// TODO Auto-generated method stub
		SSLSocketFactory sf = (SSLSocketFactory)SSLSocketFactory.getDefault();
	    try {
	    	srvSock = (SSLSocket)sf.createSocket(serverIPAddr, portNo);
	    	is = new DataInputStream(srvSock.getInputStream());
	    	os = new DataOutputStream(srvSock.getOutputStream());

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			logger.fatal(e.getMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.fatal(e.getMessage());
		}
	  
	}



}
