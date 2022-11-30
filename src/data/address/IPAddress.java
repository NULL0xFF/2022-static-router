package data.address;

import java.util.Arrays;

/**
 * IP 주소 클래스
 */
public class IPAddress implements Address {

    /**
     * 빈 IP 주소 객체
     */
    public static final IPAddress ZERO = new IPAddress("0.0.0.0");
    public static final IPAddress BROADCAST = new IPAddress("255.255.255.255");

    private final byte[] address = new byte[4];

    /**
     * IP 주소 객체 생성자
     *
     * @param address IP 주소 문자열
     */
    public IPAddress(String address) {
        // 주소 문자열 나누기
        String[] array = address.split("\\.");

        // 문자열 유효성 확인
        if (array.length != 4 || address.length() != (Arrays.stream(array).mapToInt(String::length).sum() + 3)) {
            throw new RuntimeException("illegal address string");
        }

        for (int index = 0; index < array.length; index++) {
            int parsed = Integer.parseInt(array[index]);

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
     * IP 주소 객체 생성자
     *
     * @param address IP 주소 바이트 배열
     */
    public IPAddress(byte[] address) {
        // 바이트 배열 유효성 확인
        if (address.length != 4) {
            throw new RuntimeException("illegal address array");
        }

        // 바이트 배열화 된 IP 주소를 객체화
        System.arraycopy(address, 0, this.address, 0, 4);
    }

    public boolean isNetmask() {
        for (int index = 3; index >= 0; index--) {
            for (int shift = 0; shift < 8; shift++) {
                if (((address[index] >> shift) & 0x01) == 0x01) {
                    // Network 주소 체크됨
                    for (int checkShift = shift; checkShift < 8; checkShift++) {
                        if (((address[index] >> checkShift & 0x01) == 0x00)) {
                            return false;
                        }
                    }
                    for (int checkIndex = index - 1; checkIndex >= 0; checkIndex--) {
                        for (int checkShift = 0; checkShift < 8; checkShift++) {
                            if (((address[checkIndex] >> checkShift & 0x01) == 0x00)) {
                                return false;
                            }
                        }
                    }
                    return true;
                }
            }
        }
        return true;
    }

    public IPAddress toNetwork(IPAddress netmask) {
        byte[] destAddr = new byte[4];
        destAddr[0] = (byte) (this.address[0] & netmask.address[0]);
        destAddr[1] = (byte) (this.address[1] & netmask.address[1]);
        destAddr[2] = (byte) (this.address[2] & netmask.address[2]);
        destAddr[3] = (byte) (this.address[3] & netmask.address[3]);
        return new IPAddress(destAddr);
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
     * IP 주소 객체 비교를 위한 hashCode 생성 메서드
     * 내부 변수인 byte[] address 의 내용을 기반으로 한 hashCode를 생성한다
     *
     * @return IP 주소 hashCode
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(address);
    }

    /**
     * IP 주소 객체 비교 메서드
     *
     * @return 비교 결과 값
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof IPAddress object && Arrays.equals(this.address, object.address);
    }

    /**
     * IP 주소 문자열을 반환하는 메서드
     *
     * @return IP 주소 문자열
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < address.length; index++) {
            builder.append(Byte.toUnsignedInt(address[index]));
            if (index + 1 < address.length) {
                builder.append('.');
            }
        }
        return builder.toString();
    }
}
