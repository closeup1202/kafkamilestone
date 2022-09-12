package api.kafkamilestone.utils;

/**
 * 브로커의 매트릭 정보 중 outgoing-byte-rate | incoming-byte-rate 의 string 을 상수화
 */
public interface ClusterIOValues {
	
	static final String OUTGOING_BYTE_RATE = "outgoing-byte-rate";
	static final String INCOMING_BYTE_RATE = "incoming-byte-rate";

}
