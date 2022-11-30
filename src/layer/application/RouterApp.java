package layer.application;

import data.address.IPAddress;
import data.address.MACAddress;
import launch.StaticRouterMain;
import layer.controller.LayerManager;
import layer.internet.ARPLayer;
import layer.link.NILayer;
import org.jnetpcap.PcapIf;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RouterApp extends GUILayer {

    private static final String[] ARP_TABLE_HEADER = new String[]{"IP Address", "MAC Address", "Info"};
    private static final String[] ROUTER_TABLE_HEADER = new String[]{"Dest.", "Netmask", "G/W", "Flag", "I/F", "Metric"};
    private static final List<RouteEntry> routeList = new ArrayList<>();
    private JFrame mainFrame;
    private JFrame staticRouteFrame;
    private JFrame proxyARPFrame;
    private JPanel mainPanel;
    private JTable routeTable;
    private JTable cacheTable;
    private JTable proxyTable;
    private JButton routeAddButton;
    private JButton routeDeleteButton;
    private JButton proxyAddButton;
    private JButton cacheDeleteButton;
    private JButton proxyDeleteButton;
    private JButton upButton;
    private JButton downButton;

    /**
     * 계층 객체 생성자
     *
     * @param layerName 계층 이름 문자열
     */
    public RouterApp(String layerName, int layerNumber) {
        super(layerName, layerNumber);
        initialize();
    }

    public RouterApp(String layerName) {
        this(layerName, 0);
    }

    private void initialize() {
        // Frame Initialization
        mainFrame = new JFrame("Router");

        // Frame Setting
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setJMenuBar(createMenuBar());
        mainFrame.setResizable(false);

        // Components Setting
        staticRouteFrame = createStaticRouteFrame();
        proxyARPFrame = createProxyARPFrame();
        routeAddButton.addActionListener(e -> {
            staticRouteFrame.setVisible(true);
        });
        routeDeleteButton.addActionListener(e -> {
            if (routeTable.getSelectedRow() != -1) {
                routeList.remove(routeTable.getSelectedRow());
                updateRouteTable();
            }
        });
        cacheDeleteButton.addActionListener(e -> {
            if (cacheTable.getSelectedRow() != -1) {
                IPAddress address = new IPAddress((String) cacheTable.getValueAt(cacheTable.getSelectedRow(), 0));
                ((ARPLayer) LayerManager.getInstance().get(StaticRouterMain.ARP, getLayerNumber())).removeCache(address);
            }
        });
        proxyAddButton.addActionListener(e -> {
            proxyARPFrame.setVisible(true);
        });
        proxyDeleteButton.addActionListener(e -> {
            if (proxyTable.getSelectedRow() != -1) {
                IPAddress address = new IPAddress((String) proxyTable.getValueAt(proxyTable.getSelectedRow(), 0));
                ((ARPLayer) LayerManager.getInstance().get(StaticRouterMain.ARP, getLayerNumber())).removeProxy(address);
            }
        });
        updateRouteTable();
        updateCacheTable(null);
        updateProxyTable(null, null);

        // Add Components
        mainFrame.setContentPane(mainPanel);
        mainFrame.pack();

        // Set Frame Position
        mainFrame.setLocationRelativeTo(null);
        staticRouteFrame.setLocationRelativeTo(mainFrame);
        proxyARPFrame.setLocationRelativeTo(mainFrame);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu filemenu = new JMenu("File");
        JMenuItem settingMenuItem = new JMenuItem(StaticRouterMain.SETTING + 0);
        settingMenuItem.addActionListener(e -> ((GUILayer) LayerManager.getInstance().get(StaticRouterMain.SETTING, 0)).show());
        filemenu.add(settingMenuItem);
        settingMenuItem = new JMenuItem(StaticRouterMain.SETTING + 1);
        settingMenuItem.addActionListener(e -> ((GUILayer) LayerManager.getInstance().get(StaticRouterMain.SETTING, 1)).show());
        filemenu.add(settingMenuItem);
        JMenuItem exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.addActionListener(e -> System.exit(0));
        filemenu.add(exitMenuItem);
        menuBar.add(filemenu);

        JMenu lnfMenu = new JMenu("L&F");
        JMenuItem lnfDefaultItem = new JMenuItem("Cross-Flatform");
        lnfDefaultItem.addActionListener(e -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                mainFrame.repaint();
            } catch (Exception exc) {
                System.err.println("Error loading L&F: " + exc);
            }
        });
        lnfMenu.add(lnfDefaultItem);
        JMenuItem lnfSystemItem = new JMenuItem("System");
        lnfSystemItem.addActionListener(e -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                mainFrame.repaint();
            } catch (Exception exc) {
                System.err.println("Error loading L&F: " + exc);
            }
        });
        lnfMenu.add(lnfSystemItem);
        menuBar.add(lnfMenu);

        return menuBar;
    }

    private JComboBox<NILayer.PcapIfWrapper> createInterfaceComboBox() {
        JComboBox<NILayer.PcapIfWrapper> comboBox = new JComboBox<>();
        List<PcapIf> interfaceList = NILayer.getInterfaceList();
        interfaceList.forEach(pcapIf -> comboBox.addItem(new NILayer.PcapIfWrapper(pcapIf)));
        comboBox.setSelectedIndex(-1);
        return comboBox;
    }

    private JFrame createStaticRouteFrame() {
        JFrame frame = new JFrame("Static Router");
        GroupLayout layout = new GroupLayout(frame.getContentPane());
        frame.getContentPane().setLayout(layout);

        JLabel destinationLabel = new JLabel("Destination");
        JLabel netmaskLabel = new JLabel("Netmask");
        JLabel gatewayLabel = new JLabel("Gateway");
        JLabel flagLabel = new JLabel("Flag");
        JLabel interfaceLabel = new JLabel("Interface");
        JTextField destinationField = new JTextField();
        JTextField netmaskField = new JTextField();
        JTextField gatewayField = new JTextField();
        JPanel flagBoxPanel = new JPanel(new GridLayout(0, 3, 0, 0));
        JCheckBox flagUp = new JCheckBox("UP");
        JCheckBox flagGateway = new JCheckBox("G/W");
        JCheckBox flagHost = new JCheckBox("Host");
        JComboBox<NILayer.PcapIfWrapper> interfaceComboBox = createInterfaceComboBox();
        JButton addButton = new JButton("Add");
        JButton closeButton = new JButton("Close");

        destinationLabel.setHorizontalAlignment(JLabel.CENTER);
        netmaskLabel.setHorizontalAlignment(JLabel.CENTER);
        gatewayLabel.setHorizontalAlignment(JLabel.CENTER);
        flagLabel.setHorizontalAlignment(JLabel.CENTER);
        interfaceLabel.setHorizontalAlignment(JLabel.CENTER);
        interfaceComboBox.addActionListener(e -> {
            NILayer.PcapIfWrapper selectedItem = (NILayer.PcapIfWrapper) interfaceComboBox.getSelectedItem();
            if (selectedItem != null) {
                destinationField.setText(selectedItem.getDestination().toString());
                netmaskField.setText(selectedItem.getNetmask().toString());
            }
        });

        addButton.addActionListener(e -> {
            IPAddress destination = new IPAddress(destinationField.getText());
            IPAddress netmask = new IPAddress(netmaskField.getText());
            if (!netmask.isNetmask()) {
                printError("invalid netmask");
                return;
            }
            IPAddress gateway = new IPAddress(gatewayField.getText());

            // Remove previously added entries with same destination and netmask
            routeList.removeIf(entry -> entry.destination.equals(destination) && entry.netmask.equals(netmask));

            // Add entry
            routeList.add(new RouteEntry(destination, netmask, gateway, flagUp.isSelected(), flagGateway.isSelected(), flagHost.isSelected(), (NILayer.PcapIfWrapper) interfaceComboBox.getSelectedItem(), 1));

            updateRouteTable();
            frame.dispose();
        });
        closeButton.addActionListener(e -> frame.dispose());

        flagBoxPanel.add(flagUp);
        flagBoxPanel.add(flagGateway);
        flagBoxPanel.add(flagHost);

        layout.setAutoCreateGaps(false);
        layout.setAutoCreateContainerGaps(false);

        layout.setHorizontalGroup(layout.createSequentialGroup().addGroup(layout.createParallelGroup().addComponent(destinationLabel).addComponent(netmaskLabel).addComponent(gatewayLabel).addComponent(flagLabel).addComponent(interfaceLabel).addComponent(addButton)).addGroup(layout.createParallelGroup().addComponent(destinationField).addComponent(netmaskField).addComponent(gatewayField).addComponent(flagBoxPanel).addComponent(interfaceComboBox).addComponent(closeButton)));
        layout.setVerticalGroup(layout.createSequentialGroup().addGroup(layout.createParallelGroup().addComponent(destinationLabel).addComponent(destinationField)).addGroup(layout.createParallelGroup().addComponent(netmaskLabel).addComponent(netmaskField)).addGroup(layout.createParallelGroup().addComponent(gatewayLabel).addComponent(gatewayField)).addGroup(layout.createParallelGroup().addComponent(flagLabel).addComponent(flagBoxPanel)).addGroup(layout.createParallelGroup().addComponent(interfaceLabel).addComponent(interfaceComboBox)).addGroup(layout.createParallelGroup().addComponent(addButton).addComponent(closeButton)));
        layout.linkSize(destinationLabel, destinationField, netmaskLabel, netmaskField, gatewayLabel, gatewayField, flagLabel, flagBoxPanel, interfaceLabel, interfaceComboBox, addButton, closeButton);

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setResizable(false);
        frame.pack();

        return frame;
    }

    private JFrame createProxyARPFrame() {
        JFrame frame = new JFrame("Proxy ARP");
        GroupLayout layout = new GroupLayout(frame.getContentPane());
        frame.getContentPane().setLayout(layout);

        JLabel interfaceLabel = new JLabel("Interface");
        JLabel ipLabel = new JLabel("IP");
        JLabel macLabel = new JLabel("MAC");
        JComboBox<NILayer.PcapIfWrapper> interfaceComboBox = createInterfaceComboBox();
        JTextField ipField = new JTextField();
        JTextField macField = new JTextField();
        JButton addButton = new JButton("Add");
        JButton closeButton = new JButton("Close");

        ipLabel.setHorizontalAlignment(JLabel.CENTER);
        macLabel.setHorizontalAlignment(JLabel.CENTER);
        interfaceLabel.setHorizontalAlignment(JLabel.CENTER);
        interfaceComboBox.addActionListener(e -> {
            NILayer.PcapIfWrapper selectedItem = (NILayer.PcapIfWrapper) interfaceComboBox.getSelectedItem();
            if (selectedItem != null) {
                ipField.setText(selectedItem.getIPAddress().toString());
                macField.setText(selectedItem.getMACAddress().toString());
            }
        });

        addButton.addActionListener(e -> {
            ARPLayer arpLayer = (ARPLayer) LayerManager.getInstance().get(StaticRouterMain.ARP, getLayerNumber());
            IPAddress ipAddress = new IPAddress(ipField.getText());
            MACAddress macAddress = new MACAddress(macField.getText());
            arpLayer.addProxy(ipAddress, macAddress, (NILayer.PcapIfWrapper) interfaceComboBox.getSelectedItem());
            frame.dispose();
        });
        closeButton.addActionListener(e -> frame.dispose());

        layout.setAutoCreateGaps(false);
        layout.setAutoCreateContainerGaps(false);

        layout.setHorizontalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup()
                        .addComponent(ipLabel)
                        .addComponent(macLabel)
                        .addComponent(interfaceLabel)
                        .addComponent(addButton))
                .addGroup(layout.createParallelGroup()
                        .addComponent(ipField)
                        .addComponent(macField)
                        .addComponent(interfaceComboBox)
                        .addComponent(closeButton)));
        layout.setVerticalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup()
                        .addComponent(ipLabel)
                        .addComponent(ipField))
                .addGroup(layout.createParallelGroup()
                        .addComponent(macLabel)
                        .addComponent(macField))
                .addGroup(layout.createParallelGroup()
                        .addComponent(interfaceLabel)
                        .addComponent(interfaceComboBox))
                .addGroup(layout.createParallelGroup()
                        .addComponent(addButton)
                        .addComponent(closeButton)));
        layout.linkSize(ipLabel, ipField, macLabel, macField, interfaceLabel, interfaceComboBox, addButton, closeButton);

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setResizable(false);
        frame.pack();

        return frame;
    }

    @Override
    public void show() {
        mainFrame.setVisible(true);
    }


    public void updateCacheTable(Map<IPAddress, MACAddress> cache) {
        // Update ARP Cache Table
        Object[][] tableData;
        if (cache != null) {
            tableData = new Object[cache.keySet().size()][3];
            List<IPAddress> keyList = new ArrayList<>(cache.keySet());
            for (int index = 0; index < keyList.size(); index++) {
                IPAddress key = keyList.get(index);
                MACAddress value = cache.get(key);
                // TODO Cache Timeout
                tableData[index][0] = key.toString();
                tableData[index][1] = value != null ? value.toString() : "";
                tableData[index][2] = value != null ? "done" : "requested";
            }
        } else {
            tableData = new Object[0][3];
        }
        cacheTable.setModel(new DefaultTableModel(tableData, ARP_TABLE_HEADER));
        cacheTable.getTableHeader().setReorderingAllowed(false);
        cacheTable.getTableHeader().setResizingAllowed(false);
        cacheTable.setDefaultEditor(Object.class, null);
        cacheTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        cacheTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        cacheTable.getColumnModel().getColumn(2).setPreferredWidth(130);
    }

    public void updateProxyTable(Map<IPAddress, MACAddress> macTable, Map<MACAddress, NILayer.PcapIfWrapper> ifTable) {
        // Update ARP Cache Table
        Object[][] tableData;
        if (macTable != null && ifTable != null) {
            tableData = new Object[macTable.keySet().size()][3];
            List<IPAddress> keyList = new ArrayList<>(macTable.keySet());
            for (int index = 0; index < keyList.size(); index++) {
                IPAddress ipAddress = keyList.get(index);
                MACAddress macAddress = macTable.get(ipAddress);
                NILayer.PcapIfWrapper pcapInterface = ifTable.get(macAddress);
                tableData[index][0] = ipAddress.toString();
                tableData[index][1] = macAddress.toString();
                tableData[index][2] = pcapInterface.toString();
            }
        } else {
            tableData = new Object[0][3];
        }
        proxyTable.setModel(new DefaultTableModel(tableData, ARP_TABLE_HEADER));
        proxyTable.getTableHeader().setReorderingAllowed(false);
        proxyTable.getTableHeader().setResizingAllowed(false);
        proxyTable.setDefaultEditor(Object.class, null);
        proxyTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        proxyTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        proxyTable.getColumnModel().getColumn(2).setPreferredWidth(130);
    }

    private void updateRouteTable() {
        // Update Route Table
        Object[][] tableData = new Object[routeList.size()][6];
        for (int index = 0; index < routeList.size(); index++) {
            RouteEntry entry = routeList.get(index);
            tableData[index][0] = entry.destination.toString();
            tableData[index][1] = entry.netmask.toString();
            tableData[index][2] = entry.gateway.toString();
            tableData[index][3] = String.format("%s%s%s", entry.flagUp ? "U" : "", entry.flagGateway ? "G" : "", entry.flagHost ? "H" : "");
            tableData[index][4] = entry.interfaceWrapper.toString();
            tableData[index][5] = entry.metric;
        }
        routeTable.setModel(new DefaultTableModel(tableData, ROUTER_TABLE_HEADER));
        routeTable.getTableHeader().setReorderingAllowed(false);
        routeTable.getTableHeader().setResizingAllowed(false);
        routeTable.setDefaultEditor(Object.class, null);
        routeTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        routeTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        routeTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        routeTable.getColumnModel().getColumn(3).setPreferredWidth(25);
        routeTable.getColumnModel().getColumn(5).setPreferredWidth(40);
    }

    public RouteEntry findEntry(IPAddress destination) {
        for (RouteEntry entry : routeList) {
            if (destination.toNetwork(entry.netmask).equals(entry.destination)) {
                return entry;
            }
        }
        return null;
    }

    public static final class RouteEntry {
        private final IPAddress destination;
        private final IPAddress netmask;
        private final IPAddress gateway;
        private final boolean flagUp;
        private final boolean flagGateway;
        private final boolean flagHost;
        private final NILayer.PcapIfWrapper interfaceWrapper;
        private final int metric;

        private RouteEntry(IPAddress destination, IPAddress netmask, IPAddress gateway, boolean isUp, boolean isGateway, boolean isHost, NILayer.PcapIfWrapper interfaceWrapper, int metric) {
            this.destination = destination;
            this.netmask = netmask;
            this.gateway = gateway;
            this.flagUp = isUp;
            this.flagGateway = isGateway;
            this.flagHost = isHost;
            this.interfaceWrapper = interfaceWrapper;
            this.metric = metric;
        }

        public IPAddress destination() {
            return destination;
        }

        public IPAddress netmask() {
            return netmask;
        }

        public IPAddress gateway() {
            return gateway;
        }

        public boolean isUp() {
            return flagUp;
        }

        public boolean isGateway() {
            return flagGateway;
        }

        public boolean isHost() {
            return flagHost;
        }

        public NILayer.PcapIfWrapper interfaceWrapper() {
            return interfaceWrapper;
        }

        public int metric() {
            return metric;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (RouteEntry) obj;
            return Objects.equals(this.destination, that.destination) && Objects.equals(this.netmask, that.netmask) && Objects.equals(this.gateway, that.gateway) && this.flagUp == that.flagUp && this.flagGateway == that.flagGateway && this.flagHost == that.flagHost && Objects.equals(this.interfaceWrapper, that.interfaceWrapper) && this.metric == that.metric;
        }

        @Override
        public int hashCode() {
            return Objects.hash(destination, netmask, gateway, flagUp, flagGateway, flagHost, interfaceWrapper, metric);
        }

        @Override
        public String toString() {
            return "RouteEntry[" + "destination=" + destination + ", " + "netmask=" + netmask + ", " + "gateway=" + gateway + ", " + "isUp=" + flagUp + ", " + "isGateway=" + flagGateway + ", " + "isHost=" + flagHost + ", " + "pcapInterface=" + interfaceWrapper + ", " + "metric=" + metric + ']';
        }

    }
}
