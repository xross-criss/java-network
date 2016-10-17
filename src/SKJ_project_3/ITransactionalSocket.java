package SKJ_project_3;


import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;

public interface ITransactionalSocket extends Closeable {
    public byte[] read() throws IOException;

    public void write(byte[] buff) throws IOException;

    public void openTransaction() throws IOException;

    public void closeTransaction() throws IOException;

    public void abortTransaction() throws IOException;

    public void close() throws IOException;

    public void setTimeout(int readTimeout) throws SocketException;

    public int getTimeout();

    public void setSegmentSize(int size) throws IOException;

    public int getSegmentSize();

    public int getLocalPort();

    public InetAddress getLocalAddress();

    public int getRemotePort();

    public InetAddress getRemoteAddress();

}