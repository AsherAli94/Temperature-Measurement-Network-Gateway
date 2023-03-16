// Created by : Asher Ali


package ngui;

import java.awt.Color;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SpringLayout;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JPanel;

import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import javax.swing.BoxLayout;
import java.awt.BorderLayout;
import java.awt.Panel;


public class MNGhmi {

	private JFrame frame;
	private JTextField textField_cmd;

	/* Launch the application*/
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MNGhmi window = new MNGhmi();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		
	}
	
	/*Create the application*/
	public MNGhmi() {
		initialize();
	}
	
	/*Initialize the contents of the frame*/
	private void initialize() {
		frame = new JFrame();
		//frame.setBounds(100, 100, 1310, 822);
		frame.setBounds(100, 100, 1357, 828);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenuItem mntmExit = new JMenuItem("Exit");
		mntmExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		mnFile.add(mntmExit);
		
		
			InitCharts();	
		
		/*Add the sensor data and the time-stamp on the respective graphs*/
		SW_Chart.addSeries("S0", Stime[0], Sdata[0]);
		PB_Chart.addSeries("S1", Stime[1], Sdata[1]);
		CAN_Chart.addSeries("S2", Stime[2], Sdata[2]);
		RTD_Chart.addSeries("S3", Stime[3], Sdata[3]);
		
		/*Executes when command is entered on the GUI text pane */
		textField_cmd = new JTextField();
		textField_cmd.setFont(new Font("Monospaced", Font.PLAIN, 12));
		textField_cmd.setBounds(10, 740, 1322, 19);
		textField_cmd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				CommandHandler(textField_cmd.getText());
				textField_cmd.setText("");
			}
		});
		frame.getContentPane().setLayout(null);
		frame.getContentPane().setLayout(null);
		frame.getContentPane().setLayout(null);
		frame.getContentPane().add(textField_cmd);
		textField_cmd.setColumns(10);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setFont(new Font("Monospaced", Font.PLAIN, 12));;
		scrollPane.setBounds(10, 0, 234, 738);
		frame.getContentPane().add(scrollPane);
		
		textPane_log = new JTextPane();
		textPane_log.setFont(new Font("Monospaced", Font.PLAIN, 12));
		textPane_log.setText("MNGhmi Initialized! \n");
		PrintTxtWin("\nS0: Sensor 0: Switches\nS1: Sensor 1: PushButtons\n"
				+ "S2: Sensor 2: CAN-Temperature\n"+ "S3: Sensor 3: RTD-Resistance\n",2,true);
		PrintTxtWin("Type help for command formats",1,true);
		textPane_log.setEditable(false);
		scrollPane.setViewportView(textPane_log);
		
		// Panels creation to be shown on GUI
		
		panel_canv = new CanvasW();
		panel_canv.setBounds(254, 10, 1078, 137);
		frame.getContentPane().add(panel_canv);
		panel_canv.setLayout(new SpringLayout());
		
		panel_SW = new XChartPanel<XYChart>(SW_Chart);
		panel_SW.setBounds(254, 148, 1078, 145);
		frame.getContentPane().add(panel_SW);
		panel_SW.setLayout(new SpringLayout());
		
		panel_PB = new XChartPanel<XYChart>(PB_Chart);
		panel_PB.setBounds(254, 294, 1078, 143);
		frame.getContentPane().add(panel_PB);
		panel_PB.setLayout(new SpringLayout());
		
		panel_CAN = new XChartPanel<XYChart>(CAN_Chart);
		panel_CAN.setBounds(254, 439, 1078, 157);
		frame.getContentPane().add(panel_CAN);
		panel_CAN.setLayout(new SpringLayout());
		
		panel_RTD = new XChartPanel<XYChart>(RTD_Chart);
		panel_RTD.setFont(new Font("Monospaced", Font.PLAIN, 12));
		panel_RTD.setBounds(254, 601, 1078, 137);
		frame.getContentPane().add(panel_RTD);
		panel_RTD.setLayout(new SpringLayout());
		
		/*Timer for Update Dynamic (Data Frame processing) function*/
		DispUpdate_Timer = new Timer(100, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                UpdateDynamic();
                DispUpdate_Timer.restart();
            }
        });
        DispUpdate_Timer.start();
	
		
		
		TcpOBJ = new TcpClient();
	}



	private void PrintTxtWin(String twstr, int twstyle, boolean newline) {
	    try {
	        Document doc = textPane_log.getStyledDocument();
	        StyleConstants.setItalic(TextSet, false);
	        StyleConstants.setBold(TextSet, false);
	        StyleConstants.setForeground(TextSet, Color.BLACK);
	        switch (twstyle) {
	            case 0:
	                StyleConstants.setBold(TextSet, true);
	                StyleConstants.setForeground(TextSet, Color.DARK_GRAY);
	                break;
	            case 1: StyleConstants.setForeground(TextSet, Color.BLUE);
	                break;
	            case 2: StyleConstants.setForeground(TextSet, Color.BLACK);
	                break;
	            case 3: StyleConstants.setForeground(TextSet, Color.RED);
	            	break;
	            case 4: StyleConstants.setForeground(TextSet, Color.GREEN);
	            	break;
	            default:
	                doc.remove(0, doc.getLength());
	        }
	        if (twstyle >= 0) {
	        	textPane_log.setCharacterAttributes(TextSet, true);
	        	if (newline) {
	                doc.insertString(doc.getLength(), twstr+"\n", TextSet);            		
	        	} else {
	                doc.insertString(doc.getLength(), twstr, TextSet);
	        	}
	        }
	    } catch (BadLocationException ex) {
	        System.out.println(ex.toString());
	    }
	}
