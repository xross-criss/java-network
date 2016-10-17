package SerwerTCP;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class serwerTCP {
    public static void main(String[] args) {
        try {
            int portNumber = 4455;

            ServerSocket serverSocket = new ServerSocket(portNumber);

            Socket socket = serverSocket.accept(); // accept dowiedzieć się do czego służy

            //dane wychodzace
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            //dane przychodzące
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                out.println(inputLine + "z serwera");
            }
        } catch (Exception exc) {
            System.out.println(exc.toString());

        }
    }
}
