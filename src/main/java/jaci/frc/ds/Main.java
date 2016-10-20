package jaci.frc.ds;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.net.*;

public class Main {

    public static byte[] toBytes(char[] data) {
        byte[] toRet = new byte[data.length];
        for(int i = 0; i < toRet.length; i++) {
            toRet[i] = (byte) data[i];
        }
        return toRet;
    }

    public static void initSystemTray() throws AWTException, IOException {
        if (!SystemTray.isSupported()) {
            System.exit(0);
        }
        final PopupMenu popup = new PopupMenu();
        final TrayIcon trayIcon = new TrayIcon(ImageIO.read(ClassLoader.getSystemResource("rio.png")), "RoboRIO-Tunnel");
        trayIcon.setImageAutoSize(true);
        final SystemTray tray = SystemTray.getSystemTray();
        MenuItem text = new MenuItem("RoboRIO Tunnel");
        MenuItem exit = new MenuItem("Exit");
        exit.addActionListener((actionEvent) -> { System.exit(0); });
        popup.add(text);
        popup.addSeparator();
        popup.add(exit);
        trayIcon.setPopupMenu(popup);
        tray.add(trayIcon);
    }

    public static void main(String[] args) throws IOException, InterruptedException, AWTException {
        String service = "roborio-9889-frc";
        String hostname = "roborio-9889-frc";
        String ip_str = "172.22.11.2";

        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("team") && i != args.length - 1) {
                String fullname = "roborio-" + args[i + 1] + "-frc";
                service = fullname;
                hostname = fullname;
            }
            if (args[i].equalsIgnoreCase("ip") && i != args.length - 1) {
                ip_str = args[i];
            }
        }

        char ip[] = new char[4];
        String arr[] = ip_str.split("\\.");
        for (int i = 0; i < 4; i++) {
            ip[i] = (char)(int)Integer.parseInt(arr[i]);
        }

        char payload_1[] = {
            0x00, 0x00, 0x84, 0x00,		// ID, Response Query
            0x00, 0x00, 0x00, 0x03,		// No Question, 3 Answers
            0x00, 0x00, 0x00, 0x01,		// No Authority, 1 Additional RR
        };

        char payload_2[] = {
            0x03,							// Len: 3
            0x5f, 0x6e, 0x69,				// _ni
            0x04,							// Len: 4
            0x5f, 0x74, 0x63, 0x70,			// _tcp
            0x05,							// Len: 5
            0x6c, 0x6f, 0x63, 0x61, 0x6c,	// local
            0x00,							// end of string

            0x00, 0x0c, 0x80, 0x01,			// Type: PTR (domain name PoinTeR), Class: IN, Cache flush: true
            0x00, 0x00, 0x00, 0x3C,			// TTL: 60 Sec
            0x00, (char)(0x03 + service.length()),
            (char)service.length()
        };

        char payload_3[] = service.toCharArray();

        char payload_4[] = {
            0xc0, 0x0c,					// Name Offset (0xc0, 0x0c => 12 =>._ni._tcp.local)

            // Record 2: SRV
            0xc0, 0x26, 0x00, 0x21,		// Name Offset (mdns.service_name), Type: SRV (Server Selection)
            0x80, 0x01,					// Class: IN, Cache flush: true
            0x00, 0x00, 0x00, 0x3C,		// TTL: 60 sec
            0x00, (char)(0xE + hostname.length()),	// Data Length: 14 + thnl
            0x00, 0x00, 0x00, 0x00,		// Priority: 0, Weight: 0
            0x0d, 0xfc,					// Port: 3580
            (char)hostname.length()					// Len: thnl
        };

        char payload_5[] = hostname.toCharArray();

        char payload_6[] = {
            0x05,							// Len: 5
            0x6c, 0x6f, 0x63, 0x61, 0x6c,	// local
            0x00,							// end of string

            // Record 3: TXT
            0xc0, 0x26, 0x00, 0x10,		// Name Offset (mdns.service_name), Type: TXT
            0x80, 0x01,					// Class: IN, Cache flush: true
            0x00, 0x00, 0x00, 0x3C,		// TTL: 60 sec
            0x00, 0x01, 0x00,			// Data Length: 1, TXT Length: 0

            // Additional Record: A
            0xc0, (char)(0x3b + service.length()),	// Name Offset (mdns.target_host_name)
            0x00, 0x01, 0x80, 0x01,		// Type: A, Class: IN, Cache flush: true
            0x00, 0x00, 0x00, 0x3C,		// TTL: 60 sec
            0x00, 0x04,					// Data Length: 4
            ip[0], ip[1], ip[2], ip[3]	// IP Bytes
        };

        char payload[] = new char[payload_1.length + payload_2.length + payload_3.length + payload_4.length + payload_5.length + payload_6.length];
        System.arraycopy(payload_1, 0, payload, 0, payload_1.length);
        int l = payload_1.length;
        System.arraycopy(payload_2, 0, payload, l, payload_2.length);
        l += payload_2.length;
        System.arraycopy(payload_3, 0, payload, l, payload_3.length);
        l += payload_3.length;
        System.arraycopy(payload_4, 0, payload, l, payload_4.length);
        l += payload_4.length;
        System.arraycopy(payload_5, 0, payload, l, payload_5.length);
        l += payload_5.length;
        System.arraycopy(payload_6, 0, payload, l, payload_6.length);

        InetAddress group = InetAddress.getByName("224.0.0.251");
        MulticastSocket multicast_socket = new MulticastSocket(5353);
        multicast_socket.joinGroup(group);

        byte[] byte_payload = toBytes(payload);
        System.out.println("Connected to Multicast! Broadcasting fake RoboRIO....");
        initSystemTray();
        while (true) {
            DatagramPacket packet = new DatagramPacket(byte_payload, byte_payload.length, group, 5353);
            multicast_socket.send(packet);
            Thread.sleep(5000);
        }
    }
}
