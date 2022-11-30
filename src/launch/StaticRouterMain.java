package launch;

import data.address.IPAddress;
import layer.application.GUILayer;
import layer.application.RouterApp;
import layer.application.SettingApp;
import layer.controller.LayerManager;
import layer.internet.ARPLayer;
import layer.internet.IPLayer;
import layer.link.EthernetLayer;
import layer.link.NILayer;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Scanner;

public class StaticRouterMain {

    public static final String NETWORK_INTERFACE = "NI";
    public static final String ETHERNET = "Ethernet";
    public static final String ARP = "ARP";
    public static final String IP = "IP";
    public static final String ROUTER = "Router";
    public static final String SETTING = "Setting";

    private static void setLookAndFeel() {
        // Look & Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); // System L&F
        } catch (Exception exc) {
            System.err.println("Error loading L&F: " + exc);
        }

        // Font Register
        try {
            String fontName = "Cascadia Code";
            String fontFileName = "fonts/" + fontName.replaceAll(" ", "") + ".ttf";
            GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
            File fontFile = new File(fontFileName);
            if (!fontFile.exists()) {
                try (InputStream inputStream = StaticRouterMain.class.getClassLoader().getResourceAsStream(fontFileName)) {
                    if (inputStream != null) {
                        fontFile.mkdirs();
                        Files.copy(inputStream, fontFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }

            graphicsEnvironment.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File(fontFileName)));
            Enumeration<Object> keys = UIManager.getDefaults().keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                Object value = UIManager.get(key);
                if (value instanceof FontUIResource)
                    UIManager.put(key, new FontUIResource(fontName, ((FontUIResource) value).isBold() ? Font.BOLD : Font.PLAIN, ((FontUIResource) value).getSize()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FontFormatException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        setLookAndFeel();

        LayerManager manager = LayerManager.getInstance();

        // Link Layer
        NILayer niLayer0 = new NILayer(NETWORK_INTERFACE, 0);
        NILayer niLayer1 = new NILayer(NETWORK_INTERFACE, 1);
        manager.put(niLayer0, 0);
        manager.put(niLayer1, 1);
        EthernetLayer ethernetLayer = new EthernetLayer(ETHERNET);
        manager.put(ethernetLayer);

        // Internet Layer
        ARPLayer arpLayer = new ARPLayer(ARP);
        manager.put(arpLayer);
        IPLayer ipLayer = new IPLayer(IP);
        manager.put(ipLayer);

        // Application Layer (L3, Router)
        GUILayer routerApp = new RouterApp(ROUTER);
        manager.put(routerApp);
        GUILayer settingApp0 = new SettingApp(SETTING, 0);
        GUILayer settingApp1 = new SettingApp(SETTING, 1);
        manager.put(settingApp0, 0);
        manager.put(settingApp1, 1);

        // Physical <-> Data Link
        for (NILayer niLayer : Arrays.asList(niLayer0, niLayer1)) {
            manager.connectLayers(String.format("%s ( +%s )", niLayer, ethernetLayer));
        }

        // Data Link <-> Network Layer <-> L3 Application Layer
        manager.connectLayers(String.format("%s ( +%s +%s ( -%s +%s ) )", ethernetLayer, arpLayer, ipLayer, arpLayer, routerApp));

        routerApp.show();
        Scanner scanner = new Scanner(System.in);
        while (true) {
            try {
                System.out.print("Input: ");
                String string = scanner.nextLine();
                if (string.equals("quit")) {
                    break;
                }
                String[] strings = string.split(" ");
                arpLayer.request(Integer.parseInt(strings[0]), new IPAddress(strings[1]));
            } catch (Exception e) {
            }
        }
        System.out.println("console exit");
    }

}
