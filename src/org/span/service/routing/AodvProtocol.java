/**
 *  SPAN - Smart Phone Ad-Hoc Networking project
 *  Copyright (c) 2012 The MITRE Corporation.
 */
package org.span.service.routing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import org.span.service.CircularStringBuffer;
import org.span.service.system.CoreTask;
import org.span.service.system.ManetConfig;


import android.util.Log;

public class AodvProtocol extends RoutingProtocol {

	public static final String MSG_TAG = "ADHOC -> AodvProtocol";
	
	public static final String NAME = "Adhoc On-Demand Distance Vector Routing";
	
	private static final int WAIT_TIME_MILLISEC = 1000;
		
	private Process AodvProcess = null;
	
	private AodvStreamThread outputThread = null;
	
	private AodvStreamThread errorThread = null;
	
	@Override
	public String getName() {
		return NAME;
	}
	
	@Override
	public boolean start(ManetConfig manetcfg) {
				
		try {
			stop();
			
			
			
			String command;
			
			command = "insmod " + CoreTask.DATA_FILE_PATH + "/bin/kaodv.ko";
			
			CoreTask.runRootCommand(command);
			
			if(manetcfg.getGatewayInterface().equals(ManetConfig.GATEWAY_INTERFACE_NONE))
			{
				command = CoreTask.DATA_FILE_PATH + "/bin/aodvd" + " -r 30 -s 130.203.8.40 -D -i " 
							+ manetcfg.getWifiInterface();
				AodvProcess = executeAODVRoot(command);
				Thread.sleep(WAIT_TIME_MILLISEC);
				command = "route add default dev " + manetcfg.getWifiInterface();
				CoreTask.runRootCommand(command);
			}
			else
			{
				command = CoreTask.DATA_FILE_PATH + "/bin/aodvd" + " -r 30 -s 130.203.8.40 -c 5 -D -w -i " 
							+ manetcfg.getWifiInterface();
				AodvProcess = executeAODVRoot(command);
				Thread.sleep(WAIT_TIME_MILLISEC);
				command = "iptables -t nat -A POSTROUTING -o " + manetcfg.getGatewayInterface() 
						+ " -j MASQUERADE";
				CoreTask.runRootCommand(command);
			}
			
//			AodvProcess = CoreTask.runRootCommandInBackground(command);
//			AodvProcess = executeAODVRoot(command);
//			Thread.sleep(WAIT_TIME_MILLISEC); // wait for changes to take effect
			
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
    	return AodvProcess != null;
    }
    
	@Override
    public boolean stop() {
    	try {
	    	if (AodvProcess != null) {
	    		AodvProcess = null;
	    	}
    		// check for aodvd process external to this app.
	    	CoreTask.killProcess("aodvd");
	    	String command = "rmmod kaodv";
			CoreTask.runRootCommand(command);
			
			if(outputThread != null){
				outputThread = null;
			}
			if(errorThread != null){
				errorThread = null;
			}
			
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	return true;
    }
	
	@Override
	public boolean isRunning() {
		try {
			return CoreTask.isProcessRunning(CoreTask.DATA_FILE_PATH + "/bin/aodvd");
//			return CoreTask.isProcessRunning("/system/bin/aodvd");
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public HashSet<Node> getPeers() {

		Set<String> peerAddresses = new HashSet<String>();
		
		HashSet<Node> peers = new HashSet<Node>();
		for (String addr : peerAddresses) {
			peers.add(new Node(addr, null));
		}
		
		return peers;
	}

	@Override
	public String getInfo() {
		if (!isRunning()) {
			return null;
		}
		String output = null;

		try {
			if(outputThread != null){
				output = outputThread.getOutput();
			}
//			else{
//				outputThread = new AodvStreamThread(AodvProcess.getInputStream());
//				outputThread.start();
//				output = outputThread.getOutput();
//			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return output;
	}
	
	@Override
	public String getError() {
		return this.error;
	}
	
	private Process executeAODVRoot(String command){
    	try{
			Log.d(MSG_TAG, "ROOT AODV ==> " + command);
			
			// create a dummy script so that the user doesn't have to constantly accept the SuperUser prompt			
			File scriptFile = new File(CoreTask.DATA_FILE_PATH + "/tmp/command.sh");
			scriptFile.delete(); // clear out old content

			scriptFile = new File(scriptFile.getAbsolutePath());
			scriptFile.getParentFile().mkdirs();
			scriptFile.createNewFile();
			
			BufferedWriter writer = new BufferedWriter(new FileWriter(scriptFile));
			writer.append(command);
			writer.close();
			
			// set executable permissions
			CoreTask.chmod(scriptFile.getAbsolutePath(), "0755");
			
			command = "su -c \"" + scriptFile.getAbsolutePath() + "\"";
	    		    	
    	} catch(Exception e) {
        	e.printStackTrace();
    		command = null;
    	}
    	
    	Process process = null;
		try {
			process = Runtime.getRuntime().exec(command);
			
			// we must empty the output and error stream to end the process
			outputThread  = new AodvStreamThread(process.getInputStream());
			errorThread = new AodvStreamThread(process.getErrorStream());
			outputThread.start();
			errorThread.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return process;
	}
	
    private class AodvStreamThread extends Thread {
		
		private InputStream istream = null;
		private CircularStringBuffer buff = new CircularStringBuffer();
		
		public AodvStreamThread(InputStream istream) {
			this.istream = istream;
		}
		
		public String getOutput() {
			return buff.toString().trim();
		}
		
		public void run() {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(istream));
				String line = null;
				while ((line = reader.readLine()) != null) { 
					buff.append(line).append("\n");
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					istream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
