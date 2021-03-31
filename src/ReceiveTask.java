import java.awt.*;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ReceiveTask implements Runnable {

    private GoBackFtp.Attributes atts;
    private DatagramSocket udpSocket;
    public final int MAX_PAYLOAD_SIZE = 1400;
    private InetAddress serverAddress;

    public ReceiveTask(GoBackFtp.Attributes atts, DatagramSocket udpSocket)  throws FtpException{
        try{
            this.atts = atts;
            this.udpSocket = udpSocket;
            this.serverAddress = InetAddress.getByName(atts.getServerName());

        }
        catch(Exception e){
            throw new FtpException(e.getMessage());
        }

    }

    public void run() {
        byte[] receiveBuffer;
        try{
            //While the Queue is not empty or the send thread is not done, do:
            while(!GoBackFtp.getGbnQ().isEmpty() || !GoBackFtp.isSendIsDone()){
                receiveBuffer = new byte[MAX_PAYLOAD_SIZE];
                //If the queue is not empty
                if(!GoBackFtp.getGbnQ().isEmpty()){
                                                                          /*Create a segment with SeqNo+1 of the segment at head of the queue*/
                    DatagramPacket pkt = new DatagramPacket(receiveBuffer, MAX_PAYLOAD_SIZE, serverAddress, atts.getServerPort());
                    udpSocket.receive(pkt);                                                                             /*Receive the packet*/
                    int recAckSeq = new FtpSegment(pkt).getSeqNum();
                    System.out.println("ack\t"+recAckSeq);
                    if(GoBackFtp.getGbnQ().peek().getSeqNum() <= recAckSeq-1){
                        while (GoBackFtp.getGbnQ().peek().getSeqNum() <= recAckSeq-1)
                            GoBackFtp.getGbnQ().poll();
                        GoBackFtp.stopTimerTask();
                        if(!GoBackFtp.getGbnQ().isEmpty())
                            GoBackFtp.startTimerTask(atts, udpSocket);
                    }


                }
            }





    }
        catch(Exception e){
            //Timeout
            //End of the queue
            //Log

        }



    }





}
