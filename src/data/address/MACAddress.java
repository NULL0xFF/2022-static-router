package data.address;

import java.util.Arrays;

public class MACAddress implements Address {

    public static final MACAddress ZERO = new MACAddress("00:00:00:00:00:00");
    public static final MACAddress BROADCAST = new MACAddress("FF:FF:FF:FF:FF:FF");

    private final byte[] address = new byte[6];

    /**
     * MAC 주소 객체 생성자
     *
     * @param address MAC 주소 문자열
     */
    public MACAddress(String address) {
        // 주소 문자열 나누기
        String[] array = address.split(":");

        // 문자열 유효성 확인
        if (array.length != 6 || address.length() != (Arrays.stream(array).mapToInt(String::length).sum() + 5)) {
            throw new RuntimeException("illegal address string");
        }

        for (int index = 0; index < array.length; index++) {
            // MAC 주소는 16진수 형태로 파싱
            int parsed = Integer.parseInt(array[index], 16);

            // 문자열의 숫자 Parse 유효성 확인
            if (parsed < 0 || parsed > 255) {
                // 지원하는 주소의 범위를 넘음
                throw new RuntimeException("out of range");
            }

            // 주소 저장
            this.address[index] = (byte) parsed;
        }
    }

    /**
     * MAC 주소 객체 생성자
     *
     * @param address MAC 주소 바이트 배열
     */
    public MACAddress(byte[] address) {
        // 바이트 배열화 된 MAC 주소를 객체화
        System.arraycopy(address, 0, this.address, 0, this.address.length);
    }

    /**
     * 데이터의 총 바이트 길이를 반환하는 메서드
     *
     * @return 데이터의 총 바이트 길이
     */
    @Override
    public int length() {
        return address.length;
    }

    /**
     * 데이터를 바이트 배열로 반환하는 메서드
     *
     * @return 데이터의 바이트 배열
     */
    @Override
    public byte[] toBytes() {
        return address;
    }

    /**
     * MAC 주소 객체 비교를 위한 hashCode 생성 메서드
     * 내부 변수인 byte[] address 의 내용을 기반으로 한 hashCode를 생성한다
     *
     * @return MAC 주소 hashCode
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(address);
    }

    /**
     * MAC 주소 객체 비교 메서드
     *
     * @return 비교 결과 값
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof MACAddress object && Arrays.equals(this.address, object.address);
    }

    /**
     * MAC 주소 문자열을 반환하는 메서드
     *
     * @return MAC 주소 문자열
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < address.length; index++) {
            // 16진수 대문자 형태의 값으로 변환
            int unsignedInt = Byte.toUnsignedInt(address[index]);
            builder.append(String.format("%02X", unsignedInt));
            if (index + 1 < address.length) {
                builder.append(':');
            }
        }
        return builder.toString();
    }
}
