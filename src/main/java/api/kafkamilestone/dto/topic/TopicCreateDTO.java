package api.kafkamilestone.dto.topic;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.springframework.lang.Nullable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter 
@Setter 
@ToString@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopicCreateDTO {

	@NotEmpty
	private String topicName;
	
	@NotNull
	private Integer partitions;
	
	@NotNull
	private Short replication;
	
	@Nullable
	private String cleanupPolicy;
	
	@Nullable
    private String compressionType;
	
	@Nullable
    private String deleteRetention;
	
	@Nullable
    private String fileDeleteDelay;
	
	@Nullable
    private String maxMessage;
	
	@Nullable
    private String segment;

	@Nullable
    private String minIsr;

	@NotEmpty
    private String servers;
}
