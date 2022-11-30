package layer.application;

import data.address.Address;
import layer.LayerAdapter;

public abstract class GUILayer extends LayerAdapter {

    /**
     * GUI 계층 객체 생성자
     *
     * @param layerName 계층 이름 문자열
     */
    public GUILayer(String layerName, int layerNumber) {
        super(layerName, layerNumber);
    }

    /**
     * GUI JFrame 호출 메서드
     */
    public abstract void show();


    /**
     * 계층간 바이트 배열 송신 메서드
     *
     * @param interfaceLayerTo 송신에 사용될 계층 식별 번호
     * @param address          송신할 주소 객체
     * @param data             송신된 바이트 배열
     */
    @Override
    public void send(int interfaceLayerTo, Address address, byte[] data) {
    }

    /**
     * 계층간 바이트 배열 수신 메서드
     *
     * @param interfaceLayerFrom 수신에 사용된 계층 식별 번호
     * @param data               수신된 바이트 배열
     */
    @Override
    public void receive(int interfaceLayerFrom, byte[] data) {
    }

}
