package api.kafkamilestone.dto.topic;

import java.util.List;

import org.apache.kafka.common.Node;

import api.kafkamilestone.entity.Active;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Node는 TopicPartitionInfo의 필드 멤버 {@link TopicPartitionInfo}
 * 카프카의 내부 객체 Node를 직접적으로 사용 불가 {@link Node}
 * 따라서 이는 그와 같은 내부 객체를 사용하기 위한 DTO
 * 이 DTO로 재구성된 Topic의 replicas 데이터를 TopicRetrieveDTO가 받음 {@link TopicRetrieveDTO}
 * TopicDTO에서 받은 데이터를 List로 감싸는 것이 TopicPartitionInfoDTO {@link TopicPartitionInfoDTO}
 * 
 *  TopicDescription {@link TopicDescription} 
 * 		   |
 * TopicPartitionInfo 
 * 		   |
 *        Node 
 */

@NoArgsConstructor
@AllArgsConstructor
@Getter 
@Setter 
@ToString
@Builder
public class NodeDTO {
	private int id;
	private String idString;
	private String host;
	private int port;
	
	private String isrListStr;
	private String replicasListStr;
	
	public NodeDTO(List<Node> nodeDetail) {
		nodeDetail.forEach(node -> this.setIdString(node.idString()));
		nodeDetail.forEach(node -> this.setId(node.id()));
		nodeDetail.forEach(node -> this.setHost(node.host()));
		nodeDetail.forEach(node -> this.setPort(node.port()));
	}
	
	public NodeDTO(List<Node> replAndIsrListById, NodeType type) {
		StringBuilder sb = new StringBuilder();
		replAndIsrListById.forEach(i -> sb.append(i.id() + ","));
		String result = sb.toString();
		
		if(type == NodeType.I) {
			this.isrListStr = result.substring(0, result.lastIndexOf(","));
		} else {
			this.replicasListStr = result.substring(0, result.lastIndexOf(","));	
		}
	}

	enum NodeType {
		R, 
		I;
	}
}
