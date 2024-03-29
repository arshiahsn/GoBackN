import javax.swing.text.Segment;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Resend Timer class that extends the TimerTask
 * The purpose of this class is to resend the packets
 * if the ack is not received after timeout happens
 *
 */
public class ResendTask extends TimerTask {

    private DatagramSocket udpSocket;
    private int seq;
    private GoBackFtp.Attributes atts;
    public final int MAX_PAYLOAD_SIZE = 1400;

    /**
     * Constructor to resend timer
     *
     * @param udpSocket_ the UDP socket used to send the packets
     */
    public ResendTask(GoBackFtp.Attributes atts, DatagramSocket udpSocket_){
        udpSocket = udpSocket_;
        this.atts = atts;
    }

    @Override
    public void run() {
        System.out.println("timeout\t");
        try {
            for (FtpSegment seg : GoBackFtp.getGbnQ()){                                                                 /*Iterate the queue and resend all segments*/
                DatagramPacket pkt = FtpSegment.makePacket(seg, InetAddress.getByName(atts.getServerName()),            
                        atts.getServerPort());
                udpSocket.send(pkt);
                System.out.println("retx\t" + seg.getSeqNum());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}