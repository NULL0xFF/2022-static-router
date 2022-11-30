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
     * 네트워크 인터페이스 계층 객체 생성자
     *
     * @param layerName   계층 이름 문자열
     * @param layerNumber 계층 식별 번호
     */
    public NILayer(String layerName, int layerNumber) {
        super(layerName, layerNumber);
    }

    /**
     * 이용 가능한 네트워크 인터페이스 리스트를 반환하는 메서드
     *
     * @return 이용 가능한 네트워크 인터페이스 리스트
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
        // 지역 변수 선언
        List<PcapIf> interfaceList = new ArrayList<>();
        StringBuilder errorStringBuilder = new StringBuilder();

        // 모든 장치 검색 및 리스트 추가
        int result = Pcap.findAllDevs(interfaceList, errorStringBuilder);
        System.out.println("[NILayer] number of interface: " + interfaceList.size());

        // 가용한 장치들 중에서 MAC 주소가 있는 장치들 추리기
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
        // 오류 확인
        if (result != Pcap.OK || interfaceSet.isEmpty()) {
            System.err.println("network interface not found\n" + errorStringBuilder);
            System.exit(1);
        }
    }

    /**
     * 네트워크 인터페이스 설정 메서드
     *
     * @param pcapInterface 설정할 네트워크 인터페이스
     */
    public void setInterface(PcapIf pcapInterface) {
        // 네트워크 인터페이스 초기화
        resetInterface();

        // 네트워크 인터페이스 설정
        this.pcapInterface = pcapInterface;

        // 패킷 캡처 설정
        pcapCapturePacket();

        // 패킷 수신 스레드 설정
        thread = new Thread(new ReceiveThread(pcapObject, this));
    }

    /**
     * 설정된 네트워크 인터페이스 초기화 메서드
     * 실행중인 수신 스레드를 중단하고 정리
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
     * 패킷 캡처 스레드 시작 메서드
     */
    public void startCapture() {
        if (thread != null) {
            thread.start();
        }
    }

    /**
     * Pcap 라이브러리를 이용한 네트워크 프레임 캡처 설정 메서드
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
            // 전송 실패시 오류 출력
            printError(pcapObject.getErr());
        }
    }

    @Override
    public void receive(int interfaceLayerFrom, byte[] data) {
        getUpperLayerList().forEach(layer -> layer.receive(interfaceLayerFrom, data));
    }

    /**
     * 프레임 수신 스레드 클래스
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
            // 핸들러 설정
            ByteBufferHandler<String> byteBufferHandler = (header, buffer, user) -> {
                data = new byte[buffer.capacity()];
                buffer.get(data);
                layer.receive(layer.getLayerNumber(), data);
            };
            // 스레드 중단 요청을 받기 전까지 계속 캡처 후 설정된 계층으로 데이터 송신
            while (!Thread.interrupted()) {
                try {
                    if (pcapObject == null) {
                        System.err.printf("[%s] %s pcapObject is null\n", layer.getLayerName(), Thread.currentThread().getName());
                        return;
                    }
                    pcapObject.loop(1, byteBufferHandler, "");
                } catch (Exception e) {
                    // Exception 출력 후 무시
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
