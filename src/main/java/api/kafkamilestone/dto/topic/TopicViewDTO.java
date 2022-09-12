package api.kafkamilestone.dto.topic;

import static api.kafkamilestone.dto.topic.NodeDTO.NodeType.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.kafka.common.TopicPartitionInfo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter 
@Setter 
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopicViewDTO {
	
	private String topicName;
	private List<TopicPartitionInfoDTO> partitionInfo; /* partition | leader | replicas | isr */
	private Long partitionCount;
	private Long offsetsSum;
	private Map<String, String> configs;
	
    public static List<TopicPartitionInfoDTO> toConvertPartition(List<TopicPartitionInfo> partitions) {
    	return partitions.stream().map(p -> TopicPartitionInfoDTO.builder()
						    									 .partition(p.partition())
						    									 .leader(p.leader().idString())
						    									 .replicas(new NodeDTO(p.replicas(), R).getReplicasListStr())
						    									 .isr(new NodeDTO(p.isr(), I).getIsrListStr())
						    									 .build())
		    					  .collect(Collectors.toList());
    }
	
}
