package api.kafkamilestone.dto.cluster;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;

import api.kafkamilestone.entity.Active;
import api.kafkamilestone.entity.Broker;
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
public class ClusterDTO {
	
	private Long id;
	
	@NotEmpty
	private String clusterName;
	
	@NotEmpty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String servers;
	
	@Size(max = 7)
	private String version;
	
	private Active active;
	
	public ClusterDTO(final Cluster cluster) { 
		this.id = cluster.getId();
		this.clusterName = cluster.getClusterName();
		this.servers = toStringServers(cluster.getServers());
		this.version = cluster.getVersion();
		this.active = cluster.getActive();
	}
	
	public static Cluster toCluster(final ClusterDTO dto) {
		return Cluster.builder()
					.clusterName(dto.getClusterName())
					.version(dto.getVersion())
					.active(dto.getActive())
					.build();
	}
	
	
	/**
	 * @see #toBrokerList: String convert to List<Broker> 
	 * @return List<Broker> server1 | server2 | server3 
	 */
	public static List<Broker> toBrokerList(String servers) {
		servers = StringUtils.trimAllWhitespace(servers);
		return Arrays.stream(servers.split(","))
					 .collect(Collectors.toList())
					 .stream()
					 .map(Broker::toBrokerServer)
					 .collect(Collectors.toList());
	}
	
	/**
	 * @param List<Broker> servers
	 * @return String servers
	 */
	public static String toStringServers(List<Broker> servers) {
		StringBuilder sb = new StringBuilder();
		servers.stream().map(Broker::getServer).forEach(server -> sb.append(server + ','));
		String result = sb.toString();
		return result.substring(0, result.length()-1);
	}
}
