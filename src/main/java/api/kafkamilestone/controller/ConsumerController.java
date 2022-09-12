package api.kafkamilestone.controller;

import java.util.List;

import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import api.kafkamilestone.aop.Trace;
import api.kafkamilestone.dto.consumer.ConsumerRetrieveDTO;
import api.kafkamilestone.dto.consumer.ConsumerViewDTO;
import api.kafkamilestone.dto.response.ResponseDTO;
import api.kafkamilestone.factory.KafkaFactory;
import api.kafkamilestone.service.ConsumerService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ConsumerController {
	
	private final ConsumerService consumerService;
	private final KafkaFactory factory;
	
	@Trace
	@GetMapping("consumers")
	public ResponseEntity<?> retrieveTopics(final String servers){
		
		AdminClient client = factory.getAdminClients().get(servers);
		
		List<ConsumerRetrieveDTO> dtos = consumerService.retrieveAll(client);

		ResponseDTO<ConsumerRetrieveDTO> response = ResponseDTO.<ConsumerRetrieveDTO>builder().datas(dtos).build();
		
		return ResponseEntity.ok().body(response);
	}
	
	@Trace
	@GetMapping("{groupName}/viewConsumer")
	public ResponseEntity<?> viewConsumer(final @PathVariable String groupName, String servers){

		AdminClient client = factory.getAdminClients().get(servers);
		
		List<ConsumerViewDTO> dtos = consumerService.view(client, groupName, servers);
		
		ResponseDTO<ConsumerViewDTO> response = ResponseDTO.<ConsumerViewDTO>builder().datas(dtos).build();
		
		return ResponseEntity.ok().body(response);
	}

}
