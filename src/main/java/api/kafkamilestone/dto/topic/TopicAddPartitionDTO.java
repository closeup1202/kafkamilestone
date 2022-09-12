package api.kafkamilestone.dto.topic;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import api.kafkamilestone.entity.Active;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopicAddPartitionDTO {
	
	@NotEmpty
	private String topicName;
	
	@NotNull
	private Integer addCount;
	
	@NotEmpty
    private String servers;

}
