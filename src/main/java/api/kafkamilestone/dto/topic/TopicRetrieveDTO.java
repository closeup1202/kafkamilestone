package api.kafkamilestone.dto.topic;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.kafka.common.TopicPartitionInfo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Getter 
@Setter
@ToString
@Builder
public class TopicRetrieveDTO {
	
	private Integer id;
	
    private String name;
	
    private boolean internal;
    
    private List<TopicPartitionInfoDTO> partitionInfo;
    
    private String cleanupPolicy;
    
    private Long partitionCount;
    
    private Long totalPartitionCount;
    
    public static List<TopicPartitionInfoDTO> toConvertPartition(List<TopicPartitionInfo> partitions) {
    	return partitions.stream().map(p -> TopicPartitionInfoDTO.builder()
						    									 .partition(p.partition())
						    									 .replicas(new NodeDTO(p.replicas()).getIdString())
						    									 .build())
		    					  .collect(Collectors.toList());
    }
}
