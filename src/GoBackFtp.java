
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

	public long getFileLen() {
		return fileLen;
	}

	public void setFileLen(long fileLen) {
		this.fileLen = fileLen;
	}

	public int getServerUdpPort() {
		return serverUdpPort;
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

	public static int getMaxPayloadSize() {
		return MAX_PAYLOAD_SIZE;
	}

	public ConcurrentLinkedQueue getGbnQ() {
		return gbnQ;
	}

	public void setGbnQ(ConcurrentLinkedQueue gbnQ) {
		this.gbnQ = gbnQ;
	}

	public Timer getTimer() {
		return timer;
	}

	public void setTimer(Timer timer) {
		this.timer = timer;
	}

	public DatagramSocket getUdpSocket() {
		return udpSocket;
	}

	public void setUdpSocket(DatagramSocket udpSocket) {
		this.udpSocket = udpSocket;
	}

	public int getRtoTimer() {
		return rtoTimer;
	}

	public void setRtoTimer(int rtoTimer) {
		this.rtoTimer = rtoTimer;
	}

	public int getWindowSize() {
		return windowSize;
	}

	public void setWindowSize(int windowSize) {
		this.windowSize = windowSize;
	}
	public int getCurrentSeqNo() {
		return currentSeqNo;
	}

	public void setCurrentSeqNo(int currentSeqNo) {
		this.currentSeqNo = currentSeqNo;
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}


	private int initSeqNo;
	private int currentSeqNo;
	public final static int MAX_PAYLOAD_SIZE = 1400; // bytes
	private ConcurrentLinkedQueue<DatagramPacket> gbnQ;
	private Timer timer;
	DatagramSocket udpSocket;
	private int rtoTimer;
	private int windowSize;
	// global logger
	private static final Logger logger = Logger.getLogger("GoBackFtp");
	private long fileLen;
	private int serverUdpPort;
	private String serverName;

	/**
	 * Constructor to initialize the program
	 *
	 * @param windowSize	Size of the window for Go-Back_N in units of segments
	 * @param rtoTimer		The time-out interval for the retransmission timer
	 */
	public GoBackFtp(int windowSize, int rtoTimer){
		this.windowSize = windowSize;
		this.rtoTimer = rtoTimer;
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
	public void send(String serverName, int serverPort, String fileName) throws FtpException, IOException {
		setServerName(serverName);
		handshake(serverName, serverPort, fileName, udpSocket.getLocalPort());
		int bufferSize = getWindowSize()*MAX_PAYLOAD_SIZE;
		byte[] sendBuffer = new byte[bufferSize];
		byte[] receiveBuffer = new byte[bufferSize];
		int bytesRead;
		setCurrentSeqNo(getInitSeqNo());
		File file = new File(fileName);
		FileInputStream inputStream = new FileInputStream(fileName);
		while((bytesRead = inputStream.read(sendBuffer)) != -1){
			if (bytesRead < bufferSize){
				byte[] smallerData = new byte[bytesRead];
				System.arraycopy(sendBuffer, 0, smallerData, 0, bytesRead);
				sendBuffer = smallerData;
			}
			gbnQ = makeQ(sendBuffer, getCurrentSeqNo());
			SendTask sendTask = new SendTask(gbnQ);
			ResendTask resendTask = new ResendTask(gbnQ, udpSocket, getCurrentSeqNo());
			startTimerTask(resendTask);
			ReceiveTask receiveTask = new ReceiveTask(gbnQ);
			cancelTimerTask(resendTask);




		}



	}

	public ConcurrentLinkedQueue<DatagramPacket> makeQ(byte[] sendBuffer, int initSeqNo) throws UnknownHostException {
		ConcurrentLinkedQueue<DatagramPacket> q = new ConcurrentLinkedQueue();
		int startIdx = 0;
		int len = sendBuffer.length;
		byte[] packetBuffer;
		int seqNo = initSeqNo;

		while(len > 0){
			if(len < MAX_PAYLOAD_SIZE)
				packetBuffer = new byte[len];
			else
				packetBuffer = new byte[MAX_PAYLOAD_SIZE];

			ByteBuffer bb = ByteBuffer.wrap(sendBuffer);
			bb.get(packetBuffer, startIdx, sendBuffer.length);
			startIdx += (MAX_PAYLOAD_SIZE + 1);
			FtpSegment seg = new FtpSegment(seqNo, packetBuffer);
			DatagramPacket pkt = FtpSegment.makePacket(seg, InetAddress.getByName(getServerName()), getServerUdpPort());
			q.add(pkt);
			seqNo += 1;

		}
		setCurrentSeqNo(seqNo);
		return q;
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