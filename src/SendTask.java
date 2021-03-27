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
    private int windowSize;
    private int seqNo;
    private InetAddress serverAddr;
    DatagramSocket udpSocket;
    private GoBackFtp.Attributes atts;
    public final int MAX_PAYLOAD_SIZE = 1400;


    public SendTask(GoBackFtp.Attributes atts, DatagramSocket udpSocket) throws UnknownHostException {

            this.fileName = atts.getFileName();
            this.windowSize = atts.getWindowSize();
            this.seqNo = atts.getInitSeqNo();
            this.udpSocket = udpSocket;
            this.serverAddr = InetAddress.getByName(atts.getServerName());
            this.atts = atts;


    }



    public void run() {
        byte[] sendBuffer = new byte[MAX_PAYLOAD_SIZE];
        int bytesRead;
        File file = new File(fileName);
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(fileName);
            while ((bytesRead = inputStream.read(sendBuffer)) != -1) {
                if (bytesRead < MAX_PAYLOAD_SIZE) {
                    byte[] smallerData = new byte[bytesRead];
                    System.arraycopy(sendBuffer, 0, smallerData, 0, bytesRead);
                    sendBuffer = smallerData;
                }
                FtpSegment seg = new FtpSegment(seqNo, sendBuffer);
                DatagramPacket pkt = FtpSegment.makePacket(seg, serverAddr, atts.getServerPort());


                while(GoBackFtp.getGbnQ().size() == windowSize){
                    //GoBackFtp.getGbnQ().wait();
                    Thread.yield();
                }

                udpSocket.send(pkt);
                System.out.println("send " + seqNo);
                GoBackFtp.getGbnQ().add(seg);
                if(GoBackFtp.getGbnQ().size() == 1)
                    GoBackFtp.startTimerTask(atts, udpSocket);
                seqNo += 1;


            }

            GoBackFtp.setSendIsDone(true);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
