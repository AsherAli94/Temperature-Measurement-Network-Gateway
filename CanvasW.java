/*
 ===============================================================================
 Name        : CanvasW.java
 Authors     : Asher Ali
 Version     : 3
 Copyright   : Prof. Dr. Ing. Kai Mueller , 05-DEC-2020
 Description : Display of TCP connection, RTD sensor connection, CANOpen connection, Sensor Data 
			   and Time Stamp on the Canvas Panel.
 ===============================================================================
 */
package ngui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

public class CanvasW extends JPanel {

	private static final long serialVersionUID = 1L;

	@Override
	public void paint(Graphics g) {
		Graphics2D grphc2d = (Graphics2D) g;
		String TCP_str, CAN_str, S0_str, S1_str, S2_str,S3_str,S0TP_str, S1TP_str, S2TP_str,S3TP_str, RTD_str,tperiod_str;
		int  k, bmaskP,bmaskS;

		g.clearRect(0, 0, super.getWidth(), super.getHeight());
		g.setFont(monoFont);
		TCP_str = String.format("TCP Server: %s", TCP_Server);
		
		/* TCP Server-Client Connection updation */
		if (TCP_Server.equals("0.0.0.0")) {
			grphc2d.setColor(Color.red);
		} else {
			grphc2d.setColor(m_tGreen);
		}
		g.drawString(TCP_str, 10, 12);

		/* CANOpen Connection updation */
		if (CAN_Server == 0) {
			CAN_str = "CANopen:  ---";
			grphc2d.setColor(Color.red);
		} else {
			CAN_str = String.format("CANopen: Connected");
			grphc2d.setColor(m_tGreen);	
		}
		g.drawString(CAN_str, 400, 12);
		grphc2d.setColor(Color.black);
		
		/* CANOpen Connection updation */
		
			
		tperiod_str = String.format("Sensors Sampling period");
			
		g.drawString(tperiod_str, 600, 67);
		
		
		/* RTDSensor Connection updation */
		if (RTD_Server == 0) {
			RTD_str = "RTDSensor:  ---";
			grphc2d.setColor(Color.red);
		} else {
			RTD_str = String.format("RTDSensor: Connected");
			grphc2d.setColor(m_tGreen);	
		}
		g.drawString(RTD_str, 800, 12);
		grphc2d.setColor(Color.black);
		
		/* To Display Sensor Data Value and Time Stamp */
		S0_str = String.format("S0: %2x  [%10.3f us]", S[0], T_S[0]);
		g.drawString(S0_str, 10, 42);	
		S1_str = String.format("S1: %2x  [%10.3f us]", S[1], T_S[1]);
		g.drawString(S1_str, 10, 67);
		S2_str = String.format("S2: %.2f oC [%10.3f ms]", CAN_S[2], T_S[2]);
		g.drawString(S2_str, 400, 42);
		S3_str = String.format("S3: %.2f Ohm [%10.3f ms]", RTD_S[3], T_S[3]);
		g.drawString(S3_str, 800, 42);
		
		// To display the sampling period of the sensors
		
		S0TP_str = String.format("S0: [%10.3f %s] ", TP_S[0], U_S[0]);
		g.drawString(S0TP_str, 400, 87);	
		S1TP_str = String.format("S1: [%10.3f %s] ", TP_S[1], U_S[1]);
		g.drawString(S1TP_str, 400, 107);
		S2TP_str = String.format("S2: [%10.3f %s] ", TP_S[2], U_S[2]);
		g.drawString(S2TP_str, 800, 87);
		S3TP_str = String.format("S3: [%10.3f %s] ", TP_S[3], U_S[3]);
		g.drawString(S3TP_str, 800, 107);
		
		/* To set and fill the color into the rectangular shape pushbutton
		 *  box according to pushbutton value */
		bmaskS = 1 << (N_SW - 1);
		for (k = 0; k < N_SW; k++) {
			if ((S[0] & bmaskS) != 0) {
				grphc2d.setColor(Color.green);
				grphc2d.fillRect(180+k*25, 90, 15, 15);
			} else {
				grphc2d.setColor(Color.red);
				grphc2d.drawRect(180+k*25, 90, 15, 15);
			}
			bmaskS >>= 1;
		}
		
		/* To set and fill the color into the oval shape switches 
		 * according to received switch value */
		bmaskP = 1 << (N_PB - 1);
		for (k = 0; k < N_PB; k++) {
			if ((S[1] & bmaskP) != 0) {
				grphc2d.setColor(Color.green);
				grphc2d.fillOval(10+k*25, 90, 15, 15);
			} else {
				grphc2d.setColor(Color.red);
				grphc2d.drawOval(10+k*25, 90, 15, 15);
			}
			bmaskP >>= 1;
		}			
	}
	
