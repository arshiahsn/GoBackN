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
            //While the Queue is not empty or the send thread is not done, do:
            while(!GoBackFtp.getGbnQ().isEmpty() || !GoBackFtp.isSendIsDone()){
                receiveBuffer = new byte[MAX_PAYLOAD_SIZE];
                //If the queue is not empty
                if(!GoBackFtp.getGbnQ().isEmpty()){
                    FtpSegment recSeg = new FtpSegment(GoBackFtp.getGbnQ().peek().getSeqNum()+1,
                            receiveBuffer);                                                                             /*Create a segment with SeqNo+1 of the segment at head of the queue*/
                    DatagramPacket pkt = FtpSegment.makePacket(recSeg, serverAddress, atts.getServerPort());
                    udpSocket.receive(pkt);                                                                             /*Receive the packet*/
                    System.out.println("ack\t"+GoBackFtp.getGbnQ().peek().getSeqNum()+1);
                    GoBackFtp.stopTimerTask();                                                                          /*Cancel the timer if the ack is valid*/
                }
                if(GoBackFtp.getGbnQ().poll() != null)                                                                  /*Remove the acked segment from the queue*/
                    GoBackFtp.startTimerTask(atts, udpSocket);                                                          /*If the queue is not empty, start the timer*/


            }
        }
        catch(IOException e){
            e.printStackTrace();
        }



    }





}
