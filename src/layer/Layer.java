package layer;

import data.address.Address;

import java.util.List;

/**
 * 계층 인터페이스
 */
public interface Layer {

    /**
     * 계층 이름 접근 메서드
     *
     * @return 계층 이름 문자열
     */
    String getLayerName();

    /**
     * 계층 식별 번호 접근 메서드
     *
     * @return 계층 식별 번호
     */
    int getLayerNumber();

    /**
     * 상위 계층 접근 메서드
     *
     * @param upperLayerName 접근하려는 상위 계층 이름 문자열
     * @param layerNumber    접근하려는 상위 계층 식별 번호
     * @return 접근된 상위 계층 객체
     */
    Layer getUpperLayer(String upperLayerName, int layerNumber);

    /**
     * 상위 계층 List 접근 메서드
     *
     * @return 상위 계층 List 객체
     */
    List<Layer> getUpperLayerList();

    /**
     * 하위 계층 접근 메서드
     *
     * @param underLayerName 접근하려는 하위 계층 이름 문자열
     * @param layerNumber    접근하려는 하위 계층 식별 번호
     * @return 접근된 하위 계층 객체
     */
    Layer getUnderLayer(String underLayerName, int layerNumber);

    /**
     * 하위 계층 List 접근 메서드
     *
     * @return 하위 계층 List 객체
     */
    List<Layer> getUnderLayerList();

    /**
     * 상위 계층 추가 메서드
     *
     * @param upperLayer 추가하려는 상위 계층 객체
     */
    void addUpperLayer(Layer upperLayer);

    /**
     * 하위 계층 추가 메서드
     *
     * @param underLayer 추가하려는 하위 계층 객체
     */
    void addUnderLayer(Layer underLayer);

    /**
     * 상위 계층 제거 메서드
     *
     * @param upperLayerName 제거하려는 상위 계층 이름 문자열
     * @param layerNumber    제거하려는 상위 계층 식별 번호
     */
    void removeUpperLayer(String upperLayerName, int layerNumber);

    /**
     * 하위 계층 제거 메서드
     *
     * @param underLayerName 제거하려는 하위 계층 이름 문자열
     * @param layerNumber    제거하려는 하위 계층 식별 번호
     */
    void removeUnderLayer(String underLayerName, int layerNumber);

    /**
     * 계층간 바이트 배열 송신 메서드
     *
     * @param interfaceLayerTo 송신에 사용될 계층 식별 번호
     * @param address          송신할 주소 객체
     * @param data             송신된 바이트 배열
     */
    void send(int interfaceLayerTo, Address address, byte[] data);

    /**
     * 계층간 바이트 배열 수신 메서드
     *
     * @param interfaceLayerFrom 수신에 사용된 계층 식별 번호
     * @param data               수신된 바이트 배열
     */
    void receive(int interfaceLayerFrom, byte[] data);

}
