package SerwerUDP;
//sprobowac stworzyc serwer HTTP
//może proxy
//albo z poprzednich lat na osobnych wątkach :)
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class serwerUDP {
    public static void main(String[] args) {
        try {

            boolean condition = true;
            DatagramSocket socket = new DatagramSocket(4456);

            while (condition){
                byte[] buf = new byte[256];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);

                socket.receive(packet);
                buf = packet.getData();

                InetAddress address = packet.getAddress();
                int port = packet.getPort();

                packet = new DatagramPacket(buf, buf.length, address, port);
                socket.send(packet);
            }

        } catch (Exception exc) {
            System.out.println("bład - serwer -> " + exc);
        }
    }
}