	/* To store the sensor data value and time stamp for the enabled sensor */
	public void Update(double [] mtime, double [] mval , int i)
	{		
		T_S[i]= mtime[i];
		S[i]= (int) mval[i];	
	}
	
	public void Update_tperiod(String txstr_can)
	{	
		if (Double.parseDouble(txstr_can.substring(1,2)) == 0)
		{
			//splitting of string at whitespace
    		String[] splitStr = txstr_can.split(" ");
    		
    		// converting string into Double
    		
			TP_S[0] = Double.parseDouble(splitStr[2]);
			
			// printing the Unit of sampling period
			
			U_S[0] = splitStr[3];
		}
		
	
			
		
		else if (Double.parseDouble(txstr_can.substring(1,2)) == 1)
		{
			
			//splitting of string at whitespace
    		String[] splitStr = txstr_can.split(" ");
    		
    		// converting string into Double
    		
			TP_S[1] = Double.parseDouble(splitStr[2]);
			
			// printing the Unit of sampling period
			
			U_S[1] = splitStr[3];
		}
		else if (Double.parseDouble(txstr_can.substring(1,2)) == 2)
		{
			//splitting of string at whitespace
    		String[] splitStr = txstr_can.split(" ");
    		
    		// converting string into Double
    		
			TP_S[2] = Double.parseDouble(splitStr[2]);
			
			// printing the Unit of sampling period
			
			U_S[2] = splitStr[3];
		}
		else if (Double.parseDouble(txstr_can.substring(1,2)) == 3)
		{
			//splitting of string at whitespace
    		String[] splitStr = txstr_can.split(" ");
    		
    		// converting string into Double
    		
			TP_S[3] = Double.parseDouble(splitStr[2]);
			
			// printing the Unit of sampling period
			
			U_S[3] = splitStr[3];
		}
		
	}
	
	/* To store the CANOpen sensor data value and time stamp */
	public void UpdateCAN(double [] mtime, double [] mval , int i)
	{			
		T_S[i]= mtime[i];
		
		
		CAN_S[i] = mval[i];	
		
	}
	
	/* To store the CANOpen sensor data value and time stamp */
	public void UpdateRTD(double [] mtime, double [] mval , int i)
	{			
		T_S[i]= mtime[i];
		
		
		RTD_S[i] = mval[i];	
		
	}
	
	/* To store the IP address of host */
	public void UpdateHostname(String newhost)
	{
		TCP_Server = newhost;
		
	}
	
	/* Function to check whether CANOpen sensor is enabled and set CAN_Server
	 * to display connection status on CANvas  */
	public void UpdateCANServer(String txstr_can)
	{
		if (Double.parseDouble(txstr_can.substring(2,3)) == 2) {
		
			if (RTD_Server == 1)
			{
				RTD_Server =0;
			}
			if ((txstr_can.substring(0,3)).equals("~S2")){
					CAN_Server = 1;
														}
				else {
					CAN_Server = 0;
				}
			}
		}
	
	public void UpdateRTDServer(String txstr_rtd)
	{
		if (Double.parseDouble(txstr_rtd.substring(2,3)) == 3) {
			
			if (CAN_Server == 1)
			{
				CAN_Server =0;
			}
			if ((txstr_rtd.substring(0,3)).equals("~S3")){
					RTD_Server = 1;
														}
				else  {
					RTD_Server = 0;
				}
			}
		}
	
	/* Default initialization */
	public CanvasW()
	{
		TCP_Server = "0.0.0.0";
		CAN_Server = 0;
		RTD_Server = 0;
		for (i=0; i < NumSensors; i++)
		{
			S[i] = 0;  
			T_S[i] = 0.0;
		}	
	}
	
	/* Declarations */
	private static Color m_tGreen = new Color(0, 128, 0);
	private static Font monoFont = new Font("Monospaced", Font.BOLD, 14);
	private String TCP_Server;
	private int CAN_Server;
	private int	RTD_Server;
	private int i,  NumSensors = 4;
	private final int N_SW = 8;
	private final int N_PB = 5;
	private int[] S = new int[100];
	private double[] CAN_S = new double[3];
	private double[] RTD_S = new double[4];
	private double[] T_S = new double[100];
	private double[] TP_S = new double[100];
	private String [] U_S = new String [100];
}
