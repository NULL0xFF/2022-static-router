package layer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class LayerAdapter implements Layer {

    private final Map<String, Map<Integer, Layer>> upperLayers = new HashMap<>();
    private final Map<String, Map<Integer, Layer>> underLayers = new HashMap<>();
    private final String layerName;
    private final int layerNumber;

    private int printLineNumber = 0;

    /**
     * 계층 객체 생성자
     *
     * @param layerName 계층 이름 문자열
     * @deprecated
     */
    public LayerAdapter(String layerName) {
        this(layerName, 0);
    }

    /**
     * 계층 객체 생성자
     *
     * @param layerName   계층 이름 문자열
     * @param layerNumber 계층 식별 번호
     */
    public LayerAdapter(String layerName, int layerNumber) {
        this.layerName = layerName;
        this.layerNumber = layerNumber;
        print("initialize " + this);
    }

    /**
     * 문자열 콘솔 출력 메서드
     *
     * @param str 출력하려는 문자열
     */
    protected synchronized void print(String str) {
        System.out.printf("[%s - %04d] %s\n", this, printLineNumber++, str);
    }

    /**
     * 오류 문자열 콘솔 출력 메서드
     *
     * @param errStr 출력하려는 오류 문자열
     */
    protected synchronized void printError(String errStr) {
        System.err.printf(String.format("[%s - %04d] %s\n", this, printLineNumber++, errStr));
    }

    /**
     * 바이트 배열을 16진수 문자열로 출력하는 메서드
     *
     * @param dataArray 출력하려는 데이터 바이트 배열
     */
    protected synchronized void printHex(byte[] dataArray) {
        String prefix = String.format("[%s - %04d] ", this, printLineNumber++);
        StringBuilder stringBuilder = new StringBuilder();

        for (int index = 0; index < dataArray.length; index++) {
            if (index % 8 == 0) {
                stringBuilder.append(prefix);
            }
            stringBuilder.append(String.format("%02X", dataArray[index]));
            if (index + 1 < dataArray.length) {
                stringBuilder.append((index + 1) % 8 != 0 ? " " : "\n");
            }
        }

        System.out.println(stringBuilder);
    }

    @Override
    public String getLayerName() {
        return this.layerName;
    }

    @Override
    public int getLayerNumber() {
        return this.layerNumber;
    }

    @Override
    public Layer getUpperLayer(String upperLayerName, int layerNumber) {
        return upperLayers.computeIfAbsent(upperLayerName, k -> new HashMap<>()).get(layerNumber);
    }

    @Override
    public List<Layer> getUpperLayerList() {
        List<Layer> upperLayerList = new ArrayList<>();
        upperLayers.forEach((name, layerMap) -> layerMap.forEach((number, layer) -> upperLayerList.add(layer)));
        return upperLayerList;
    }

    @Override
    public Layer getUnderLayer(String underLayerName, int layerNumber) {
        return underLayers.computeIfAbsent(underLayerName, k -> new HashMap<>()).get(layerNumber);
    }

    @Override
    public List<Layer> getUnderLayerList() {
        List<Layer> underLayerList = new ArrayList<>();
        underLayers.forEach((name, layerMap) -> layerMap.forEach((number, layer) -> underLayerList.add(layer)));
        return underLayerList;
    }

    @Override
    public void addUpperLayer(Layer upperLayer) {
        upperLayers.putIfAbsent(upperLayer.getLayerName(), new HashMap<>());
        upperLayers.get(upperLayer.getLayerName()).put(upperLayer.getLayerNumber(), upperLayer);
    }

    @Override
    public void addUnderLayer(Layer underLayer) {
        underLayers.putIfAbsent(underLayer.getLayerName(), new HashMap<>());
        underLayers.get(underLayer.getLayerName()).put(underLayer.getLayerNumber(), underLayer);
    }

    @Override
    public void removeUpperLayer(String upperLayerName, int layerNumber) {
        upperLayers.computeIfPresent(upperLayerName, (key, value) -> {
            value.remove(layerNumber);
            return value;
        });
    }

    @Override
    public void removeUnderLayer(String underLayerName, int layerNumber) {
        underLayers.computeIfPresent(underLayerName, (key, value) -> {
            value.remove(layerNumber);
            return value;
        });
    }

    @Override
    public String toString() {
        return this.layerName + this.layerNumber;
    }
}
