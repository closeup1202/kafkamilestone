package api.kafkamilestone.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import api.kafkamilestone.dto.response.ResponseDTO;
import api.kafkamilestone.factory.KafkaFactory;
import api.kafkamilestone.service.BrokerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BrokerController {
	
	private final KafkaFactory factory;
	private final BrokerService brokerService;

	@GetMapping("brokers")
	public ResponseEntity<?> retrieveBrokers(final String servers){
		
		ResponseDTO<?> response = brokerService.retrieveAll(factory.getAdminClients().get(servers));
		
		return ResponseEntity.ok().body(response);
	}
}
