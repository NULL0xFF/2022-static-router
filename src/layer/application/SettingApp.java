package layer.application;

import data.address.IPAddress;
import data.address.MACAddress;
import launch.StaticRouterMain;
import layer.controller.LayerManager;
import layer.link.NILayer;
import org.jnetpcap.PcapIf;

import javax.swing.*;
import java.util.List;

public class SettingApp extends GUILayer {

    private JFrame mainFrame;
    private JPanel mainPanel;
    private JComboBox<NILayer.PcapIfWrapper> interfaceComboBox;
    private JTextField macTextField;
    private JTextField ipTextField;
    private JButton settingButton;

    private PcapIf pcapIf;
    private MACAddress macAddress;
    private IPAddress ipAddress;

    /**
     * GUI 계층 객체 생성자
     *
     * @param layerName 계층 이름 문자열
     */
    public SettingApp(String layerName) {
        this(layerName, 0);
    }

    public SettingApp(String layerName, int layerNumber) {
        super(layerName, layerNumber);
        initialize();
    }

    private void initialize() {
        // Frame Initialization
        mainFrame = new JFrame(this.toString());

        // Frame Setting
        mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        mainFrame.setResizable(false);

        // Components Setting
        initializeComponents();

        // Add Components
        mainFrame.setContentPane(mainPanel);
        mainFrame.pack();

        // Set Frame Position
        mainFrame.setLocationRelativeTo(null);
    }

    private void initializeComponents() {
        NILayer niLayer = (NILayer) LayerManager.getInstance().get(StaticRouterMain.NETWORK_INTERFACE, getLayerNumber());

        // Combobox
        List<PcapIf> interfaceList = NILayer.getInterfaceList();
        interfaceList.forEach(pcapIf -> interfaceComboBox.addItem(new NILayer.PcapIfWrapper(pcapIf)));
        interfaceComboBox.addActionListener(e -> setSelectedInterfaceItem());

        // Config Button
        settingButton.addActionListener(e -> {
            switch (settingButton.getText()) {
                case "Apply" -> {
                    if (interfaceComboBox.getSelectedItem() != null) {
                        try {
                            NILayer.PcapIfWrapper wrapper = (NILayer.PcapIfWrapper) interfaceComboBox.getSelectedItem();
                            niLayer.setInterface(wrapper.get());
                            print(wrapper.toString());
                            interfaceComboBox.setEnabled(false);
                            macTextField.setEnabled(false);
                            macAddress = new MACAddress(macTextField.getText());
                            ipTextField.setEnabled(false);
                            ipAddress = new IPAddress(ipTextField.getText());
                            settingButton.setText("Reset");
                            pcapIf = wrapper.get();
                            niLayer.startCapture();
                        } catch (RuntimeException ex) {
                            ex.printStackTrace();
                            niLayer.resetInterface();
                            interfaceComboBox.setEnabled(true);
                            macTextField.setEnabled(true);
                            ipTextField.setEnabled(true);
                            pcapIf = null;
                        }
                    }
                }
                case "Reset" -> {
                    niLayer.resetInterface();
                    setSelectedInterfaceItem();
                    settingButton.setText("Apply");
                    interfaceComboBox.setEnabled(true);
                    macAddress = null;
                    macTextField.setEnabled(true);
                    ipAddress = null;
                    ipTextField.setEnabled(true);
                    pcapIf = null;
                }
                default -> throw new IllegalStateException("Unexpected value: " + settingButton.getText());
            }
        });
    }

    private void setSelectedInterfaceItem() {
        NILayer.PcapIfWrapper selectedItem = (NILayer.PcapIfWrapper) interfaceComboBox.getSelectedItem();
        if (selectedItem != null) {
            macTextField.setText(selectedItem.getMACAddress().toString());
            ipTextField.setText(selectedItem.getIPAddress().toString());
        }
    }

    public PcapIf getInterface() {
        return pcapIf;
    }

    public MACAddress getMyMACAddress() {
        return macAddress;
    }

    public IPAddress getMyIPAddress() {
        return ipAddress;
    }

    @Override
    public void show() {
        mainFrame.setVisible(true);
    }

}
