import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;


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
            while ((bytesRead = inputStream.read(sendBuffer)) != -1) {                                                  /*Read from the file*/
                if (bytesRead < MAX_PAYLOAD_SIZE) {                                                                     /*If it is the last segment, hence less than 1400bytes*/
                    byte[] smallerData = new byte[bytesRead];
                    System.arraycopy(sendBuffer, 0, smallerData, 0, bytesRead);
                    sendBuffer = smallerData;
                }
                FtpSegment seg = new FtpSegment(seqNo, sendBuffer);
                DatagramPacket pkt = FtpSegment.makePacket(seg, serverAddr, atts.getServerPort());


                while(GoBackFtp.getGbnQ().size() == windowSize)                                                         /*While the queue is full, wait*/
                    Thread.yield();


                udpSocket.send(pkt);
                System.out.println("send " + seqNo);
                GoBackFtp.getGbnQ().add(seg);                                                                           /*Add the sent segment to the queue*/
                if(GoBackFtp.getGbnQ().size() == 1)                                                                     /*If it's the first segment, start the timer*/
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
