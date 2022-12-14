package layer.link;

import data.address.Address;
import data.address.IPAddress;
import data.address.MACAddress;
import layer.Layer;
import layer.LayerAdapter;
import org.jnetpcap.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NILayer extends LayerAdapter {

    private static final Set<PcapIf> interfaceSet = new HashSet<>();

    static {
        ////////////////////////////////////////////////////////////////////////
        //////////////////////// jNetPcap Library Setup ////////////////////////
        ////////////////////////////////////////////////////////////////////////
        try {
            String jNetPcap;
            String jNetPcapResource;
            String osName = System.getProperty("os.name").toLowerCase();

            System.out.println("[NILayer] operating system : " + osName);

            if (osName.contains("win")) {
                jNetPcap = "jnetpcap.dll";
                jNetPcapResource = "native/windows/" + jNetPcap;
            } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
                jNetPcap = "libjnetpcap.so";
                jNetPcapResource = "native/linux/" + jNetPcap;
            } else {
                throw new RuntimeException("unsupported operating system");
            }

            // Native Library File
            File jNetPcapFile = new File(jNetPcap);
            // Try Copy Library from JAR to Launch Folder
            try (InputStream inputStream = NILayer.class.getClassLoader().getResourceAsStream(jNetPcapResource)) {
                if (inputStream != null) {
                    jNetPcapFile.mkdirs();
                    Files.copy(inputStream, jNetPcapFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (NullPointerException | IOException e) {
                e.printStackTrace();
            }

            // Load Library
            System.load(jNetPcapFile.getAbsolutePath());

            System.out.println("[NILayer] file loaded: " + jNetPcapFile.getName());
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
            System.exit(1);
        }
        ////////////////////////////////////////////////////////////////////////
        parseInterfaceSet();
    }

    private final StringBuilder errorStringBuilder = new StringBuilder();
    private Pcap pcapObject;
    private PcapIf pcapInterface;
    private Thread thread;

    /**
     * ???????????? ??????????????? ?????? ?????? ?????????
     *
     * @param layerName   ?????? ?????? ?????????
     * @param layerNumber ?????? ?????? ??????
     */
    public NILayer(String layerName, int layerNumber) {
        super(layerName, layerNumber);
    }

    /**
     * ?????? ????????? ???????????? ??????????????? ???????????? ???????????? ?????????
     *
     * @return ?????? ????????? ???????????? ??????????????? ?????????
     */
    public static List<PcapIf> getInterfaceList() {
        List<PcapIf> list = new ArrayList<>(interfaceSet);
        list.sort((PcapIf o1, PcapIf o2) -> {
            String s1 = o1.getDescription() != null ? o1.getDescription() : o1.getName();
            String s2 = o2.getDescription() != null ? o2.getDescription() : o2.getName();
            return s1.compareTo(s2);
        });
        return list;
    }

    private static void parseInterfaceSet() {
        // ?????? ?????? ??????
        List<PcapIf> interfaceList = new ArrayList<>();
        StringBuilder errorStringBuilder = new StringBuilder();

        // ?????? ?????? ?????? ??? ????????? ??????
        int result = Pcap.findAllDevs(interfaceList, errorStringBuilder);
        System.out.println("[NILayer] number of interface: " + interfaceList.size());

        // ????????? ????????? ????????? MAC ????????? ?????? ????????? ?????????
        interfaceList.forEach(pcapInterface -> {
            byte[] hardwareAddress = null;
            try {
                hardwareAddress = pcapInterface.getHardwareAddress();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (hardwareAddress == null) {
                System.err.println("no h/w address found \"" + pcapInterface.getDescription() + "\"");
            } else {
                interfaceSet.add(pcapInterface);
                System.out.println(pcapInterface.getDescription());
                pcapInterface.getAddresses().forEach(pcapAddr -> {
                    if (pcapAddr.getAddr().getFamily() == PcapSockAddr.AF_INET) {
                        System.out.println(pcapAddr);
                    }
                });
                StringBuilder hexBuilder = new StringBuilder();
                for (int index = 0; index < hardwareAddress.length; index++) {
                    if (index % 8 == 0) {
                        hexBuilder.append("[NILayer] ");
                    }
                    hexBuilder.append(String.format("%02X", hardwareAddress[index]));
                    if (index + 1 < hardwareAddress.length) {
                        hexBuilder.append((index + 1) % 8 != 0 ? " " : "\n");
                    }
                }
                System.out.println(hexBuilder);
            }
        });
        // ?????? ??????
        if (result != Pcap.OK || interfaceSet.isEmpty()) {
            System.err.println("network interface not found\n" + errorStringBuilder);
            System.exit(1);
        }
    }

    /**
     * ???????????? ??????????????? ?????? ?????????
     *
     * @param pcapInterface ????????? ???????????? ???????????????
     */
    public void setInterface(PcapIf pcapInterface) {
        // ???????????? ??????????????? ?????????
        resetInterface();

        // ???????????? ??????????????? ??????
        this.pcapInterface = pcapInterface;

        // ?????? ?????? ??????
        pcapCapturePacket();

        // ?????? ?????? ????????? ??????
        thread = new Thread(new ReceiveThread(pcapObject, this));
    }

    /**
     * ????????? ???????????? ??????????????? ????????? ?????????
     * ???????????? ?????? ???????????? ???????????? ??????
     */
    public void resetInterface() {
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
        pcapInterface = null;
        pcapObject = null;
    }

    /**
     * ?????? ?????? ????????? ?????? ?????????
     */
    public void startCapture() {
        if (thread != null) {
            thread.start();
        }
    }

    /**
     * Pcap ?????????????????? ????????? ???????????? ????????? ?????? ?????? ?????????
     */
    private void pcapCapturePacket() {
        int snapLength = 64 * 1024; // Capture all packets, no truncation
        int flags = Pcap.MODE_PROMISCUOUS; // capture all packets
        int timeout = 10 * 1000; // 10 seconds in millis
        pcapObject = Pcap.openLive(pcapInterface.getName(), snapLength, flags, timeout, errorStringBuilder);
    }

    @Override
    public void send(int interfaceLayerTo, Address address, byte[] data) {
        if (this.getLayerNumber() != interfaceLayerTo) {
            return;
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        if (pcapObject.sendPacket(byteBuffer) != Pcap.OK) {
            // ?????? ????????? ?????? ??????
            printError(pcapObject.getErr());
        }
    }

    @Override
    public void receive(int interfaceLayerFrom, byte[] data) {
        getUpperLayerList().forEach(layer -> layer.receive(interfaceLayerFrom, data));
    }

    /**
     * ????????? ?????? ????????? ?????????
     */
    private static class ReceiveThread implements Runnable {
        private final Pcap pcapObject;
        private final Layer layer;
        private byte[] data;

        public ReceiveThread(Pcap pcapObject, Layer layer) {
            this.pcapObject = pcapObject;
            this.layer = layer;
        }

        @Override
        public void run() {
            System.out.printf("[%s] %s started\n", layer.getLayerName(), Thread.currentThread().getName());
            // ????????? ??????
            ByteBufferHandler<String> byteBufferHandler = (header, buffer, user) -> {
                data = new byte[buffer.capacity()];
                buffer.get(data);
                layer.receive(layer.getLayerNumber(), data);
            };
            // ????????? ?????? ????????? ?????? ????????? ?????? ?????? ??? ????????? ???????????? ????????? ??????
            while (!Thread.interrupted()) {
                try {
                    if (pcapObject == null) {
                        System.err.printf("[%s] %s pcapObject is null\n", layer.getLayerName(), Thread.currentThread().getName());
                        return;
                    }
                    pcapObject.loop(1, byteBufferHandler, "");
                } catch (Exception e) {
                    // Exception ?????? ??? ??????
                    e.printStackTrace();
                }
            }
            System.out.printf("[%s] %s interrupted\n", layer.getLayerName(), Thread.currentThread().getName());
        }
    }

    public static class PcapIfWrapper {

        private final PcapIf pcapIf;
        private final MACAddress macAddress;
        private final IPAddress ipAddress;
        private final IPAddress netmask;
        private final IPAddress destination;

        public PcapIfWrapper(PcapIf pcapIf) {
            this.pcapIf = pcapIf;

            IPAddress tempIP = null;
            IPAddress tempMask = null;
            IPAddress tempDest = null;
            MACAddress tempMAC;

            // MAC Address
            try {
                tempMAC = new MACAddress(pcapIf.getHardwareAddress());
            } catch (IOException e) {
                e.printStackTrace();
                tempMAC = MACAddress.ZERO;
            }
            macAddress = tempMAC;

            // IP Address
            for (PcapAddr pcapAddr : pcapIf.getAddresses()) {
                if (pcapAddr.getAddr().getFamily() == PcapSockAddr.AF_INET) {
                    tempIP = new IPAddress(pcapAddr.getAddr().getData());
                    tempMask = new IPAddress(pcapAddr.getNetmask().getData());
                    tempDest = tempIP.toNetwork(tempMask);
                    break;
                }
            }
            if (tempIP == null) {
                tempIP = IPAddress.ZERO;
                tempMask = IPAddress.ZERO;
                tempDest = IPAddress.ZERO;
            }
            ipAddress = tempIP;
            netmask = tempMask;
            destination = tempDest;
        }

        public PcapIf get() {
            return pcapIf;
        }

        public MACAddress getMACAddress() {
            return macAddress;
        }

        public IPAddress getIPAddress() {
            return ipAddress;
        }

        public IPAddress getNetmask() {
            return netmask;
        }

        public IPAddress getDestination() {
            return destination;
        }

        @Override
        public String toString() {
            return pcapIf.getDescription() != null ? pcapIf.getDescription() : pcapIf.getName() != null ? pcapIf.getName() : macAddress.toString();
        }
    }
}
