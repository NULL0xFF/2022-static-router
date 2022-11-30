package layer.internet;

import data.address.Address;
import data.address.IPAddress;
import data.address.MACAddress;
import data.unit.frame.EthernetFrame;
import data.unit.packet.ARPPacket;
import launch.StaticRouterMain;
import layer.LayerAdapter;
import layer.application.RouterApp;
import layer.application.SettingApp;
import layer.controller.LayerManager;
import layer.link.EthernetLayer;
import layer.link.NILayer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ARPLayer extends LayerAdapter {

    private final Map<IPAddress, MACAddress> cache = new HashMap<>();
    private final Map<IPAddress, MACAddress> proxyMAC = new HashMap<>();
    private final Map<MACAddress, NILayer.PcapIfWrapper> proxyInterface = new HashMap<>();
    private final Map<IPAddress, Thread> requestThreads = new HashMap<>();
    private final Map<IPAddress, Thread> timeoutThreads = new HashMap<>();

    /**
     * 계층 객체 생성자
     *
     * @param layerName 계층 이름 문자열
     */
    public ARPLayer(String layerName) {
        this(layerName, 0);
    }

    public ARPLayer(String layerName, int layerNumber) {
        super(layerName, layerNumber);
    }

    /**
     * ARP 패킷 생성 메서드
     *
     * @return ARP 패킷 객체
     */
    private ARPPacket createPacket(int interfaceLayerTo) {
        SettingApp setting = (SettingApp) LayerManager.getInstance().get(StaticRouterMain.SETTING, interfaceLayerTo);
        ARPPacket packet = new ARPPacket();
        packet.setSourceMAC(setting.getMyMACAddress());
        packet.setSourceIP(setting.getMyIPAddress());
        return packet;
    }

    /**
     * ARP 패킷의 수신자가 자기 자신인지 확인하는 메서드
     *
     * @param address 수신자 IP 주소 객체
     */
    private boolean isValid(int interfaceLayerFrom, IPAddress address) {
        return ((SettingApp) LayerManager.getInstance().get(StaticRouterMain.SETTING, interfaceLayerFrom)).getMyIPAddress().equals(address);
    }

    /**
     * ARP 패킷의 수신자가 프록시 중에 있는지 확인하는 메서드
     *
     * @param address 수신자 IP 주소 객체
     */
    private boolean isProxy(IPAddress address) {
        return proxyMAC.containsKey(address);
    }

    public MACAddress getMACAddress(IPAddress address) {
        return cache.get(address);
    }

    /**
     * ARP 캐시 추가 메서드
     *
     * @param ipAddress  추가하려는 IP 주소 객체
     * @param macAddress 추가하려는 MAC 주소 객체
     */
    private synchronized void addCache(IPAddress ipAddress, MACAddress macAddress) {
        RouterApp routerApp = (RouterApp) LayerManager.getInstance().get(StaticRouterMain.ROUTER, getLayerNumber());
        cache.put(ipAddress, macAddress);
        routerApp.updateCacheTable(cache);
    }

    /**
     * ARP 캐시 제거 메서드
     *
     * @param ipAddress 제거하려는 IP 주소 객체
     */
    public synchronized void removeCache(IPAddress ipAddress) {
        RouterApp routerApp = (RouterApp) LayerManager.getInstance().get(StaticRouterMain.ROUTER, getLayerNumber());

        // Interrupt Request Thread
        if (requestThreads.containsKey(ipAddress)) {
            Thread thread = requestThreads.get(ipAddress);
            thread.interrupt();
            requestThreads.remove(thread);
        }

        // Interrupt Timeout Thread
        if (timeoutThreads.containsKey(ipAddress)) {
            Thread thread = timeoutThreads.get(ipAddress);
            thread.interrupt();
            timeoutThreads.remove(thread);
        }

        cache.remove(ipAddress);

        routerApp.updateCacheTable(cache);
    }

    /**
     * ARP 캐시 모두 제거 메서드
     */
    public synchronized void clearCache() {
        RouterApp routerApp = (RouterApp) LayerManager.getInstance().get(StaticRouterMain.ROUTER, getLayerNumber());
        requestThreads.forEach((address, thread) -> thread.interrupt());
        timeoutThreads.forEach((address, thread) -> thread.interrupt());
        cache.clear();
        routerApp.updateCacheTable(cache);
    }

    /**
     * ARP 프록시 추가 메서드
     *
     * @param ipAddress     추가하려는 IP 주소 객체
     * @param macAddress    추가하려는 MAC 주소 객체
     * @param pcapIfWrapper 추가하려는 인터페이스 객체
     */
    public synchronized void addProxy(IPAddress ipAddress, MACAddress macAddress, NILayer.PcapIfWrapper pcapIfWrapper) {
        RouterApp routerApp = (RouterApp) LayerManager.getInstance().get(StaticRouterMain.ROUTER, getLayerNumber());
        proxyMAC.put(ipAddress, macAddress);
        proxyInterface.put(macAddress, pcapIfWrapper);
        routerApp.updateProxyTable(proxyMAC, proxyInterface);
    }

    /**
     * ARP 프록시 제거 메서드
     *
     * @param ipAddress 제거하려는 IP 주소 객체
     */
    public synchronized void removeProxy(IPAddress ipAddress) {
        RouterApp routerApp = (RouterApp) LayerManager.getInstance().get(StaticRouterMain.ROUTER, getLayerNumber());
        proxyInterface.remove(proxyMAC.get(ipAddress));
        proxyMAC.remove(ipAddress);
        routerApp.updateProxyTable(proxyMAC, proxyInterface);
    }

    public Thread request(int interfaceLayerTo, IPAddress ipAddress) {
        if (requestThreads.containsKey(ipAddress)) {
            requestThreads.get(ipAddress).interrupt();
        }

        Thread thread = new Thread(() -> {
            // Request
            Thread requestThread = new Thread(new Request(this, interfaceLayerTo, ipAddress));
            requestThread.start();
            try {
                requestThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Check if cache added
            if (cache.get(ipAddress) == null) {
                // Cancel Request
                removeCache(ipAddress);
            } else {
                // Cache Added
                new Thread(new Timeout(this, ipAddress)).start();
            }
        });
        thread.start();
        return thread;
    }


    public void request(int interfaceLayerTo, MACAddress macAddress) {
        // GARP
        new Thread(() -> {
            ARPPacket packet = createPacket(interfaceLayerTo);
            packet.setSourceMAC(macAddress);
            packet.setDestinationMAC(MACAddress.BROADCAST);
            packet.setDestinationIP(packet.getSourceIP());
            ((EthernetLayer) getUnderLayer(StaticRouterMain.ETHERNET, getLayerNumber())).send(interfaceLayerTo, MACAddress.BROADCAST, packet.toBytes(), EthernetFrame.Type.ARP);
        }).start();
    }

    public Thread getRequestThread(IPAddress address) {
        return requestThreads.get(address);
    }

    /**
     * 계층간 바이트 배열 송신 메서드
     *
     * @param interfaceLayerTo 송신에 사용될 계층 식별 번호
     * @param address          송신할 주소 객체
     * @param data             송신된 바이트 배열
     */
    @Override
    public void send(int interfaceLayerTo, Address address, byte[] data) {
        if (address instanceof IPAddress ipAddress) {
            request(interfaceLayerTo, ipAddress);
        } else if (address instanceof MACAddress macAddress) {
            request(interfaceLayerTo, macAddress);
        } else {
            throw new RuntimeException("unsupported address type");
        }
    }

    /**
     * 계층간 바이트 배열 수신 메서드
     *
     * @param interfaceLayerFrom 수신에 사용된 계층 식별 번호
     * @param data               수신된 바이트 배열
     */
    @Override
    public void receive(int interfaceLayerFrom, byte[] data) {
        ARPPacket receivedPacket = new ARPPacket(data);

        if (receivedPacket.getOperation().equals(ARPPacket.Operation.REQUEST)) {
            // ARP Request 수신

            // Cache Table 업데이트 (GARP)
            if (cache.containsKey(receivedPacket.getSourceIP())) {
                addCache(receivedPacket.getDestinationIP(), receivedPacket.getSourceMAC());
            }

            // 수신자 및 프록시 여부 확인
            if (isValid(interfaceLayerFrom, receivedPacket.getDestinationIP()) || isProxy(receivedPacket.getDestinationIP())) {
                // 수신자가 자기 자신 또는 프록시 가능하므로 ARP 응답 송신
                ARPPacket replyPacket = createPacket(interfaceLayerFrom);
                replyPacket.setOperation(ARPPacket.Operation.REPLY);
                replyPacket.setSourceIP(receivedPacket.getDestinationIP());
                replyPacket.setDestinationMAC(receivedPacket.getSourceMAC());
                replyPacket.setDestinationIP(receivedPacket.getSourceIP());
                new Thread(() -> ((EthernetLayer) getUnderLayer(StaticRouterMain.ETHERNET, getLayerNumber())).send(interfaceLayerFrom, replyPacket.getDestinationMAC(), replyPacket.toBytes(), EthernetFrame.Type.ARP)).start();
            }
        } else if (receivedPacket.getOperation().equals(ARPPacket.Operation.REPLY)) {
            // ARP Reply 수신

            // 수신자 확인
            if (isValid(interfaceLayerFrom, receivedPacket.getDestinationIP())) {
                // 수신자가 자기 자신이므로 ARP 응답 처리
                Thread thread = requestThreads.get(receivedPacket.getSourceIP());
                if (thread != null && thread.isAlive()) {
                    addCache(receivedPacket.getSourceIP(), receivedPacket.getSourceMAC());
                    thread.interrupt();
                }
            }
        }
    }

    private static class Request implements Runnable {
        private final ARPLayer layer;
        private final IPAddress destination;
        private final int interfaceNumber;

        public Request(ARPLayer layer, int targetInterfaceNumber, IPAddress destination) {
            this.layer = layer;
            this.interfaceNumber = targetInterfaceNumber;
            this.destination = destination;
        }

        @Override
        public void run() {
            layer.requestThreads.put(destination, Thread.currentThread());

            ARPPacket packet = layer.createPacket(interfaceNumber);
            packet.setOperation(ARPPacket.Operation.REQUEST);
            packet.setDestinationMAC(MACAddress.ZERO);
            packet.setDestinationIP(destination);

            layer.addCache(destination, null);
            ((EthernetLayer) layer.getUnderLayer(StaticRouterMain.ETHERNET, layer.getLayerNumber())).send(interfaceNumber, MACAddress.BROADCAST, packet.toBytes(), EthernetFrame.Type.ARP);

            try {
                TimeUnit.MINUTES.sleep(3);
            } catch (InterruptedException e) {
            }

            layer.requestThreads.remove(destination);
        }
    }

    private static class Timeout implements Runnable {

        private final ARPLayer layer;
        private final IPAddress destination;

        public Timeout(ARPLayer layer, IPAddress destination) {
            this.layer = layer;
            this.destination = destination;
        }

        @Override
        public void run() {
            layer.timeoutThreads.put(destination, Thread.currentThread());

            try {
                TimeUnit.MINUTES.sleep(20);
            } catch (InterruptedException e) {
            }

            layer.removeCache(destination);
            layer.timeoutThreads.remove(destination);
        }
    }
}
