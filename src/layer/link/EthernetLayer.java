package layer.link;

import data.address.Address;
import data.address.MACAddress;
import data.unit.frame.EthernetFrame;
import launch.StaticRouterMain;
import layer.LayerAdapter;
import layer.application.SettingApp;
import layer.controller.LayerManager;

/**
 * 이더넷 계층 클래스
 */
public class EthernetLayer extends LayerAdapter {

    /**
     * 이더넷 계층 객체 생성자
     *
     * @param layerName 계층 이름 문자열
     */
    public EthernetLayer(String layerName) {
        this(layerName, 0);
    }

    public EthernetLayer(String layerName, int layerNumber) {
        super(layerName, layerNumber);
    }

    /**
     * 기본 이더넷 프레임 생성 메서드
     */
    private EthernetFrame createFrame(int layerNumberTo) {
        SettingApp setting = (SettingApp) LayerManager.getInstance().get(StaticRouterMain.SETTING, layerNumberTo);
        EthernetFrame frame = new EthernetFrame();
        frame.setSource(setting.getMyMACAddress());
        return frame;
    }

    /**
     * 이더넷 프레임의 송신자가 자기 자신인지 확인하는 메서드
     *
     * @param frame 이더넷 프레임
     * @return {@code true} if frame is sent from me
     */
    private boolean isMyFrame(int interfaceLayerFrom, EthernetFrame frame) {
        return frame.getSource().equals(((SettingApp) LayerManager.getInstance().get(StaticRouterMain.SETTING, interfaceLayerFrom)).getMyMACAddress());
    }

    /**
     * 이더넷 프레임이 Broadcast 프레임인지 확인하는 메서드
     *
     * @param frame 이더넷 프레임
     * @return {@code true} if frame is broadcast frame
     */
    private boolean isBroadcast(EthernetFrame frame) {
        return frame.getDestination().equals(MACAddress.BROADCAST);
    }

    /**
     * 이더넷 프레임의 목적지가 자기 자신인지 확인하는 메서드
     *
     * @param frame 이더넷 프레임
     * @return {@code true} if frame is sent to me
     */
    private boolean isValid(int interfaceLayerFrom, EthernetFrame frame) {
        return frame.getDestination().equals(((SettingApp) LayerManager.getInstance().get(StaticRouterMain.SETTING, interfaceLayerFrom)).getMyMACAddress());
    }

    public void send(int interfaceLayerTo, Address address, byte[] data, EthernetFrame.Type type) {
        EthernetFrame frame = createFrame(interfaceLayerTo);
        frame.setDestination((MACAddress) address);
        frame.setType(type);
        frame.setData(data);
        getUnderLayerList().forEach(layer -> layer.send(interfaceLayerTo, null, frame.toBytes()));
    }

    @Override
    public void send(int interfaceLayerTo, Address address, byte[] data) {
        send(interfaceLayerTo, address, data, EthernetFrame.Type.IP);
    }

    /**
     * 계층간 바이트 배열 수신 메서드
     *
     * @param interfaceLayerFrom 수신에 사용된 계층 식별 번호
     * @param data               수신된 바이트 배열
     */
    @Override
    public void receive(int interfaceLayerFrom, byte[] data) {
        // 수신된 바이트 배열을 프레임 객체로 캐스팅
        EthernetFrame frame = new EthernetFrame(data);

        if (!isMyFrame(interfaceLayerFrom, frame)) {
            if (isBroadcast(frame)) {
                // 브로드캐스트 프레임
                if (frame.getType().equals(EthernetFrame.Type.IP)) {
                    // IP 패킷일 경우
                    getUpperLayer(StaticRouterMain.IP, getLayerNumber()).receive(interfaceLayerFrom, frame.getData());
                } else if (frame.getType().equals(EthernetFrame.Type.ARP)) {
                    // ARP 패킷일 경우
                    getUpperLayer(StaticRouterMain.ARP, getLayerNumber()).receive(interfaceLayerFrom, frame.getData());
                }
            } else if (isValid(interfaceLayerFrom, frame)) {
                // 수신자가 자기 자신이므로 유효
                if (frame.getType().equals(EthernetFrame.Type.IP)) {
                    // IP 패킷일 경우
                    getUpperLayer(StaticRouterMain.IP, getLayerNumber()).receive(interfaceLayerFrom, frame.getData());
                } else if (frame.getType().equals(EthernetFrame.Type.ARP)) {
                    // ARP 패킷일 경우
                    getUpperLayer(StaticRouterMain.ARP, getLayerNumber()).receive(interfaceLayerFrom, frame.getData());
                }
            }
        }
//        else {
//            if (isBroadcast(frame) && frame.getType().equals(EthernetFrame.Type.ARP)) {
//                getUpperLayer(StaticRouterMain.ARP, getLayerNumber()).receive(interfaceLayerFrom, frame.getData());
//            }
//        }
    }
}
