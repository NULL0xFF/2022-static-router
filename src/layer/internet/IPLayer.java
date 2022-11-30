package layer.internet;

import data.address.Address;
import data.address.IPAddress;
import data.address.MACAddress;
import data.unit.frame.EthernetFrame;
import data.unit.packet.IPPacket;
import launch.StaticRouterMain;
import layer.Layer;
import layer.LayerAdapter;
import layer.application.RouterApp;
import layer.application.SettingApp;
import layer.controller.LayerManager;
import layer.link.EthernetLayer;

public class IPLayer extends LayerAdapter {
    /**
     * 계층 객체 생성자
     *
     * @param layerName 계층 이름 문자열
     */
    public IPLayer(String layerName) {
        this(layerName, 0);
    }

    public IPLayer(String layerName, int layerNumber) {
        super(layerName, layerNumber);
    }

    private IPPacket createPacket(int interfaceLayerTo) {
        IPPacket packet = new IPPacket();
        packet.setSource(((SettingApp) LayerManager.getInstance().get(StaticRouterMain.SETTING, interfaceLayerTo)).getMyIPAddress());
        return packet;
    }

    private boolean isValid(int interfaceLayerFrom, IPAddress address) {
        return ((SettingApp) LayerManager.getInstance().get(StaticRouterMain.SETTING, interfaceLayerFrom)).getMyIPAddress().equals(address);
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
        if (!(address instanceof IPAddress destinationIP)) {
            return;
        }

        ARPLayer arpLayer = (ARPLayer) getUnderLayer(StaticRouterMain.ARP, getLayerNumber());
        MACAddress destinationMAC = arpLayer.getMACAddress(destinationIP);
        if (destinationMAC == null) {
            new Thread(() -> {
                try {
                    arpLayer.request(getLayerNumber(), destinationIP).join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                MACAddress requestedMAC = arpLayer.getMACAddress(destinationIP);
                if (requestedMAC != null) {
                    IPPacket packet = createPacket(interfaceLayerTo);
                    packet.setDestination(destinationIP);
                    packet.setData(data);
                    new Thread(() -> ((EthernetLayer) getUnderLayer(StaticRouterMain.ETHERNET, getLayerNumber())).send(interfaceLayerTo, requestedMAC, packet.toBytes(), EthernetFrame.Type.IP));
                }
            }).start();
        }

        new Thread(() -> {
            IPPacket packet = createPacket(interfaceLayerTo);
            packet.setDestination(destinationIP);
            packet.setData(data);
            ((EthernetLayer) getUnderLayer(StaticRouterMain.ETHERNET, getLayerNumber())).send(interfaceLayerTo, destinationMAC, packet.toBytes(), EthernetFrame.Type.IP);
        }).start();
    }

    /**
     * 계층간 바이트 배열 수신 메서드
     *
     * @param interfaceLayerFrom 수신에 사용된 계층 식별 번호
     * @param data               수신된 바이트 배열
     */
    @Override
    public void receive(int interfaceLayerFrom, byte[] data) {
        // 첫째, 해당 패킷의 IP 목적지 주소를 가져온다.
        // 둘째, Routing Table 을 통해, 해당 패킷을 전달할 네트워크 주소를 알아낸다.
        // 셋째, 네트워크 주소로 보내기 위한 인터페이스를 선택한다.
        // 넷째, 선택된 인터페이스를 통해 Gateway(by Routing Table)로 패킷을 전송한다.
        // -> Gateway 의 IP 주소는 Routing Table 을 통해 알 수 있다.
        // -> 패킷을 Gateway 에게 전달하려면, 그 Gateway 의 MAC 주소를 알아야 한다.
        // -> MAC 주소는 ARP Cache Table 에서, Gateway 주소에 해당하는 MAC 주소를 가져온다.
        // -> ARP Cache Table 에 Gateway 의 정보가 없다면, ARP 메시지를 통해서 MAC 주소를 알아 낸다.
        // 모든 Router 가 이 과정을 반복하며, 목적지까지 패킷을 전달한다.

        IPPacket receivedPacket = new IPPacket(data);

        if (isValid(interfaceLayerFrom, receivedPacket.getDestination())) {
            // 목적지가 라우터일 경우
            return; // TODO ICMP의 경우 처리 ?
        } else {
            // 패킷 라우팅 처리

            RouterApp routerApp = (RouterApp) getUpperLayer(StaticRouterMain.ROUTER, getLayerNumber());
            RouterApp.RouteEntry entry = routerApp.findEntry(receivedPacket.getDestination());

            if (entry != null) {
                // Entry 가 존재할 경우 Static Routing 처리
                // Entry에 해당하는 인터페이스가 적용된 NILayer 찾기
                for (Layer layer : LayerManager.getInstance().getList(StaticRouterMain.SETTING)) {
                    if (entry.interfaceWrapper().get().equals(((SettingApp) layer).getInterface())) {
                        // 인터페이스 선택됨
                        ARPLayer arpLayer = (ARPLayer) getUnderLayer(StaticRouterMain.ARP, getLayerNumber());
                        MACAddress destinationMAC = arpLayer.getMACAddress(receivedPacket.getDestination());
                        if (destinationMAC == null) {
                            new Thread(() -> {
                                try {
                                    arpLayer.request(layer.getLayerNumber(), receivedPacket.getDestination()).join();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                MACAddress requestMAC = arpLayer.getMACAddress(entry.gateway());
                                if (requestMAC != null) {
                                    new Thread(() -> ((EthernetLayer) getUnderLayer(StaticRouterMain.ETHERNET, getLayerNumber())).send(layer.getLayerNumber(), requestMAC, data, EthernetFrame.Type.IP)).start();
                                }
                            }).start();
                        } else {
                            new Thread(() -> ((EthernetLayer) getUnderLayer(StaticRouterMain.ETHERNET, getLayerNumber())).send(layer.getLayerNumber(), destinationMAC, data, EthernetFrame.Type.IP)).start();
                        }
                    }
                }
            }
        }

    }
}
