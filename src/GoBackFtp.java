
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

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.*;



public class GoBackFtp {

	private static long rtoTimer;
	private static Timer timer;
	private static ResendTask resendTask;
	private static ConcurrentLinkedQueue<FtpSegment> gbnQ;

	public long getFileLen() {
		return fileLen;
	}

	public void setFileLen(long fileLen) {
		this.fileLen = fileLen;
	}


	public void setServerUdpPort(int serverUdpPort) {
		this.serverUdpPort = serverUdpPort;
	}

	public int getInitSeqNo() {
		return initSeqNo;
	}

	public void setInitSeqNo(int initSeqNo) {
		this.initSeqNo = initSeqNo;
	}


	public static ConcurrentLinkedQueue<FtpSegment> getGbnQ() {
		return gbnQ;
	}


	public DatagramSocket getUdpSocket() {
		return udpSocket;
	}


	public int getWindowSize() {
		return windowSize;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}


	private int initSeqNo;
	private int currentSeqNo;
	public final static int MAX_PAYLOAD_SIZE = 1400; // bytes
	private DatagramSocket udpSocket;
	private int windowSize;
	// global logger
	private static final Logger logger = Logger.getLogger("GoBackFtp");
	private long fileLen;
	private int serverUdpPort;
	private String serverName;
	private static volatile boolean sendIsDone = false;

	public static boolean isSendIsDone() {
		return sendIsDone;
	}

	public static void setSendIsDone(boolean sendIsDone) {
		GoBackFtp.sendIsDone = sendIsDone;
	}


	/**
	 * Constructor to initialize the program
	 *
	 * @param windowSize	Size of the window for Go-Back_N in units of segments
	 * @param rtoTimer		The time-out interval for the retransmission timer
	 */
	public GoBackFtp(int windowSize, int rtoTimer){
		this.windowSize = windowSize;
		this.rtoTimer = rtoTimer;
		this.timer = new Timer();
		try{
			udpSocket = new DatagramSocket();
			udpSocket.setSoTimeout(2000);

		}catch(IOException e){
			e.printStackTrace();
		}

	}


	public class Attributes {
		private String serverName;
		private int serverPort;

		public String getServerName() {
			return serverName;
		}


		public int getServerPort() {
			return serverPort;
		}


		public String getFileName() {
			return fileName;
		}



		public int getWindowSize() {
			return windowSize;
		}



		public int getInitSeqNo() {
			return initSeqNo;
		}


		private String fileName;
		private int windowSize;
		private int initSeqNo;

		public Attributes(String serverName, int serverPort, String fileName, int windowSize, int initSeqNo) {
			this.serverName = serverName;
			this.serverPort = serverPort;
			this.fileName = fileName;
			this.windowSize = windowSize;
			this.initSeqNo = initSeqNo;
		}

	}


	public static synchronized void startTimerTask(){
		timer.scheduleAtFixedRate(resendTask, rtoTimer, rtoTimer);
	}
	public static synchronized void stopTimerTask(){
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
	public void send(String serverName, int serverPort, String fileName) throws FtpException {
		try{
			setServerName(serverName);
			handshake(serverName, serverPort, fileName, udpSocket.getLocalPort());
			Attributes atts = new Attributes(serverName, serverPort, fileName, getWindowSize(), getInitSeqNo());
			resendTask = new ResendTask(atts, getUdpSocket());

			SendTask sendTask = new SendTask(atts, getUdpSocket());
			Thread sendThread = new Thread(sendTask);
			sendThread.start();
			ReceiveTask receiveTask = new ReceiveTask(atts, getUdpSocket());
			Thread receiveThread = new Thread(receiveTask);
			receiveThread.start();
		}catch(Exception e){
			throw new FtpException(e.getMessage());
		}

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