package api.kafkamilestone.dto.topic;

import java.util.List;

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
public class TopicModifyConfigsDTO {

	@NotEmpty
	private String topicName;
	private List<TopicConfigKV> keyValueList;
	
	@NotEmpty
	private String servers;
}
