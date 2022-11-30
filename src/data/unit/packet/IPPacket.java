package data.unit.packet;

import data.address.IPAddress;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class IPPacket implements Packet {

    private byte version;
    private byte headerLength;
    private Service serviceType;
    private short totalLength;
    private short identification;
    private boolean dontFragment;
    private boolean moreFragment;
    private short fragmentationOffset;
    private byte timeToLive;
    private Protocol protocol;
    private short headerChecksum;
    private IPAddress source;
    private IPAddress destination;
    private byte[] data;

    public IPPacket() {
        version = 4; // IPv4
        headerLength = 5; // 4의 배수로 계산되는 헤더 길이, 옵션이 없으므로 총 20 바이트
        serviceType = new Service(); // 서비스 타입
        totalLength = 20; // 헤더와 데이터를 포함한 총 길이
        identification = 0; // 각 IP 데이터그램을 구별하기 위해 추가되었지만 현재 사용하지 않는 필드
        dontFragment = false;
        moreFragment = false;
        fragmentationOffset = 0;
        timeToLive = 64;
        protocol = Protocol.ZERO;
        headerChecksum = 0;
        source = IPAddress.ZERO;
        destination = IPAddress.ZERO;
        data = new byte[0];
    }

    public IPPacket(byte[] packet) {
        version = (byte) ((packet[0] >> 4) & 0xF);
        headerLength = (byte) (packet[0] & 0xF);
        serviceType = new Service(packet[1]);
        totalLength = ByteBuffer.wrap(Arrays.copyOfRange(packet, 2, 4)).getShort();
        identification = ByteBuffer.wrap(Arrays.copyOfRange(packet, 4, 6)).getShort();
        dontFragment = ((packet[6] >> 6) & 0x1) == 0x1;  // 6번 패킷의 7번째 비트
        moreFragment = ((packet[6] >> 5) & 0x1) == 0x1;  // 6번 패킷의 6번째 비트
        fragmentationOffset = (short) (ByteBuffer.wrap(Arrays.copyOfRange(packet, 6, 8)).getShort() & 0x1F_FF);  // 하위 13바이트를 사용
        timeToLive = packet[8];
        protocol = new Protocol(packet[9]);
        headerChecksum = ByteBuffer.wrap(Arrays.copyOfRange(packet, 10, 12)).getShort();
        source = new IPAddress(Arrays.copyOfRange(packet, 12, 16));
        destination = new IPAddress(Arrays.copyOfRange(packet, 16, 20));
        data = ByteBuffer.wrap(Arrays.copyOfRange(packet, 20, packet.length)).array();
    }

    public byte getVersion() {
        return version;
    }

    public void setVersion(byte version) {
        this.version = version;
    }

    public byte getHeaderLength() {
        return headerLength;
    }

    public void setHeaderLength(byte headerLength) {
        this.headerLength = headerLength;
    }

    public Service getServiceType() {
        return serviceType;
    }

    public void setServiceType(Service serviceType) {
        this.serviceType = serviceType;
    }

    public short getTotalLength() {
        return totalLength;
    }

    public void setTotalLength(short totalLength) {
        this.totalLength = totalLength;
    }

    public short getIdentification() {
        return identification;
    }

    public void setIdentification(short identification) {
        this.identification = identification;
    }

    public boolean isDontFragment() {
        return dontFragment;
    }

    public void setDontFragment(boolean dontFragment) {
        this.dontFragment = dontFragment;
    }

    public boolean isMoreFragment() {
        return moreFragment;
    }

    public void setMoreFragment(boolean moreFragment) {
        this.moreFragment = moreFragment;
    }

    public short getFragmentationOffset() {
        return fragmentationOffset;
    }

    public void setFragmentationOffset(short fragmentationOffset) {
        this.fragmentationOffset = fragmentationOffset;
    }

    public byte getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(byte timeToLive) {
        this.timeToLive = timeToLive;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public short getHeaderChecksum() {
        return headerChecksum;
    }

    public void setHeaderChecksum(short headerChecksum) {
        this.headerChecksum = headerChecksum;
    }

    public IPAddress getSource() {
        return source;
    }

    public void setSource(IPAddress source) {
        this.source = source;
    }

    public IPAddress getDestination() {
        return destination;
    }

    public void setDestination(IPAddress destination) {
        this.destination = destination;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    /**
     * 데이터 유닛의 총 바이트 길이를 반환하는 메서드
     *
     * @return 데이터 유닛의 총 바이트 길이
     */
    @Override
    public int length() {
        return 20 + data.length;
    }

    /**
     * 데이터를 캡슐화 하여 바이트 배열로 반환하는 메서드
     *
     * @return 캡슐화된 데이터 바이트 배열
     */
    @Override
    public byte[] toBytes() {
        byte[] packet = new byte[length()];

        packet[0] = (byte) (version << 4 | 0x05);
        packet[1] = serviceType.toBytes()[0];
        System.arraycopy(ByteBuffer.allocate(4).putInt(length()).array(), 2, packet, 2, 2);
        System.arraycopy(ByteBuffer.allocate(2).putShort(identification).array(), 0, packet, 4, 2);
        packet[6] = (byte) (((dontFragment ? 1 : 0) << 6) | ((moreFragment ? 1 : 0) << 5) | (fragmentationOffset & 0x1F));
        packet[7] = (byte) (fragmentationOffset & 0xFF);
        packet[8] = timeToLive;
        packet[9] = protocol.value();
        System.arraycopy(ByteBuffer.allocate(2).putShort(headerChecksum).array(), 0, packet, 10, 2);
        System.arraycopy(source.toBytes(), 0, packet, 12, 4);
        System.arraycopy(destination.toBytes(), 0, packet, 16, 4);

        if (data != null) {
            System.arraycopy(data, 0, packet, 20, length() - 20);
        }

        return packet;
    }

    public static class Service {

        private byte type;
        private boolean delay = false;
        private boolean throughput = false;
        private boolean reliability = false;
        private boolean minimumCost = false;

        public Service() {
            setType(Type.ROUTINE);
        }

        public Service(byte type) {
            setType(type);
        }

        public Service(Type type) {
            setType(type);
        }

        public int getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = switch (type) {
                case ROUTINE -> 0;
                case PRIORITY -> 1;
                case IMMEDIATE -> 2;
                case FLASH -> 3;
                case FLASH_OVERRIDE -> 4;
                case CRITICAL -> 5;
                case INTERNET_CONTROL -> 6;
                case NETWORK_CONTROL -> 7;
            };
        }

        public void setType(byte type) {
            this.type = type;
        }

        /**
         * @return {@code true} for high delay, {@code false} for normal delay
         */
        public boolean isDelayed() {
            return delay;
        }

        public void setDelay(boolean delay) {
            this.delay = delay;
        }

        /**
         * @return {@code true} for high throughput, {@code false} for normal throughput
         */
        public boolean isHighThroughput() {
            return throughput;
        }

        public void setThroughput(boolean throughput) {
            this.throughput = throughput;
        }

        /**
         * @return {@code true} for high reliability, {@code false} for normal reliability
         */
        public boolean isReliable() {
            return reliability;
        }

        public void setReliability(boolean reliability) {
            this.reliability = reliability;
        }

        /**
         * @return {@code true} for low cost, {@code false} for normal cost
         */
        public boolean isMinimumCost() {
            return minimumCost;
        }

        public void setMinimumCost(boolean minimumCost) {
            this.minimumCost = minimumCost;
        }

        public byte[] toBytes() {
            byte[] bytes = new byte[1];
            bytes[0] = (byte) (((type & 0x7) << 5) | ((delay ? 1 : 0) << 4) | ((throughput ? 1 : 0) << 3) | ((reliability ? 1 : 0) << 2) | ((minimumCost ? 1 : 0) << 1));
            return bytes;
        }

        public enum Type {
            ROUTINE, PRIORITY, IMMEDIATE, FLASH, FLASH_OVERRIDE, CRITICAL, INTERNET_CONTROL, NETWORK_CONTROL
        }

    }

    public static class Protocol {

        public static final IPPacket.Protocol ZERO = new IPPacket.Protocol((byte) 0);
        public static final IPPacket.Protocol ICMP = new IPPacket.Protocol((byte) 1);
        public static final IPPacket.Protocol IGMP = new IPPacket.Protocol((byte) 2);
        public static final IPPacket.Protocol TCP = new IPPacket.Protocol((byte) 6);
        public static final IPPacket.Protocol UDP = new IPPacket.Protocol((byte) 17);

        private final byte type;

        public Protocol(byte type) {
            this.type = type;
        }

        public byte value() {
            return type;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof IPPacket.Protocol object && type == object.type;
        }

    }
}
