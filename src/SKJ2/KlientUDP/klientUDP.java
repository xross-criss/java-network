package SKJ2.KlientUDP;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class klientUDP {
    public static void main(String[] args) {
        try{
            String str = "hello";

            DatagramSocket socket = new DatagramSocket();

            byte[] buf = new byte[256];
            buf = str.getBytes("UTF-8");

            InetAddress adress = InetAddress.getByName("localhost");

            DatagramPacket packet = new DatagramPacket(buf, buf.length, adress, 4456);

            socket.send(packet);
            //==============================================

            packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);

            String receive = new String(packet.getData(), 0, packet.getLength());
            System.out.println(receive);
            socket.close();

        } catch (Exception exc){
            System.out.println("błąd - klient -> " + exc);
        }
    }
}