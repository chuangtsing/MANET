/**
 *  SPAN - Smart Phone Ad-Hoc Networking project
 *  Copyright (c) 2012 The MITRE Corporation.
 */
package org.span.manager;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.io.*;

import org.span.R;
import org.span.service.ManetObserver;
import org.span.service.core.ManetService.AdhocStateEnum;
import org.span.service.routing.Node;
import org.span.service.system.ManetConfig;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.util.Log;

public class SendMessageActivity extends Activity implements OnItemSelectedListener, ManetObserver {
	
	private static final String PROMPT = "Enter address ...";
	
	private ManetManagerApp app = null;
    
    private Spinner spnDestination = null;
    private EditText etAddress = null;
    private EditText etMessage = null;
    private Button btnSend = null;
    private Button btnCancel = null;
    private CheckBox cbServer = null;
    private String selection = null;
    
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);	
		
        // init application
        app = (ManetManagerApp)getApplication();
		
		setContentView(R.layout.sendmessageview);
		
	    app = (ManetManagerApp)getApplication();
	    
	    etAddress = (EditText) findViewById(R.id.etAddress);
	    etMessage = (EditText) findViewById(R.id.etMessage);
	    
	    cbServer = (CheckBox) findViewById(R.id.cbServer);
	    
	    app.manet.registerObserver(this);
	    app.manet.sendPeersQuery();
	    
	    spnDestination = (Spinner) findViewById(R.id.spnDestination);
	    spnDestination.setOnItemSelectedListener(this);
		
	    btnSend = (Button) findViewById(R.id.btnSend);
	    btnSend.setOnClickListener(new View.OnClickListener() {
	  		public void onClick(View v) {
	  			String destination = (String) spnDestination.getSelectedItem();
	  			String msg = etMessage.getText().toString();
	  			String address = null;
	  			String error = null, errorMsg = "";
	  			if (selection.equals(PROMPT)) {
	  				address = etAddress.getText().toString();
  					// remove user id
	  				if (address.contains("(")) {
	  					address = address.split("(")[0];
	  				}
		  			if (!Validation.isValidIpAddress(address)) {
		  				error = "Invalid IP address.";
						errorMsg += error + "\n";
		  			}
	  			} else {
	  				address = destination;
	  			}
	  			if (destination == null) {
	  				error = "Destination is empty.";
					errorMsg += error + "\n";
	  			}
	  			if (msg.isEmpty()) {
	  				error = "Message is empty.";
	  				errorMsg += error + "\n";
	  			}
	  			if (errorMsg.isEmpty()) {
	  				
	  				boolean throughServer = cbServer.isChecked();
	  				
	  				Thread t = new Thread(new SendMessageThread(address, msg, throughServer));
		    	    t.start();  
	  				finish();
	  				
	  				
//	  				msg = app.manetcfg.getIpAddress() + " (" + app.manetcfg.getUserId() + ")\n" + msg;
//	  				String retval = null;
//	  				try {
//	  					SendMessageTask task = new SendMessageTask();
//	  					task.execute(new String[] {address, msg});
//	  					retval = task.get();
//	  				} catch (Exception e) {
//	  					retval = "Error: " + e.getMessage();
//	  				}
//	  			    app.displayToastMessage(retval);
	  			    
	  			} else {
	  				// show error messages
	  				AlertDialog.Builder builder = new AlertDialog.Builder(SendMessageActivity.this);
	  				builder.setTitle("Please Make Corrections")
	  					.setMessage(errorMsg.trim())
	  					.setCancelable(false)
	  					.setPositiveButton("OK", null);
	  				AlertDialog alert = builder.create();
	  				alert.show();
	  			}
	  		}
		});
	    
	    btnCancel = (Button) findViewById(R.id.btnCancel);
	  	btnCancel.setOnClickListener(new View.OnClickListener() {
	  		public void onClick(View v) {
				finish();
	  		}
		});
    }
	
	
	@Override
	public void onStart() {
		super.onStart();
	}
	
	@Override
	public void onResume() {
		super.onResume();
	}
	
	@Override
	public void onStop() {
		super.onStop();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		app.manet.unregisterObserver(this);
	}
	
	public static void open(Activity parentActivity) {
		Intent it = new Intent("android.intent.action.SEND_MESSAGE_ACTION");
		parentActivity.startActivity(it);
	}


	@Override
	public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
		selection = (String)spnDestination.getItemAtPosition(position);
		if (selection.equals(PROMPT)) {
			etAddress.setVisibility(EditText.VISIBLE);
			etAddress.setText(app.manetcfg.getIpNetwork());
			etAddress.setSelection(etAddress.getText().length()); // move cursor to end
			app.focusAndshowKeyboard(etAddress);
			cbServer.setEnabled(true);
		} 
		else if (selection.equals(app.manetcfg.getDataServer() + " (Server)")) 
		{
			etAddress.setVisibility(EditText.GONE);
			cbServer.setChecked(false);
			cbServer.setEnabled(false);
		}
		else
		{
			etAddress.setVisibility(EditText.GONE);
			cbServer.setEnabled(true);
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
	}
	

	private class SendMessageTask extends AsyncTask<String, Void, String> {
		 
		 @Override
		 protected String doInBackground(String... params) {
			String address = params[0]; 
			String msg = params[1]; 
			String retval = sendTCPMessage(address, msg);
			finish();
			return retval;
		 }
		 
		 private String sendMessage(String address, String msg) {

			 	String retval = null;
				DatagramSocket socket = null;
				try {
					socket = new DatagramSocket();
					
					byte buff[] = msg.getBytes();
					int msgLen = buff.length;
					boolean truncated = false;
					if (msgLen > MessageService.MAX_MESSAGE_LENGTH) {
						msgLen = MessageService.MAX_MESSAGE_LENGTH;
						truncated = true;
					}
					
					DatagramPacket packet = 
							new DatagramPacket(buff, msgLen, InetAddress.getByName(address), MessageService.MESSAGE_PORT);
					socket.send(packet);
					
					if (truncated) {
						retval = "Message truncated and sent.";
					} else {
						retval = "Message sent.";
					}
					
				} catch (Exception e) {
					e.printStackTrace();
					retval = "Error: " + e.getMessage();
				} finally {
					if (socket != null) {
						socket.close();
					}
				}
				
				return retval;
			}
		 
		 
		 private String sendTCPMessage(String address, String msg) {
			 	String retval = "Message sent.";
				try {
					
					InetAddress serverAddr = InetAddress.getByName(address);
					Socket socket = new Socket(serverAddr, MessageService.MESSAGE_PORT);
					try 
					{
						PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
					    
					    if (out != null && !out.checkError()) 
					    {
					    	out.println(msg);
					        out.flush();
					    }
					}
					catch (Exception e) 
	    			{
	                    e.printStackTrace();
	                    retval = "Error: " + e.getMessage();
	                } 
	    			finally 
	                {
	    				socket.close();
	                }
	    		} 
	    		catch (Exception e) 
	    		{
	                e.printStackTrace();
	                retval = "Error: " + e.getMessage();
	            }
				
				return retval;
		 }
	}; 

	
	private class SendMessageThread implements Runnable{
		private String address;
		private String message;
		private String retval;
		private boolean byServer;
		
		SendMessageThread(String addr, String msg, boolean throughServer)
		{
			this.address = addr;
			this.message = msg;
			this.byServer = throughServer;
		}
		
		private String sendTCPMessage()
		{
		 	String retval = "Message sent.";
			try {
				
				InetAddress serverAddr = InetAddress.getByName(address);
				Socket socket = new Socket(serverAddr, MessageService.MESSAGE_PORT);
				try 
				{
					PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
				    
				    if (out != null && !out.checkError()) 
				    {
				    	out.println(message);
				        out.flush();
				    }
				}
				catch (Exception e) 
				{
	                e.printStackTrace();
	                retval = "Error: " + e.getMessage();
	            } 
				finally 
	            {
					socket.close();
	            }
			} 
			catch (Exception e) 
			{
	            e.printStackTrace();
	            retval = "Error: " + e.getMessage();
	            
	            if(byServer) {
	            	/*Trying to send message to server, then to destination by server*/
	            	ManetManagerApp app = ManetManagerApp.getInstance();
	            	if(!address.equals(app.manetcfg.getDataServer()))
	            	{
	            		message = address + "\n" + message;
		            	String secRetValue = "Instead, message will be sent by server.";
			            try {
			            	
							InetAddress serverAddr = InetAddress.getByName(app.manetcfg.getDataServer());
							Socket socket = new Socket(serverAddr, MessageService.MESSAGE_PORT);
							try 
							{
								PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
							    
							    if (out != null && !out.checkError()) 
							    {
							    	out.println(message);
							        out.flush();
							    }
							}
							catch (Exception ea) 
							{
				                ea.printStackTrace();
				                secRetValue = "Error: " + ea.getMessage();
				            } 
							finally 
				            {
								socket.close();
				            }
						}
			            catch (Exception eb) 
						{
				            eb.printStackTrace();
				            secRetValue = "Error: " + eb.getMessage();
						}
			            finally {
			            	retval += "\n"+secRetValue;
			            }
	            	}	
	            }  
	        }
			
			return retval;
		};
		
		
		final Handler mHandler = new Handler();
		final Runnable mUpdateResults = new Runnable() {
		    public void run() {
		    	ManetManagerApp app = ManetManagerApp.getInstance();
		        Toast.makeText(app.getApplicationContext(), retval, Toast.LENGTH_LONG).show();
		    }
		};

		public void run() {
			ManetManagerApp app = ManetManagerApp.getInstance();
			message = app.manetcfg.getIpAddress() + " (" + app.manetcfg.getUserId() + ")\n" + message;
			retval = sendTCPMessage();
			mHandler.post(mUpdateResults);
		}
	}
	 
	@Override
	public void onServiceConnected() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onServiceDisconnected() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onServiceStarted() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onServiceStopped() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onAdhocStateUpdated(AdhocStateEnum state, String info) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onConfigUpdated(ManetConfig manetcfg) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onRoutingInfoUpdated(String info) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPeersUpdated(HashSet<Node> peers) {
		// provide option to enter peer address
		Set<String> options = new TreeSet<String>();
//		options.add(app.manetcfg.getIpBroadcast() + " (Broadcast)");
		options.add(PROMPT);
		options.add(app.manetcfg.getDataServer() + " (Server)");
		
		String option = null;
		for (Node peer : peers) {
			if (peer.userId != null) {
				option = peer.addr + " (" + peer.userId + ")";
			} else {
				option = peer.addr;	
			}
			options.add(option);
		}
		
		ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, options.toArray());
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spnDestination.setAdapter(adapter);
	}


	@Override
	public void onError(String error) {
		// TODO Auto-generated method stub
		
	}
}

