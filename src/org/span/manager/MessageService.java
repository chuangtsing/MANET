/**
 *  SPAN - Smart Phone Ad-Hoc Networking project
 *  Copyright (c) 2012 The MITRE Corporation.
 */
package org.span.manager;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import org.span.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

public class MessageService extends Service {
	
	public static final int MESSAGE_PORT = 9000;
	public static final int MAX_MESSAGE_LENGTH = 256; // 10;
	
	public static final String MESSAGE_FROM_KEY = "message_from";
	public static final String MESSAGE_CONTENT_KEY = "message_content";
	
	private NotificationManager notifier = null;
	
	private Notification notification = null;
	
	private PendingIntent pendingIntent = null;
	
    // one thread for all activities
    private static Thread msgListenerThread = null;
    
    private static Thread msgCheckerThread = null;
    
    private int notificationId = 0;
    
    @Override 
    public void onCreate() {
    	// do nothing until prompted by startup activity
    }    
    
    @Override    
    public int onStartCommand(Intent intent, int flags, int startId) {
    	
    	if (msgListenerThread == null) 
    	{	
	    	msgListenerThread = new MessageListenerThread();
	    	msgListenerThread.start();
    	}
    	
    	if (msgCheckerThread == null)
    	{
    		msgCheckerThread = new MessageCheckerThread();
    		msgCheckerThread.start();
    	}
    	return START_STICKY; // run until explicitly stopped    
	}
    
    @Override    
    public void onDestroy() {        
    	// TODO Auto-generated method stub
	}    
    
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
    
    /**     
     * Show a notification while this service is running.     
     */    
    private void showNotification(String tickerStr, Bundle extras) {
    	
    	if (notifier == null) {
    		// get reference to notifier
    		notifier = (NotificationManager)getSystemService(NOTIFICATION_SERVICE); 
    	}
    	
    	// unique notification id
    	notificationId++;
    	
    	// set the icon, ticker text, and timestamp        
    	notification = 
    		new Notification(R.drawable.exclamation, tickerStr, System.currentTimeMillis());
    	  	
    	Intent intent = new Intent(this, ViewMessageActivity.class);
    	if (extras != null) {
    		intent.putExtras(extras);
    	}
    	
    	// NOTE: Use a unique notification id to ensure a new pending intent is created.
    	
    	// pending intent to launch main activity if the user selects notification	  
    	pendingIntent = 
    		PendingIntent.getActivity(this, notificationId, intent, 0);
    	
    	// cancel the notification after it is checked by the user
    	notification.flags |= Notification.FLAG_AUTO_CANCEL;
    	
    	// vibrate
    	// notification.defaults |= Notification.DEFAULT_VIBRATE; // DEBUG

    	// set the info for the views that show in the notification panel    
    	notification.setLatestEventInfo(this, "Wireless AdHoc", tickerStr, pendingIntent);
    	
    	// NOTE: Use a unique notification id, otherwise an existing notification with the same id will be replaced.
    	
    	// send the notification
    	notifier.notify(notificationId, notification);
    }
    
    private class MessageListenerThread extends Thread {
    	
    	private boolean running = false;
    	
    	public void run() {
    		
    		running = true;
    		ServerSocket serverSocket = null;
    		
	    	try {
	    			serverSocket = new ServerSocket(MESSAGE_PORT);	
	    	}
	    	catch (IOException e)
			{
                e.printStackTrace();
            } 
	    	
	    	Socket client = null;
	    	
	    	while(true){
	    		try{
	    				client = serverSocket.accept();
	    		}
	    		catch (IOException e)
				{
	                e.printStackTrace();
	            }
	    		
	    		Thread t = new Thread(new ReceiveMessage(client, false));
	    	    t.start();    		
	    	}
    	}
    };
    
    
    private class ReceiveMessage implements Runnable {
    	
        private Socket socket;
        private boolean fromServer;
        
        ReceiveMessage(Socket socket, boolean fromServer ) {
            this.socket = socket;
            this.fromServer = fromServer;
        }

        public void run() {
	        BufferedReader in = null;
	        PrintWriter out = null;
	        try {
	            try {
	                /* obtain an input stream to this client ... */
	                in = new BufferedReader(new InputStreamReader(
	                		socket.getInputStream()));
	                /* ... and an output stream to the same client */
	                out = new PrintWriter(new BufferedWriter(
	                		new OutputStreamWriter(socket.getOutputStream())), true);
	            } catch (IOException e) {
	                e.printStackTrace();
	                socket.close();
	                return;
	            }
	
	            String msg;
	            String messages = "";
	            try {
	                while ((msg = in.readLine()) != null) {
	                	messages += msg + "\n";
	                }
	                
	                if (messages != "")
					{
						String from = messages.substring(0, messages.indexOf("\n"));
						
						if(fromServer){
							from += " through server ";
						}
							
						String content = messages.substring(messages.indexOf("\n")+1);
						
						// from = client.getRemoteSocketAddress().toString();
						String tickerStr = "New message";
						
				    	Bundle extras = new Bundle();
				    	extras.putString(MESSAGE_FROM_KEY, from);
				    	extras.putString(MESSAGE_CONTENT_KEY, content);
						
						showNotification(tickerStr, extras);
					}
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	            finally {
	            	if(socket != null)
	            		socket.close();
	            }
	        } catch (IOException e) {
				e.printStackTrace();
			}
        }
    }

    
    private class MessageCheckerThread extends Thread {
    	
    	public void run() 
    	{
    		while(true)
    		{
    			try {
					sleep(1000*60*1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
    			
	    		Socket socket = null;
	    		try {
	    			ManetManagerApp app = ManetManagerApp.getInstance();
	    			InetAddress serverAddr = InetAddress.getByName(app.manetcfg.getDataServer());
	    			socket = new Socket(serverAddr, MESSAGE_PORT);
	    				
	    			Thread t = new Thread(new ReceiveMessage(socket, true));
	    	    	t.start();   
	    		} 
	    		catch (Exception e) {
	    	            e.printStackTrace();
	    		}		
    		}
    	}
    };
}