import javax.swing.text.Segment;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
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
    private ConcurrentLinkedQueue<DatagramPacket> gbnQ;


    /**
     * Constructor to resend timer
     *
     * @param gbnQ_	Hold a copy of the packet to send to avoid race conditions
     * @param seq_	Sequence number of the packet to resend
     * @param udpSocket_ the UDP socket used to send the packets
     */
    public ResendTask(ConcurrentLinkedQueue<DatagramPacket> gbnQ_, DatagramSocket udpSocket_, int seq_){
        this.gbnQ = gbnQ_;
        udpSocket = udpSocket_;
        seq = seq_;
    }

    @Override
    public void run() {
        System.out.println("timeout\t");
        try {
            for (DatagramPacket pkt : gbnQ){
                udpSocket.send(pkt);
                System.out.println("retx\t" + seq);
                seq++;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}