
/**
 * GoBackFtp Class
 * 
 * GoBackFtp implements a basic FTP application based on UDP data transmission.
 * It implements a Go-Back-N protocol. The window size is an input parameter.
 * 
 * @author 	Majid Ghaderi
 * @version	2021
 *
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.*;



public class GoBackFtp {


	/**
	 * Resend Timer class that extends the TimerTask
	 * The purpose of this class is to resend the packets
	 * if the ack is not received after timeout happens
	 *
	 */
	public class ResendTask extends TimerTask {

		private DatagramPacket pkt;
		private DatagramSocket udpSocket;
		private int seq;
		private ConcurrentLinkedQueue gbnQ;


		/**
		 * Constructor to resend timer
		 *
		 * @param gbnQ	Hold a copy of the packet to send to avoid race conditions
		 * @param seq_	Sequence number of the packet to resend
		 * @param udpSocket_ the UDP socket used to send the packets
		 */
		public ResendTask(ConcurrentLinkedQueue gbnQ, DatagramSocket udpSocket_, int seq_){
			this.gbnQ = gbnQ;
			udpSocket = udpSocket_;
			seq = seq_;
		}

		@Override
		public void run() {
			System.out.println("timeout\t");
			try {
				udpSocket.send(pkt);
				System.out.println("retx\t" + seq);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}





	// global logger
	private static final Logger logger = Logger.getLogger("GoBackFtp");
	private long fileLen;
	private int serverUdpPort;
	private int initSeqNo;
	public final static int MAX_PAYLOAD_SIZE = 1400; // bytes
	private ConcurrentLinkedQueue gbnQ;
	private Timer timer;
	DatagramSocket udpSocket;
	private int rtoTimer;
	private int windowSize;

	/**
	 * Constructor to initialize the program
	 *
	 * @param windowSize	Size of the window for Go-Back_N in units of segments
	 * @param rtoTimer		The time-out interval for the retransmission timer
	 */
	public GoBackFtp(int windowSize, int rtoTimer){
		this.windowSize = windowSize;
		this.rtoTimer = rtoTimer;
		gbnQ = new ConcurrentLinkedQueue();
		timer = new Timer();
		try{
			udpSocket = new DatagramSocket();

		}catch(IOException e){
			e.printStackTrace();
		}

	}


	public synchronized void startTimerTask(ResendTask resendTask){
		timer.scheduleAtFixedRate(resendTask, rtoTimer, rtoTimer);
	}

	public synchronized void cancelTimerTask(ResendTask resendTask){
		resendTask.cancel();
	}





	/**
	 * Send the specified file to the specified remote server
	 * 
	 * @param serverName	Name of the remote server
	 * @param serverPort	Port number of the remote server
	 * @param fileName		Name of the file to be trasferred to the rmeote server
	 * @throws FtpException If anything goes wrong while sending the file
	 */
	public void send(String serverName, int serverPort, String fileName) throws FtpException{

		handshake(serverName, serverPort, fileName, udpSocket.getLocalPort());



	}




	/*
	 *	1. Send the name of the file as a UTF encoded string
	 *	2. Send the length (in bytes) of the file as a long value
	 *	3. Send the local UDP port number used for file transfer as an int value
	 *	4. Receive the server UDP port number used for file transfer as an int value
	 *  5. Receive the initial sequence number used by the server as an int value
	 */
	public void handshake(String serverName, int serverPort, String fileName, int udpPort){
		try{
			Socket tcpSocket = new Socket(serverName, serverPort);
			DataOutputStream tcpOutput = new DataOutputStream(tcpSocket.getOutputStream());
			DataInputStream tcpInput = new DataInputStream(tcpSocket.getInputStream());
			//Send the UTF encoded file name
			tcpOutput.writeUTF(fileName);
			//Send the file length
			File file = new File(fileName);
			setFileLen(file.length());
			tcpOutput.writeLong(getFileLen());
			//Send the local UDP port number
			tcpOutput.writeInt(udpPort);
			//Receive the server Port
			setServerUdpPort(tcpInput.readInt());
			//Receive the seq number
			setInitSeqNo(tcpInput.readInt());
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
} // end of class