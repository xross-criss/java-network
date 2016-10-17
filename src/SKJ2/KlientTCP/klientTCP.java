package SKJ2.KlientTCP;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class klientTCP {
    public static void main(String[] args) {
        try {

            //System.out.println(" hej");
            String hostName = "localhost";
            int portNumber = 4455;

            //tworze gniazdo
            Socket echoSocket = new Socket(hostName, portNumber);

            //Dane wychodzące
            PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);

            //Odczytywanie danie
            BufferedReader in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));

            //Odczytywanie danych z konsoli
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

            String userInput;
            while ((userInput = stdIn.readLine()) != null) {
                out.println(userInput);
                System.out.println("echo: " + in.readLine());
            }
        } catch (Exception exc){
            System.out.println(exc.toString());
        }

    }
}
