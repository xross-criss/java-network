package SKJ;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;

public interface IWaitingSocket extends Closeable {

    public ITransactionalSocket listen() throws IOException;

    public int getPort();

    public InetAddress getAddress();

    public void close() throws IOException;

}