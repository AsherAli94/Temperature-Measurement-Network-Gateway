/*
 ===============================================================================
 Name        : TcpClient.java
 Authors     : Asher Ali
 Version     : 3
 Copyright   : Asher Ali , 03-March-2022
 Description : TCP Client connection application
 ===============================================================================
 */

package ngui;

import java.net.*;
import java.io.*;

/*******************************************************************************
 * Comments: Defines function related to TCP Client connection, receives
*  data from server and store it in ring buffer for data 
*  frame process. 
*******************************************************************************/
public class TcpClient {

	public void tcp_conn(String hostname) {
		Host_Name = hostname;
       
		Thread ConnTHR = new Thread() {
			public void run() {
				boolean do_terminate;
		        do_terminate = false;
		        
		        try (Socket socket = new Socket(Host_Name, Port_Num)) {
		        	
		        	/* parameter to write to the server */
		            output = socket.getOutputStream();
		            writer = new PrintWriter(output, true);
		            
		            /* parameter to receive the string from the server */
		            input = socket.getInputStream();
		            reader = new BufferedReader(new InputStreamReader(input));
		 
		            String line;
		            Is_Connected = true;/* Created by : Asher Ali
		            Matriculation : 38703
		            Date : 2/3/2022	
		         */
		            
		            do {
		            	/*Read the received string and store 
		            	 * it in the ring buffer*/
		            	if ((line = reader.readLine()) != null) {
		            		SBuff[RingB_InP] = line;
		            		if (RingB_Len < RINGB_SIZE) {
		            			RingB_Len++;
		            		}
		            		RingB_InP = (RingB_InP + 1) % RINGB_SIZE;
		                    
		            		do_terminate = line.startsWith("exit");
		            	
		            	} else {
		            		System.out.println("- no data -");
		            		do_terminate = true;
		            	  }
		            } while (!do_terminate);
		            
		        } catch (UnknownHostException ex) {
		        	System.out.println("Server not found: " + ex.getMessage());
		        	
		          } catch (IOException ex) {
		        	  System.out.println("I/O error: " + ex.getMessage());
		            }
		         Is_Connected = false;
			}
		};
		/*The thread is started here after the definition*/
		ConnTHR.start();
	}

	/*Function to write the command string to the server */
	public void TcpSend(String txstr)
	{
		if (Is_Connected) {
			writer.println(txstr);
		}
	}

	/*This function returns the string that is read in the ring buffer */
	public String PopMessage()
	{
		String rline = "";

		if (RingB_Len > 0) {
			rline = SBuff[RingB_OutP];
			RingB_OutP = (RingB_OutP + 1) % RINGB_SIZE;
			RingB_Len--;
		}
		return rline;
	}

	/*Returns TCP connection status*/
	public boolean TcpConnected()
	{
		return Is_Connected;
	}

	public TcpClient()
	{
		Is_Connected = false;
		Host_Name = "172.20.48.31";
		RingB_InP = 0;
		RingB_OutP = 0;
		RingB_Len = 0;
	}
	
	/*Declaration*/
	private boolean Is_Connected;
	private String Host_Name;
	
	/*Should be same as the server port*/
	private final int Port_Num = 50012; 
	
	private OutputStream output;
	private PrintWriter writer;
	private InputStream input;
	private BufferedReader reader;
	private int RingB_InP, RingB_OutP, RingB_Len;
	private final int RINGB_SIZE = 33;
	private String[] SBuff = new String[RINGB_SIZE];

}
