import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ReceiveTask implements Runnable {

    private GoBackFtp.Attributes atts;
    private DatagramSocket udpSocket;
    public final int MAX_PAYLOAD_SIZE = 1400;
    private InetAddress serverAddress;

    public ReceiveTask(GoBackFtp.Attributes atts, DatagramSocket udpSocket) throws UnknownHostException {
        this.atts = atts;
        this.udpSocket = udpSocket;
        this.serverAddress = InetAddress.getByName(atts.getServerName());

    }

    public void run(){
        byte[] receiveBuffer;
        try{
            while(!GoBackFtp.getGbnQ().isEmpty() || !GoBackFtp.isSendIsDone()){
                receiveBuffer = new byte[MAX_PAYLOAD_SIZE];
                FtpSegment recSeg = new FtpSegment(GoBackFtp.getGbnQ().peek().getSeqNum(), receiveBuffer);
                DatagramPacket pkt = FtpSegment.makePacket(recSeg, this.serverAddress, atts.getServerPort());
                udpSocket.receive(pkt);
                GoBackFtp.stopTimerTask();
                if(GoBackFtp.getGbnQ().poll() != null)
                    GoBackFtp.startTimerTask();
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }



    }





}
