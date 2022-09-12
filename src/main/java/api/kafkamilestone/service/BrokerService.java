package api.kafkamilestone.service;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.common.Node;
import org.springframework.stereotype.Service;

import static api.kafkamilestone.exception.ErrorCode.CLUSTER_EXECUTION;
import static api.kafkamilestone.utils.ClusterIOValues.*;

import api.kafkamilestone.dto.broker.BrokersDTO;
import api.kafkamilestone.dto.response.ResponseDTO;
import api.kafkamilestone.exception.MilestoneException;
import api.kafkamilestone.utils.IsCluster;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BrokerService {
	
	private final ClusterService clientService;

	public ResponseDTO<?> retrieveAll(AdminClient client) {
		
		try {
			Collection<Node> brokers = client.describeCluster().nodes().get();
			
			List<Double> brokersIncomingValues = clientService.brokerMetricValues(client, INCOMING_BYTE_RATE, IsCluster.BROKER);
			List<Double> brokersOutGoingValues = clientService.brokerMetricValues(client, OUTGOING_BYTE_RATE, IsCluster.BROKER);
			
			List<BrokersDTO> dtos = brokers.stream().map(br -> BrokersDTO.builder().id(br.idString())
																				   .host(br.host())
																				   .port(br.port())
																				   .build())
							.collect(Collectors.toList());
			
			IntStream.range(0, dtos.size()).forEach(i -> dtos.get(i).setBytesIn(brokersIncomingValues.get(i)));
			IntStream.range(0, dtos.size()).forEach(i -> dtos.get(i).setBytesOut(brokersOutGoingValues.get(i)));
			
			ResponseDTO<BrokersDTO> response = ResponseDTO.<BrokersDTO>builder().datas(dtos).build();
			
			return response;
			
		} catch (InterruptedException | ExecutionException e) {
			throw new MilestoneException(CLUSTER_EXECUTION);
		} 
	}

}