//// Function to get the commands
	private void CommandHandler(String cmds) {
		String txstr, hostname;
	
		if (cmds.equals("help")) {
			PrintTxtWin("cll - clear log window\n", 1, true);
			PrintTxtWin("tcps - ip address\n", 1, true);
			PrintTxtWin("tx - transmit to server\n", 1, true);
			PrintTxtWin("exit - exit MNGhmi GUI\n", 1, true);
			// PrintTxtWin("tcps {ip address} - \nconnect to TCP server\n",
			// 1, true);
			PrintTxtWin("--- Please enter one of the following options after tx---\n", 1, true);
			PrintTxtWin("Sensor_type  No_iterations	 Sampling_rate Unit \n", 1, true);
			PrintTxtWin("Example S0 10 100 ms\n", 1, true);
			PrintTxtWin("auto (for automatic execution of sensors \n)", 1, true);
			PrintTxtWin("manual (for exiting from auto mode \n)", 1, true);
			PrintTxtWin("exit (only exiting from server \n)", 1, true);
			
		} else if (cmds.equals("cll")) {
			PrintTxtWin("", -1, false);
		} else if (cmds.equals("tcps")) {
			PrintTxtWin("Connecting to 172.20.48.31...", 0, true);
			hostname = "172.20.48.31";
			TcpOBJ.tcp_conn(hostname);
			((CanvasW) panel_canv).UpdateHostname(hostname);
			panel_canv.repaint();
		
		} else if (cmds.startsWith("tx ")) {
			txstr = cmds.substring(3);
			if (txstr.startsWith("S0")) {
				PrintTxtWin(String.format("sending [%s]", txstr), 0, true);
				TcpOBJ.TcpSend(txstr);
				((CanvasW) panel_canv).Update_tperiod(txstr);
			}
			else if (txstr.startsWith("S1")) {
				PrintTxtWin(String.format("sending [%s]", txstr), 0, true);
				TcpOBJ.TcpSend(txstr);
				((CanvasW) panel_canv).Update_tperiod(txstr);
				
			}
			else if (txstr.startsWith("S2")) {
				PrintTxtWin(String.format("sending [%s]", txstr), 0, true);
				TcpOBJ.TcpSend(txstr);
				
				((CanvasW) panel_canv).Update_tperiod(txstr);	
				
				
			}
			else if (txstr.startsWith("S3")) {
				PrintTxtWin(String.format("sending [%s]", txstr), 0, true);
				TcpOBJ.TcpSend(txstr);
				
				((CanvasW) panel_canv).Update_tperiod(txstr);
				
			}
			else if (txstr.startsWith("auto")) {
				PrintTxtWin(String.format("sending [%s]", txstr), 0, true);
				TcpOBJ.TcpSend(txstr);
		
			}
			else if (txstr.startsWith("manual")) {
				PrintTxtWin(String.format("sending [%s]", txstr), 0, true);
				TcpOBJ.TcpSend(txstr);
		
				
			}
			else if (txstr.startsWith("exit")) {
				PrintTxtWin(String.format("sending [%s]", txstr), 0, true);
				TcpOBJ.TcpSend(txstr);
				
				
			}
			
			else {
				PrintTxtWin("Write the valid command", 0, true);
	
			}
	
		} else if (cmds.equals("exit")) {
			txstr = "exit";
			PrintTxtWin("Communication stopped", 1, true);
			TcpOBJ.TcpSend(txstr);
			System.exit(0);
		} else {
			PrintTxtWin(" *** unknown cmd ***", 3, true);
		}
	}
	
	/*Initialization of XY data on graph */
	private void InitCharts()
	{
		int k;
		for (i = 0; i < NumSensors ;i++) {
			for (k = 0; k < CHART_POINTS; k++) {
				Sdata[i][k] = 0.01* k;
				Stime[i][k] = 0.001 * k;		
			}	
		}	
	}

	/**************************************************************************/
	/* Function: Data Frame processing
	 * Comments: Extracting, converting and passing the 
	 * sensor data to the graph */
	/**************************************************************************/
    private void UpdateDynamic() {
    	String rx_str;
    	double[] mtime = new double [NumSensors];
    	double[] mval = new double [NumSensors];
    	double init_time;
    	double init_value;
    	
    	int k;
    	double latest_time_SW = 0 ;
    	double latest_time_PB = 0 ;
    	double latest_time_CAN = 0 ;
    	double latest_time_RTD = 0 ;
    	

    	if (TcpOBJ.TcpConnected()) {
    		do {
    			rx_str = TcpOBJ.PopMessage();		
        		if (!rx_str.isEmpty()) {
        			
        			// Storing the initial time and value 
        		 if (rx_str.startsWith("~INIT")) {
        				PrintTxtWin(String.format("rx string: [%s]",  rx_str), 3, true);
        				try {
        						String[] splitStr = rx_str.split(" ");
        						
        						init_value = Double.parseDouble(splitStr[2]);
        					
        						init_time = Double.parseDouble(splitStr[1]);
        						
        					
        				} catch (NumberFormatException e) {PrintTxtWin(e.toString(), 3, true);}
        				
        			}
        		 
        		 
        		 else if (rx_str.startsWith("~S0")) {
        			
        				PrintTxtWin(String.format("rx string: [%s]",  rx_str), 3, true);
        				try {
        					
        					i=0;
        					
        					//splitting of string at whitespace
                    		String[] splitStr = rx_str.split(" ");
                    	
                    		
                    		// converting string into Double
                    		mval[i] = Double.parseDouble(splitStr[2]);
                    		mtime[i] = Double.parseDouble(splitStr[1]);
                    		
                    		
        					// allowing only the new timestamp and data to show on the graph
                    		
        					if(latest_time_SW > mtime[i])
        					{
        						continue;
        					}
        					else
        					{
        						latest_time_SW = mtime[i];
        					}

                		// Shifting the data coming from the sensor left to show on the graph
        					
                		for (k=0; k<CHART_POINTS-1; k++)
                		{	                			
                			Stime[i][k] = Stime[i][k+1];
                			Sdata[i][k] = Sdata[i][k+1];
                		}
                		
                		Stime[i][CHART_POINTS-1] = mtime[i];
                		Sdata[i][CHART_POINTS-1] = mval[i];
                		S = String.format("S%d", i);
        				
        				Sdata[i][CHART_POINTS-1]= Sdata[i][CHART_POINTS - 1]; 
        				SW_Chart.updateXYSeries(S, Stime[i], Sdata[i], null);
        				panel_SW.repaint();

        				((CanvasW) panel_canv).Update(mtime, mval,i);
        				panel_canv.repaint();
        				} catch (NumberFormatException e) {PrintTxtWin(e.toString(), 3, true);}
        			}
        			
        			// Reading and displaying of data for Push Button
        			
        			else if (rx_str.startsWith("~S1")) {
        				
        				PrintTxtWin(String.format("rx string: [%s]",  rx_str), 3, true);
        				try {
        					
        					i=1;
        					
        					
        					 //splitting of string at whitespace
                    		String[] splitStr = rx_str.split(" ");
                    	
                    		
                    		// converting string into Double
                    		mval[i] = Double.parseDouble(splitStr[2]);
                    		mtime[i] = Double.parseDouble(splitStr[1]);
                    		
                    		// allowing only the new timestamp and data to show on the graph
        					
        					if(latest_time_PB > mtime[i])
        					{
        						continue;
        					}
        					else
        					{
        						latest_time_PB = mtime[i];
        					}

        				// Shifting the data coming from the sensor left to show on the graph
        					
                		for (k=0; k<CHART_POINTS-1; k++)
                		{	                			
                			Stime[i][k] = Stime[i][k+1];
                			Sdata[i][k] = Sdata[i][k+1];
                		}
                		
                		Stime[i][CHART_POINTS-1] = mtime[i];
                		Sdata[i][CHART_POINTS-1] = mval[i];
                		S = String.format("S%d", i);
        			
        				Sdata[i][CHART_POINTS-1]= Sdata[i][CHART_POINTS - 1];
        				PB_Chart.updateXYSeries(S, Stime[i], Sdata[i], null);
        				panel_PB.repaint();

        				((CanvasW) panel_canv).Update(mtime, mval,i);
        				panel_canv.repaint();
        				} catch (NumberFormatException e) {PrintTxtWin(e.toString(), 3, true);}
        			}
        			
        			// Reading and displaying of data for CAN Sensor
        			
        			else if (rx_str.startsWith("~S2")) {
        				
        				PrintTxtWin(String.format("rx string: [%s]",  rx_str), 3, true);
        				try {
        					((CanvasW) panel_canv).UpdateRTDServer(rx_str);
        					
        					i=2;
        					
        					
        					 //splitting of string at whitespace
                    		String[] splitStr = rx_str.split(" ");
                    	
                    		
                    		// converting string into Double
                    		mval[i] = Double.parseDouble(splitStr[2]);
                    		mtime[i] = Double.parseDouble(splitStr[1]);
                    		
                    		// allowing only the new timestamp and data to show on the graph
        					
        					if(latest_time_CAN > mtime[i])
        					{
        						continue;
        					}
        					else
        					{
        						latest_time_CAN = mtime[i];
        					}

                		
                		for (k=0; k<CHART_POINTS-1; k++)
                		{	                			
                			Stime[i][k] = Stime[i][k+1];
                			Sdata[i][k] = Sdata[i][k+1];
                		}
                		
                		Stime[i][CHART_POINTS-1] = mtime[i];
                		Sdata[i][CHART_POINTS-1] = mval[i];
                		S = String.format("S%d", i);
        				
        				Sdata[i][CHART_POINTS-1]= Sdata[i][CHART_POINTS - 1]; 
        				CAN_Chart.updateXYSeries(S, Stime[i], Sdata[i], null);
        				panel_CAN.repaint();

        				((CanvasW) panel_canv).UpdateCAN(mtime, mval,i);
        				panel_canv.repaint();
        				} catch (NumberFormatException e) {PrintTxtWin(e.toString(), 3, true);}
        			}
        			
        			// Reading and displaying of data for RTD Sensor
        			
        			else if (rx_str.startsWith("~S3")) {
        				
        				PrintTxtWin(String.format("rx string: [%s]",  rx_str), 3, true);
        				try {
        					
        					((CanvasW) panel_canv).UpdateRTDServer(rx_str);
        					i=3;
        	
            				
            				 //splitting of string at whitespace
                    		String[] splitStr = rx_str.split(" ");
                    	
                    		
                    		// converting string into Double
                    		
                    		mval[i] = Double.parseDouble(splitStr[2]);
                    		mtime[i] = Double.parseDouble(splitStr[1])/1000;
                    		
                    		// allowing only the new timestamp and data to show on the graph
                    	
        					if(latest_time_RTD > mtime[i])
        					{
        						continue;
        					}
        					else
        					{
        						latest_time_RTD = mtime[i];
        					}

                		
                		for (k=0; k<CHART_POINTS-1; k++)
                		{	                			
                			Stime[i][k] = Stime[i][k+1];
                			Sdata[i][k] = Sdata[i][k+1];
                		}
                		
                		Stime[i][CHART_POINTS-1] = mtime[i];
                		Sdata[i][CHART_POINTS-1] = mval[i];
        					
        			
                		S = String.format("S%d", i);
        				
        				Sdata[i][CHART_POINTS-1]= Sdata[i][CHART_POINTS - 1]; 
        				RTD_Chart.updateXYSeries(S, Stime[i], Sdata[i], null);
        				panel_RTD.repaint();
        				((CanvasW) panel_canv).UpdateRTD(mtime,mval,i);
        				panel_canv.repaint();
        				} catch (NumberFormatException e) {PrintTxtWin(e.toString(), 3, true);}
        			}
        			
        			else {
        				PrintTxtWin(String.format("Invalid format rx: [%s]", 
        						    rx_str), 3, true);
        				 }  
        		}
        	
    			}while (!rx_str.isEmpty());
    		}
    	}
    private JPanel panel_canv;
	private JPanel panel_CAN;
	private JPanel panel_RTD;
	private JPanel panel_PB;
	private JPanel panel_SW;
	private JTextPane textPane_log;
    private Timer DispUpdate_Timer;
    private SimpleAttributeSet TextSet = new SimpleAttributeSet();
    private TcpClient TcpOBJ;
	private XYChart CAN_Chart = new XYChartBuilder().width(400).height(180)
			                     .title("CAN-Temperature").xAxisTitle("time(s)")
			                     .yAxisTitle("Data(oC)").build();
	private XYChart RTD_Chart = new XYChartBuilder().width(200).height(90)
	         .title("RTD-Resistance").xAxisTitle("time(s)")
	         .yAxisTitle("Data(Ohm)").build();
	private XYChart PB_Chart = new XYChartBuilder().width(400).height(180)
	            .title("Pushbutton").xAxisTitle("time(s)")
	            .yAxisTitle("Data").build();
	private XYChart SW_Chart = new XYChartBuilder().width(400).height(180)
	            .title("Switch").xAxisTitle("time(s)")
            .yAxisTitle("Data").build();
	private final int CHART_POINTS = 10;
	private int i,  NumSensors = 4;
	private String S;
	private double[][] Sdata = new double [NumSensors][CHART_POINTS];
	private double[][] Stime = new double [NumSensors][CHART_POINTS];
}
