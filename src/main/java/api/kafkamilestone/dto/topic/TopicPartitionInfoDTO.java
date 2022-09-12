package api.kafkamilestone.dto.topic;


import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 카프카의 내부 객체 NODE를 직접적으로 사용 불가 {@link Node} 
 * Node는 TopicPartitionInfo의 필드 멤버이기 때문에 TopicPartitionInfo도 직접적인 사용 불가능 {@link TopicPartitionInfo}
 * 따라서 TopicPartitionInfo를 재구성 및 필요한 데이터만 추출하는 DTO
 * 이 DTO는 TopicRetrieveDTO의 필드 멤버 {@link TopicRetrieveDTO}
 * 
 *  TopicDescription {@link TopicDescription} 
 * 		   |
 * TopicPartitionInfo 
 * 		   |
 *        Node 
 */

@Getter 
@Setter 
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicPartitionInfoDTO{

	private Integer partition;
	private String leader;
	private String replicas;
	private String isr;
}
