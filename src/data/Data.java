package data;

public interface Data {

    /**
     * 데이터의 총 바이트 길이를 반환하는 메서드
     *
     * @return 데이터의 총 바이트 길이
     */
    int length();

    /**
     * 데이터를 바이트 배열로 반환하는 메서드
     *
     * @return 데이터의 바이트 배열
     */
    byte[] toBytes();
}
