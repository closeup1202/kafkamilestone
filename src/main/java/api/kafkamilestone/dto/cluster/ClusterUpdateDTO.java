package api.kafkamilestone.dto.cluster;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import api.kafkamilestone.entity.Active;
import api.kafkamilestone.entity.Cluster;
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
public class ClusterUpdateDTO {

	private Long id;
	private String clusterName;
	
	@NotEmpty
	private String servers;
	
	@Size(max = 7)
	private String version;
	private Active active;
	
	public static Cluster toCluster(final ClusterUpdateDTO dto) {
		return Cluster.builder()
					.id(dto.getId())
					.clusterName(dto.getClusterName())
					.version(dto.getVersion())
					.active(dto.getActive())
					.build();
	}
}
