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
                if(!GoBackFtp.getGbnQ().isEmpty()){
                    FtpSegment recSeg = new FtpSegment(GoBackFtp.getGbnQ().peek().getSeqNum()+1, receiveBuffer);
                    DatagramPacket pkt = FtpSegment.makePacket(recSeg, serverAddress, atts.getServerPort());
                    udpSocket.receive(pkt);
                    System.out.println("ack\t"+GoBackFtp.getGbnQ().peek().getSeqNum()+1);
                    GoBackFtp.stopTimerTask();
                }
                if(GoBackFtp.getGbnQ().poll() != null){
                    //GoBackFtp.getGbnQ().notify();
                    GoBackFtp.startTimerTask(atts, udpSocket);
                }

            }
        }
        catch(IOException e){
            e.printStackTrace();
        }



    }





}
