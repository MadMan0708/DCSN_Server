/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.bc.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.logging.Level;

/**
 * Class which waits for incoming broadcast client's connection requests and
 * answers them
 *
 * @author Jakub Hava
 */
public class DiscoveryThread extends Thread {

    private int port;
    private boolean discovering = true;
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(Server.class.getName());

    /**
     * Constructor
     *
     * @param port port on which server is running
     */
    public DiscoveryThread(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try {
            DatagramSocket socket = new DatagramSocket(port, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);
            while (discovering) {
                LOG.log(Level.INFO, "Ready to receive broadcast packets.");
                byte[] buffer = new byte[15000];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                LOG.log(Level.INFO, "Discovery packet received from: {0}", packet.getAddress().getHostAddress());
                LOG.log(Level.INFO, "Packet received; data: {0}", new String(packet.getData()));

                //Check if it's broadcast server dicovery packet
                String message = new String(packet.getData()).trim();
                if (message.equals("DISCOVER_SERVER_REQUEST")) {
                    byte[] sendData = "DISCOVER_SERVER_RESPONSE".getBytes();
                    //Send a response
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
                    socket.send(sendPacket);
                    LOG.log(Level.INFO, "Sent packet to: {0}", sendPacket.getAddress().getHostAddress());
                }
            }
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Stops listening for incoming broadcast packets
     */
    public void stopDiscovering() {
        discovering = false;
    }

    /**
     * Starts listening for incoming broadcast packets
     */
    public void startDiscovering() {
        discovering = true;
        this.start();
    }
}
