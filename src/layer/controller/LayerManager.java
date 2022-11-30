package layer.controller;


import layer.Layer;

import java.util.*;

/**
 * 계층 관리 클래스
 */
public class LayerManager {

    private static final LayerManager instance = new LayerManager();
    private final Map<String, Map<Integer, Layer>> layers = new HashMap<>();

    public static LayerManager getInstance() {
        return instance;
    }

    /**
     * @deprecated
     */
    public Layer get(String name) {
        return get(name, 0);
    }

    /**
     * 계층 접근 메서드
     */
    public Layer get(String name, int number) {
        return layers.computeIfAbsent(name, k -> new HashMap<>()).get(number);
    }

    public List<Layer> getList(String name) {
        return new ArrayList<>(layers.computeIfAbsent(name, k -> new HashMap<>()).values());
    }

    public Layer put(Layer layer) {
        return put(layer, 0);
    }

    /**
     * 계층 추가 메서드
     */
    public Layer put(Layer layer, int number) {
        return layers.computeIfAbsent(layer.getLayerName(), k -> new HashMap<>()).put(number, layer);
    }

    /**
     * 계층 간 연결 설정 메서드
     * <p>
     * 입력받은 문자열을 공백 문자를 기준으로 파싱해서 계층 간의 연결을 설정하는 메서드
     * <p>
     * 첫 문자열은 계층 이름, 괄호는 계층의 상/하위 단계 의미
     * 이후 모든 문자열은 상/하위 계층과의 관계를 의미하는 mode 문자와 계층 문자열을 가짐
     *
     * @param layerListString 계층 간의 연결을 의미하는 문자열
     */
    public void connectLayers(String layerListString) {
        StringTokenizer tokenizer = new StringTokenizer(layerListString, " ");
        Stack<Layer> layerStack = new Stack<>();
        Layer layer = null;

        while (tokenizer.hasMoreTokens()) {
            if (layer == null) {
                // 기반 레이어 설정
                String token = tokenizer.nextToken();
                String layerName = token.replaceAll("[0-9]", "");
                int layerNumber = Integer.parseInt(token.replaceAll("[^0-9]", ""));
                layer = get(layerName, layerNumber);
            } else {
                String token = tokenizer.nextToken();
                switch (token) {
                    case "(" ->
                        // 레이어 단계 증가
                            layerStack.push(layer);
                    case ")" ->
                        // 레이어 단계 감소
                            layerStack.pop();
                    default -> {
                        String layerToken = token.substring(1);
                        String layerName = layerToken.replaceAll("[0-9]", "");
                        int layerNumber = Integer.parseInt(layerToken.replaceAll("[^0-9]", ""));
                        char mode = token.charAt(0);
                        layer = get(layerName, layerNumber);
                        switch (mode) {
                            case '+' -> {
                                // Stack 상단 레이어의 위에 현재 layer 추가
                                layerStack.peek().addUpperLayer(layer);
                                layer.addUnderLayer(layerStack.peek());
                            }
                            case '-' -> {
                                // Stack 상단 레이어의 아래에 현재 layer 추가
                                layer.addUpperLayer(layerStack.peek());
                                layerStack.peek().addUnderLayer(layer);
                            }
                            default ->
                                // 유효하지 않은 문자열의 경우 Exception 발생
                                    throw new RuntimeException("layer list parse failed");
                        }
                    }
                }
            }
        }
    }

}
