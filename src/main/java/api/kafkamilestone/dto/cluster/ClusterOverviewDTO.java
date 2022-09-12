package api.kafkamilestone.dto.cluster;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter 
@Setter 
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ClusterOverviewDTO {

	/* 브로커 정보 */
	private Integer brokers;
	private double outGoingByte;
	private double inComingByte;
	
	/* 토픽 정보 */
	private Integer topics;
	private Long partitions;
	private Integer outOfSyncReplicas;
	
	/* 매트릭 정보 */
	private String healthState;
	private String availableMetrics;
}
