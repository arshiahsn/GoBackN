import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SendTask implements Runnable {
    private String fileName;
    private int bufferSize;
    private ConcurrentLinkedQueue<DatagramPacket> gbnQ;
    private int windowSize;
    private int seqNo;
    private int serverPort;
    private InetAddress serverAddr;
    private int rtoTimer;
    DatagramSocket udpSocket;
    Timer timer;


    public SendTask(ConcurrentLinkedQueue<DatagramPacket> gbnQ, GoBackFtp.Attributes atts, DatagramSocket udpSocket, int rtoTimer) throws UnknownHostException {
        this.fileName = atts.getFileName();
        this.bufferSize = 1400;
        this.gbnQ = gbnQ;
        this.windowSize = atts.getWindowSize();
        this.seqNo = atts.getInitSeqNo();
        this.serverAddr = InetAddress.getByName(atts.getServerName());
        this.serverPort = atts.getServerPort();
        this.udpSocket = udpSocket;
        timer = new Timer();
        this.rtoTimer = rtoTimer;
    }

    public synchronized void startTimerTask(ResendTask resendTask){
        timer.scheduleAtFixedRate(resendTask, rtoTimer, rtoTimer);
    }


    public void run() {
        byte[] sendBuffer = new byte[bufferSize];
        int bytesRead;
        File file = new File(fileName);
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(fileName);
            while ((bytesRead = inputStream.read(sendBuffer)) != -1) {
                if (bytesRead < bufferSize) {
                    byte[] smallerData = new byte[bytesRead];
                    System.arraycopy(sendBuffer, 0, smallerData, 0, bytesRead);
                    sendBuffer = smallerData;
                }
                while(gbnQ.size() == windowSize)
                    Thread.yield();

                FtpSegment seg = new FtpSegment(seqNo, sendBuffer);
                DatagramPacket pkt = FtpSegment.makePacket(seg, serverAddr, serverPort);
                udpSocket.send(pkt);
                System.out.println("send " + seqNo);
                gbnQ.add(pkt);
                if(gbnQ.size() == 1)
                    startTimerTask(new ResendTask(gbnQ, seqNo, udpSocket));
                seqNo += 1;


            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
