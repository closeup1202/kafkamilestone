package api.kafkamilestone.dto.topic;

import javax.validation.constraints.NotEmpty;

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
public class TopicModifyReplicasDTO {
	
	@NotEmpty
	private String topicName;
	private int replication;
	
	@NotEmpty
	private String servers;
}
