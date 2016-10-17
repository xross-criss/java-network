package SKJ;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class WaitingSocket implements IWaitingSocket {

    private InetAddress serverAdres;
    private boolean czySlucha;
    private int serverPort;
    private DatagramSocket socket;

    @Override
    public ITransactionalSocket listen() throws IOException {
        czySlucha = true;
        if (socket.isClosed()) {
            czySlucha = false;
            throw new IOException();
        }
        byte[] buff = new byte[1024];
        DatagramPacket pakiet = new DatagramPacket(buff, buff.length);
        this.socket.receive(pakiet);
        czySlucha = false;
        return TransactionalSocket.createTransactionalSocket(pakiet.getPort(), pakiet.getAddress(), 0, socket.getLocalAddress());
    }

    @Override
    public int getPort() {
        return this.serverPort;
    }

    @Override
    public InetAddress getAddress() {
        return this.serverAdres;
    }

    @Override
    public void close() throws IOException {
        if (czySlucha) {
            czySlucha = false;
            throw new IOException();
        }
        socket.close();
        czySlucha = false;
    }

    public WaitingSocket(InetAddress serverAdres, int serverPort) throws SocketException {
        this.serverPort = serverPort;
        this.serverAdres = serverAdres;
        socket = new DatagramSocket(serverPort, serverAdres);
    }

}
