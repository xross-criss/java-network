package SKJ;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeMap;


public class TransactionalSocket implements ITransactionalSocket {
    private DatagramSocket socket;
    private boolean isTransactionOpened;
    private int localPort;
    private int remotePort;
    private InetAddress localAddress;
    private InetAddress remoteAddress;

    private static final int BEGIN = 13;
    private static final int ABORT = 14;
    private static final int END = 15;
    private static final int DANE = 67;
    private static final int ZADANIE = 78;
    private static final int CLOSE = 99;

    private TreeMap<Integer, byte[]> recieved;
    private int receivedPacketsMax = 0;

    private boolean wyrzucEPrzyCloseT = false;
    private int timeout = 5000;

    private boolean odebrano_pusty = false;

    private HashMap<Integer, DatagramPacket> p;
    private int pCoont;

    private boolean isAborted = false;

    private int datagramLength = 32;

    public TransactionalSocket(InetAddress serverAddress, int serverPort) throws IOException {
        this.socket = new DatagramSocket(0, InetAddress.getByName("127.0.0.1"));
        socket.setSoTimeout(this.timeout);

        this.remotePort = serverPort;
        this.remoteAddress = serverAddress;

        socket.connect(remoteAddress, remotePort);
        try {
            Thread.sleep(75);
        } catch (InterruptedException e) {
        }

        socket.send(makeDatagramPacket(0, 0, new byte[0]));

        socket.disconnect();

        DatagramPacket p = new DatagramPacket(new byte[datagramLength], datagramLength);
        socket.receive(p);
        datagramLength = pobierzAkcje(p.getData());

        this.remoteAddress = p.getAddress();
        this.remotePort = p.getPort();

        socket.connect(remoteAddress, remotePort);
        try {
            Thread.sleep(75);
        } catch (InterruptedException e) {
        }

        socket.send(makeDatagramPacket(0, 0, new byte[0]));

        this.run();
    }

    public TransactionalSocket() {
    }


