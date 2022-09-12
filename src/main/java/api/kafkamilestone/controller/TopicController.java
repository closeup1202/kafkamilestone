package api.kafkamilestone.controller;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.validation.Valid;

import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import api.kafkamilestone.aop.Trace;
import api.kafkamilestone.dto.response.ResponseDTO;
import api.kafkamilestone.dto.topic.TopicAddPartitionDTO;
import api.kafkamilestone.dto.topic.TopicCreateDTO;
import api.kafkamilestone.dto.topic.TopicModifyConfigsDTO;
import api.kafkamilestone.dto.topic.TopicModifyLeaderDTO;
import api.kafkamilestone.dto.topic.TopicModifyReplicasDTO;
import api.kafkamilestone.dto.topic.TopicRetrieveDTO;
import api.kafkamilestone.dto.topic.TopicViewDTO;
import api.kafkamilestone.factory.KafkaFactory;
import api.kafkamilestone.service.TopicService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TopicController {

	private final TopicService topicService;
	private final KafkaFactory factory;
	
	@GetMapping("topics")
	public ResponseEntity<?> retrieveTopics(final String servers){
		
		AdminClient client = factory.getAdminClients().get(servers);
		
		List<TopicRetrieveDTO> dtos = topicService.retrieveAll(client);

		Long totalPartitionCount = dtos.stream().mapToLong(TopicRetrieveDTO::getPartitionCount).sum();
		
		TopicRetrieveDTO dto = new TopicRetrieveDTO();
		dto.setTotalPartitionCount(totalPartitionCount);
		
		ResponseDTO<TopicRetrieveDTO> response = ResponseDTO.<TopicRetrieveDTO>builder().data(dto).datas(dtos).build();
		
		return ResponseEntity.ok().body(response);
	}
	
	@Trace
	@PostMapping("createTopic")
	public ResponseEntity<?> createTopic(final @Valid @RequestBody TopicCreateDTO dto) {
		
		AdminClient client = factory.getAdminClients().get(dto.getServers());
		
		TopicCreateDTO reponseDTO = topicService.create(dto, client);
		
		ResponseDTO<TopicCreateDTO> response = ResponseDTO.<TopicCreateDTO>builder().data(reponseDTO).build();
		
		return ResponseEntity.ok().body(response);	
	}
	

	@Trace
	@GetMapping("{topicName}/viewTopic")
	public ResponseEntity<?> viewTopic(final @PathVariable String topicName, String servers){
		
		AdminClient client = factory.getAdminClients().get(servers);
		
		TopicViewDTO dto = topicService.view(client, topicName, servers);
		
		ResponseDTO<TopicViewDTO> response = ResponseDTO.<TopicViewDTO>builder().data(dto).build();
		
		return ResponseEntity.ok().body(response);
	}
	
	@Trace
	@DeleteMapping("{topicName}/deleteTopic")
	public ResponseEntity<?> deleteTopic(final @PathVariable String topicName, String servers){
		
		AdminClient client = factory.getAdminClients().get(servers);
		
		String deletedTopic = topicService.delete(client, topicName);
		
		ResponseDTO<String> response = ResponseDTO.<String>builder().data(deletedTopic).build();
		
		return ResponseEntity.ok().body(response);	
	}
	
	@Trace
	@PostMapping("addPartitionByTopic")
	public ResponseEntity<?> addPartitionByTopic(final @Valid @RequestBody TopicAddPartitionDTO dto){
		
		AdminClient client = factory.getAdminClients().get(dto.getServers());
		
		TopicAddPartitionDTO reponseDTO = topicService.addPartition(dto, client);
		
		ResponseDTO<TopicAddPartitionDTO> response = ResponseDTO.<TopicAddPartitionDTO>builder().data(reponseDTO).build();
		
		return ResponseEntity.ok().body(response);	
	}
	
	
	@Trace
	@PostMapping("modifyConfigs")
	public ResponseEntity<?> modifyReplication(final @Valid @RequestBody TopicModifyConfigsDTO dto){
		
		AdminClient client = factory.getAdminClients().get(dto.getServers());
		
		TopicModifyConfigsDTO reponseDTO = topicService.modifyConfigs(dto, client);
		
		ResponseDTO<TopicModifyConfigsDTO> response = ResponseDTO.<TopicModifyConfigsDTO>builder().data(reponseDTO).build();
		
		return ResponseEntity.ok().body(response);	
	}
	
	@Trace
	@PostMapping("modifyReplication")
	public ResponseEntity<?> modifyReplicationFactor(final @Valid @RequestBody TopicModifyReplicasDTO dto){
		
		String servers = dto.getServers();
		
		AdminClient client = factory.getAdminClients().get(servers);
		
		TopicModifyReplicasDTO reponseDTO = topicService.modifyReplicationFactor(dto, client, servers);
		
		ResponseDTO<TopicModifyReplicasDTO> response = ResponseDTO.<TopicModifyReplicasDTO>builder().data(reponseDTO).build();
		
		return ResponseEntity.ok().body(response);	
	}
	
	@Trace
	@PostMapping("modifyLeader")
	public ResponseEntity<?> modifyLeader(final @Valid @RequestBody TopicModifyLeaderDTO dto) {

		String servers = dto.getServers();
		
		AdminClient client = factory.getAdminClients().get(servers);
		
		TopicModifyLeaderDTO reponseDTO = topicService.modifyLeader(dto, client, servers);
		
		ResponseDTO<TopicModifyLeaderDTO> response = ResponseDTO.<TopicModifyLeaderDTO>builder().data(reponseDTO).build();
		
		return ResponseEntity.ok().body(response);	
	}
}
