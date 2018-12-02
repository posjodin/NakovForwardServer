/**
 * ForwardServerClientThread handles the clients of Nakov Forward Server. It
 * finds suitable server from the server pool, connects to it and starts
 * the TCP forwarding between given client and its assigned server. After
 * the forwarding is failed and the two threads are stopped, closes the sockets.
 */
 
import java.net.Socket;
import java.net.SocketException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
 
public class ForwardServerClientThread extends Thread
{
    private ForwardClient mForwardClient = null;
    private Socket mClientSocket = null;
    private Socket mServerSocket = null;
    private boolean mBothConnectionsAreAlive = false;
    private String mClientHostPort;
    private String mServerHostPort;
    private int mServerPort;
    private String mServerHost;
    /**
     * Creates a client thread for handling clients of NakovForwardServer.
     * A client socket should be connected and passed to this constructor.
     * A server socket is created later by run() method.
     */
    public ForwardServerClientThread(ForwardClient ForwardClient, Socket aClientSocket, String serverhost, int serverport)
    {
        mForwardClient = ForwardClient;
        mClientSocket = aClientSocket;
        mServerPort = serverport;
        mServerHost = serverhost;

    }
 
    /**
     * Obtains a destination server socket to some of the servers in the list.
     * Starts two threads for forwarding : "client in <--> dest server out" and
     * "dest server in <--> client out", waits until one of these threads stop
     * due to read/write failure or connection closure. Closes opened connections.
     */
    public void run()
    {
        try {
           mClientHostPort = mClientSocket.getInetAddress().getHostAddress() + ":" + mClientSocket.getPort();
 
           mServerSocket = new Socket(mServerHost, mServerPort);
 
           // Obtain input and output streams of server and client
           InputStream clientIn = mClientSocket.getInputStream();
           OutputStream clientOut = mClientSocket.getOutputStream();
           InputStream serverIn = mServerSocket.getInputStream();
           OutputStream serverOut = mServerSocket.getOutputStream();
 
           mServerHostPort = mServerHost + ":" + mServerPort;
           mForwardClient.log("TCP Forwarding  " + mServerPort + " <--> " + mServerHostPort + "  started.");
 
           // Start forwarding of socket data between server and client
           ForwardThread clientForward = new ForwardThread(this, clientIn, serverOut);
           ForwardThread serverForward = new ForwardThread(this, serverIn, clientOut);
           mBothConnectionsAreAlive = true;
           clientForward.start();
           serverForward.start();
 
        } catch (IOException ioe) {
           ioe.printStackTrace();
        }
    }
 
    /**
     * connectionBroken() method is called by forwarding child threads to notify
     * this thread (their parent thread) that one of the connections (server or client)
     * is broken (a read/write failure occured). This method disconnects both server
     * and client sockets causing both threads to stop forwarding.
     */
    public synchronized void connectionBroken()
    {
        if (mBothConnectionsAreAlive) {
           // One of the connections is broken. Close the other connection and stop forwarding
           // Closing these socket connections will close their input/output streams
           // and that way will stop the threads that read from these streams
           try { mServerSocket.close(); } catch (IOException e) {}
           try { mClientSocket.close(); } catch (IOException e) {}
 
           mBothConnectionsAreAlive = false;
 
           mForwardClient.log("TCP Forwarding  " + mClientHostPort + " <--> " + mServerHostPort + "  stopped.");
        }
    }
 
}
