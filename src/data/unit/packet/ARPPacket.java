package data.unit.packet;

import data.address.IPAddress;
import data.address.MACAddress;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class ARPPacket implements Packet {

    private short hardware;
    private short protocol;
    private byte hardwareLength;
    private byte protocolLength;
    private Operation operation;
    private MACAddress sourceMAC;
    private IPAddress sourceIP;
    private MACAddress destinationMAC;
    private IPAddress destinationIP;

    /**
     * ARP 패킷 객체 생성자
     */
    public ARPPacket() {
        hardware = 1; // Ethernet
        protocol = 0x0800; // IP
        hardwareLength = 6; // MAC address length
        protocolLength = 4; // IP address length
        operation = Operation.REQUEST;
        sourceMAC = MACAddress.ZERO;
        sourceIP = IPAddress.ZERO;
        destinationMAC = MACAddress.ZERO;
        destinationIP = IPAddress.ZERO;
    }

    /**
     * ARP 패킷 객체 생성자
     *
     * @param packet ARP 패킷의 바이트 배열
     */
    public ARPPacket(byte[] packet) {
        hardware = ByteBuffer.wrap(Arrays.copyOfRange(packet, 0, 2)).getShort();
        protocol = ByteBuffer.wrap(Arrays.copyOfRange(packet, 2, 4)).getShort();
        hardwareLength = packet[4];
        protocolLength = packet[5];
        operation = new Operation(Arrays.copyOfRange(packet, 6, 8));
        sourceMAC = new MACAddress(Arrays.copyOfRange(packet, 8, 14));
        sourceIP = new IPAddress(Arrays.copyOfRange(packet, 14, 18));
        destinationMAC = new MACAddress(Arrays.copyOfRange(packet, 18, 24));
        destinationIP = new IPAddress(Arrays.copyOfRange(packet, 24, 28));
    }

    /**
     * 하드웨어 타입 접근 메소드
     *
     * @return 하드웨어 타입
     */
    public short getHardware() {
        return hardware;
    }

    /**
     * 하드웨어 타입 설정 메소드
     *
     * @param hardware 하드웨어 타입
     */
    public void setHardware(short hardware) {
        this.hardware = hardware;
    }

    /**
     * 프로토콜 타입 접근 메소드
     *
     * @return 프로토콜 타입
     */
    public short getProtocol() {
        return protocol;
    }

    /**
     * 프로토콜 타입 설정 메소드
     *
     * @param protocol 프로토콜 타입
     */
    public void setProtocol(short protocol) {
        this.protocol = protocol;
    }

    /**
     * 하드웨어 주소의 길이 접근 메소드
     *
     * @return 하드웨어 주소의 길이
     */
    public byte getHardwareLength() {
        return hardwareLength;
    }

    /**
     * 하드웨어 주소의 길이 설정 메소드
     *
     * @param hardwareLength 하드웨어 주소의 길이
     */
    public void setHardwareLength(byte hardwareLength) {
        this.hardwareLength = hardwareLength;
    }

    /**
     * 프로토콜 주소의 길이 접근 메소드
     *
     * @return 프로토콜 주소의 길이
     */
    public byte getProtocolLength() {
        return protocolLength;
    }

    /**
     * 프로토콜 주소의 길이 설정 메소드
     *
     * @param protocolLength 프로토콜 주소의 길이
     */
    public void setProtocolLength(byte protocolLength) {
        this.protocolLength = protocolLength;
    }

    /**
     * 동작 객체의 접근 메소드
     *
     * @return 동작 객체
     */
    public Operation getOperation() {
        return operation;
    }

    /**
     * 동작 객체의 설정 메소드
     *
     * @param operation 동작 객체
     */
    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    /**
     * 출발지 MAC 주소의 접근 메소드
     *
     * @return 출발지 MAC 주소
     */
    public MACAddress getSourceMAC() {
        return sourceMAC;
    }

    /**
     * 출발지 MAC 주소의 설정 메소드
     *
     * @param sourceMAC 출발지 MAC 주소
     */
    public void setSourceMAC(MACAddress sourceMAC) {
        this.sourceMAC = sourceMAC;
    }

    /**
     * 출발지 IP 주소의 접근 메소드
     *
     * @return 출발지 IP 주소
     */
    public IPAddress getSourceIP() {
        return sourceIP;
    }

    /**
     * 출발지 IP 주소의 설정 메소드
     *
     * @param sourceIP 출발지 IP 주소
     */
    public void setSourceIP(IPAddress sourceIP) {
        this.sourceIP = sourceIP;
    }

    /**
     * 도착지 MAC 주소의 접근 메소드
     *
     * @return 도착지 MAC 주소
     */
    public MACAddress getDestinationMAC() {
        return destinationMAC;
    }

    /**
     * 도착지 MAC 주소의 설정 메소드
     *
     * @param destinationMAC 도착지 MAC 주소
     */
    public void setDestinationMAC(MACAddress destinationMAC) {
        this.destinationMAC = destinationMAC;
    }

    /**
     * 도착지 IP 주소의 접근 메소드
     *
     * @return 도착지 IP 주소
     */
    public IPAddress getDestinationIP() {
        return destinationIP;
    }

    /**
     * 도착지 IP 주소의 설정 메소드
     *
     * @param destinationIP 도착지 IP 주소
     */
    public void setDestinationIP(IPAddress destinationIP) {
        this.destinationIP = destinationIP;
    }

    /**
     * 데이터 유닛의 총 바이트 길이를 반환하는 메서드
     *
     * @return 데이터 유닛의 총 바이트 길이
     */
    @Override
    public int length() {
        return 28;
    }

    /**
     * 데이터를 캡슐화 하여 바이트 배열로 반환하는 메서드
     *
     * @return 캡슐화된 데이터 바이트 배열
     */
    @Override
    public byte[] toBytes() {
        byte[] packet = new byte[length()];

        System.arraycopy(ByteBuffer.allocate(2).putShort(hardware).array(), 0, packet, 0, 2);
        System.arraycopy(ByteBuffer.allocate(2).putShort(protocol).array(), 0, packet, 2, 2);
        packet[4] = hardwareLength;
        packet[5] = protocolLength;
        System.arraycopy(operation.toBytes(), 0, packet, 6, 2);
        System.arraycopy(sourceMAC.toBytes(), 0, packet, 8, 6);
        System.arraycopy(sourceIP.toBytes(), 0, packet, 14, 4);
        System.arraycopy(destinationMAC.toBytes(), 0, packet, 18, 6);
        System.arraycopy(destinationIP.toBytes(), 0, packet, 24, 4);

        return packet;
    }

    public static class Operation {

        public static final Operation REQUEST = new Operation((short) 1);
        public static final Operation REPLY = new Operation((short) 2);

        private final short type;

        public Operation(short type) {
            this.type = type;
        }

        public Operation(byte[] type) {
            this.type = ByteBuffer.wrap(type).getShort();
        }

        public short value() {
            return type;
        }

        public byte[] toBytes() {
            return ByteBuffer.allocate(2).putShort(type).array();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Operation object && type == object.type;
        }

    }
}