    public static ITransactionalSocket createTransactionalSocket(int remotePort, InetAddress remoteAddress, int localPort, InetAddress localAddress) throws IOException {
        TransactionalSocket transactionalSocket = new TransactionalSocket();
        transactionalSocket.localPort = localPort;
        transactionalSocket.remotePort = remotePort;
        transactionalSocket.localAddress = localAddress;
        transactionalSocket.remoteAddress = remoteAddress;
        transactionalSocket.socket = new DatagramSocket(transactionalSocket.localPort, transactionalSocket.localAddress);
        transactionalSocket.socket.connect(remoteAddress, remotePort);
        try {
            Thread.sleep(75);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        transactionalSocket.socket.setSoTimeout(transactionalSocket.timeout);
        transactionalSocket.socket.send(transactionalSocket.makeDatagramPacket(transactionalSocket.datagramLength, 0, new byte[0]));
        transactionalSocket.odbierz();
        transactionalSocket.run();

        return transactionalSocket;
    }

    private int pobierzAkcje(byte[] data) {
        return bytesToInteger(Arrays.copyOfRange(data, 0, 4));
    }

    private int pobierzID(byte[] data) {
        return bytesToInteger(Arrays.copyOfRange(data, 4, 8));
    }

    private DatagramPacket odbierz() throws IOException {
        DatagramPacket packet = new DatagramPacket(new byte[datagramLength], datagramLength);
        socket.receive(packet);
        return packet;
    }

    public int getMissingPacketID() {
        int counter = 0;
        for (int current : recieved.keySet()) {
            if (current != counter) {
                return counter;
            }
            counter++;
        }
        if (receivedPacketsMax > counter) {
            return counter;
        }
        return -1;
    }

    private byte[] makeData(int akcja, int idPakietu, byte[] data) {
        byte[] tmp = new byte[0];
        tmp = concat(tmp, integerToBytes(akcja));
        tmp = concat(tmp, integerToBytes(idPakietu));
        tmp = concat(tmp, data);
        return tmp;
    }

    private DatagramPacket makeDatagramPacket(int akcja, int idPakietu, byte[] data) {
        byte[] tmp = makeData(akcja, idPakietu, data);
        return new DatagramPacket(tmp, tmp.length);
    }

    private byte[] concat(byte[] a, byte[] b) {
        int aLen = a.length;
        int bLen = b.length;
        byte[] c = new byte[aLen + bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }

    private byte[] integerToBytes(int x) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.putInt(x);
        return buffer.array();
    }

    private int bytesToInteger(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.put(bytes);
        buffer.flip();
        return buffer.getInt();
    }

    @Override
    public byte[] read() throws IOException {
        byte[] dane = new byte[0];

        while (isTransactionOpened) {
            try {
                Thread.sleep(75);
            } catch (InterruptedException e) {
            }
        }
        for (byte[] data : recieved.values()) {
            dane = concat(dane, data);
        }

        return dane;
    }

    private void run() {
        System.out.println("READY CONN " + socket.getLocalAddress().getHostName() + ":" + socket.getLocalPort() + "->" + socket.getInetAddress().getHostName() + ":" + socket.getPort());

        Thread thread = new Thread(() -> {
            while (!socket.isClosed()) {
                try {
                    DatagramPacket datagram;

                    datagram = new DatagramPacket(new byte[datagramLength], datagramLength);
                    socket.receive(datagram);

                    switch (pobierzAkcje(datagram.getData())) {
                        case ZADANIE:
                            socket.send(p.get(pobierzID(datagram.getData())));
                            break;
                        case CLOSE:
                            socket.close();
                            break;
                        case DANE:
                            recieved.put(pobierzID(datagram.getData()), Arrays.copyOfRange(datagram.getData(), 8, datagram.getData().length));
                            break;
                        case ABORT:
                            socket.send(makeDatagramPacket(0, 0, new byte[0]));
                            recieved = new TreeMap<>();
                            p = new HashMap<>();
                            pCoont = 0;
                            receivedPacketsMax = 0;
                            isAborted = false;
                            afterAbort();
                            break;
                        case END:
                            receivedPacketsMax = pobierzID(datagram.getData());
                            int missingID;
                            if ((missingID = getMissingPacketID()) > 0) {
                                socket.send(makeDatagramPacket(ZADANIE, missingID, new byte[0]));
                                break;
                            }
                            closeT();
                            break;
                        case BEGIN:
                            openTransactionReceived();
                            break;
                        default:
                            odebrano_pusty = true;
                            break;

                    }

                } catch (SocketTimeoutException e) {
                } catch (IOException e) {
                    wyrzucEPrzyCloseT = true;
                }
            }

            System.out.println("Thread stopped");

        }

        );

        thread.start();

    }

    @Override
    public void write(byte[] buff) throws IOException {
        if (!isTransactionOpened) {
            throw new IOException();
        }
        System.out.println("BEGIN WRITE");
        byte[] tmp;
        int packetDataLength = datagramLength - 8;
        int n = (int) Math.ceil(1.0 * buff.length / packetDataLength);
        for (int i = 0; i < n; i++) {
            if (socket.isClosed()) {
                wyrzucEPrzyCloseT = true;
                break;
            }
            tmp = Arrays.copyOfRange(buff, i * packetDataLength, Math.max((i + 1) * packetDataLength, datagramLength));
            DatagramPacket p = new DatagramPacket(tmp, tmp.length);
            this.p.put(pCoont, p);
            socket.send(p);
            pCoont++;
        }

        System.out.println("END WRITE");
    }

    @Override
    public void openTransaction() throws IOException {
        System.out.println("Sending BEGIN");
        try {
            if (!socket.isClosed() || isAborted) {
                socket.send(makeDatagramPacket(BEGIN, 0, new byte[0]));
                while (!odebrano_pusty) {
                    Thread.sleep(75);
                }
            }
        } catch (IOException e) {
            throw new IOException(e.getMessage());
        } catch (InterruptedException e) {
        }
        odebrano_pusty = false;
        recieved = new TreeMap<>();
        p = new HashMap<>();
        pCoont = 0;
        receivedPacketsMax = 0;
        isAborted = false;
        openTransactionPost();
    }

    private void openTransactionReceived() throws IOException {
        socket.send(makeDatagramPacket(0, 0, new byte[0]));
        recieved = new TreeMap<>();
        p = new HashMap<>();
        pCoont = 0;
        receivedPacketsMax = 0;
        isAborted = false;
        openTransactionPost();
    }

    private void openTransactionPost() throws IOException {
        if (isTransactionOpened) {
            throw new IOException("Transaction is already opened");
        }
        if (!socket.isConnected()) {
            socket.connect(remoteAddress, remotePort);
        }
        isAborted = false;
        isTransactionOpened = true;
    }

    @Override
    public void closeTransaction() throws IOException {
        System.out.println("Sending END");
        try {
            if (!socket.isClosed() || isAborted) {
                socket.send(makeDatagramPacket(END, 0, new byte[0]));
                while (!odebrano_pusty) {
                    Thread.sleep(75);
                }
            }
        } catch (IOException e) {
            throw new IOException(e.getMessage());
        } catch (InterruptedException e) {
        }
        odebrano_pusty = false;
        if (!socket.isConnected()) {
            throw new IOException("Socket is not connected");
        }
        if (!isTransactionOpened) {
            throw new IOException("Transaction is already closed");
        }
        isTransactionOpened = false;
        wyrzucEPrzyCloseT = false;
    }

    private void closeT() throws IOException {
        socket.send(makeDatagramPacket(0, 0, new byte[0]));
        if (wyrzucEPrzyCloseT) {
            wyrzucEPrzyCloseT = false;
            throw new IOException("Exception occurred during transaction");
        }
        wyrzucEPrzyCloseT = false;
        if (!isTransactionOpened) {
            throw new IOException("Transaction is already closed");
        }
        if (!socket.isConnected()) {
            throw new IOException("Socket is not connected");
        }
        isTransactionOpened = false;
    }

    @Override
    public void abortTransaction() throws IOException {
        System.out.println("Sending ABORT");
        try {
            if (!socket.isClosed() || isAborted) {
                socket.send(makeDatagramPacket(ABORT, 0, new byte[0]));
                while (!odebrano_pusty) {
                    Thread.sleep(75);
                }
            }
        } catch (IOException e) {
            throw new IOException(e.getMessage());
        } catch (InterruptedException e) {
        }
        odebrano_pusty = false;
        recieved = new TreeMap<>();
        p = new HashMap<>();
        pCoont = 0;
        receivedPacketsMax = 0;
        isAborted = false;
        afterAbort();
    }

    private void afterAbort() throws IOException {
        if (!isTransactionOpened || !socket.isConnected()) {
            throw new IOException();
        }
        isAborted = true;
    }

    @Override
    public void close() throws IOException {
        if (socket == null) {
            throw new IOException();
        }
        if (socket.isClosed()) {
            return;
        }
        socket.send(makeDatagramPacket(CLOSE, 0, new byte[0]));
        socket.close();
    }

    @Override
    public void setTimeout(int readTimeout) throws SocketException {
        this.timeout = readTimeout;
        socket.setSoTimeout(readTimeout);
    }

    @Override
    public int getTimeout() {
        return timeout;
    }

    @Override
    public void setSegmentSize(int size) throws IOException {
        this.datagramLength = size - 8;
    }

    @Override
    public int getSegmentSize() {
        return this.datagramLength;
    }

    @Override
    public int getLocalPort() {
        return this.localPort;
    }

    @Override
    public InetAddress getLocalAddress() {
        return this.localAddress;
    }

    @Override
    public int getRemotePort() {
        return this.remotePort;
    }

    @Override
    public InetAddress getRemoteAddress() {
        return this.remoteAddress;
    }

}
