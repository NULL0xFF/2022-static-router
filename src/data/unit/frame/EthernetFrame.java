package data.unit.frame;

import data.address.MACAddress;

import java.util.Arrays;

/**
 * 이더넷 프레임 클래스
 */
public class EthernetFrame implements Frame {

    public static final int MTU = 1500;

    private MACAddress destination;
    private MACAddress source;
    private Type type;
    private byte[] data;

    /**
     * 이더넷 프레임 객체 생성자
     */
    public EthernetFrame() {
        destination = MACAddress.ZERO;
        source = MACAddress.ZERO;
        type = Type.IP;
    }

    /**
     * 이더넷 프레임 객체 생성자
     *
     * @param frame 이더넷 프레임 바이트 배열
     */
    public EthernetFrame(byte[] frame) {
        destination = new MACAddress(Arrays.copyOfRange(frame, 0, 6));
        source = new MACAddress(Arrays.copyOfRange(frame, 6, 12));
        type = new Type(Arrays.copyOfRange(frame, 12, 14));
        data = Arrays.copyOfRange(frame, 14, frame.length);
    }

    /**
     * 수신자 MAC 주소 접근 메서드
     *
     * @return 수신자 MAC 주소 객체
     */
    public MACAddress getDestination() {
        return destination;
    }

    /**
     * 수신자 MAC 주소 설정 메서드
     *
     * @param destination 설정할 수신자 MAC 주소 객체
     */
    public void setDestination(MACAddress destination) {
        this.destination = destination;
    }

    /**
     * 송신자 MAC 주소 접근 메서드
     *
     * @return 송신자 MAC 주소 객체
     */
    public MACAddress getSource() {
        return source;
    }

    /**
     * 송신자 MAC 주소 설정 메서드
     *
     * @param source 설정할 송신자 MAC 주소 객체
     */
    public void setSource(MACAddress source) {
        this.source = source;
    }

    /**
     * 이더넷 타입 접근 메서드
     *
     * @return 접근된 이더넷 타입 객체
     */
    public Type getType() {
        return type;
    }

    /**
     * 이더넷 타입 설정 메서드
     *
     * @param type 설정할 이더넷 타입 객체
     */
    public void setType(Type type) {
        this.type = type;
    }

    public byte[] getData() {
        return data;
    }

    /**
     * 이더넷 프레임 데이터 설정 메서드
     *
     * @param data 설정할 이더넷 프레임 데이터 바이트 배열
     */
    public void setData(byte[] data) {
        if (data == null) {
            this.data = new byte[MTU];
        } else if (data.length < 46) {
            this.data = new byte[46];
            System.arraycopy(data, 0, this.data, 0, data.length);
        } else if (MTU < data.length) {
            this.data = new byte[MTU];
            System.arraycopy(data, 0, this.data, 0, MTU);
        } else {
            this.data = data;
        }
    }

    /**
     * 데이터 유닛의 총 바이트 길이를 반환하는 메서드
     *
     * @return 데이터 유닛의 총 바이트 길이
     */
    @Override
    public int length() {
        int offset = ((data != null) ? data.length : 0);
        // 헤더 길이 + 데이터 길이 (최소 46 바이트)
        return 14 + Math.max(offset, 46);
    }

    /**
     * 데이터를 캡슐화 하여 바이트 배열로 반환하는 메서드
     *
     * @return 캡슐화된 데이터 바이트 배열
     */
    @Override
    public byte[] toBytes() {
        byte[] frame = new byte[length()];

        System.arraycopy(destination.toBytes(), 0, frame, 0, 6);
        System.arraycopy(source.toBytes(), 0, frame, 6, 6);
        System.arraycopy(type.toBytes(), 0, frame, 12, 2);
        System.arraycopy(data, 0, frame, 14, data.length);

        return frame;
    }

    public static class Type {

        public static final Type IP = new Type(0x0800);
        public static final Type ARP = new Type(0x0806);

        private final int type;

        public Type(int type) {
            this.type = type;
        }

        public Type(byte[] type) {
            this.type = (type[0] << 8) | type[1];
        }

        public int value() {
            return type;
        }

        public byte[] toBytes() {
            byte[] type = new byte[2];

            type[0] = (byte) ((this.type >>> 8) & 0xFF);
            type[1] = (byte) (this.type & 0xFF);

            return type;
        }

        @Override
        public int hashCode() {
            return type;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Type object && type == object.type;
        }

    }
}
