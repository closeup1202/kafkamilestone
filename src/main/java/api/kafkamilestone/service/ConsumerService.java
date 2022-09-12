package api.kafkamilestone.service;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.DescribeConsumerGroupsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.springframework.stereotype.Service;

import static api.kafkamilestone.exception.ErrorCode.*;
import static org.apache.kafka.clients.admin.AdminClientConfig.*;

import api.kafkamilestone.aop.Trace;
import api.kafkamilestone.dto.consumer.ConsumerRetrieveDTO;
import api.kafkamilestone.dto.consumer.ConsumerViewDTO;
import api.kafkamilestone.exception.MilestoneException;
import api.kafkamilestone.factory.KafkaFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsumerService {
	
	private final KafkaFactory factory;
	
	/**
	 * @param client
	 *  
	 *  컨슈머 그룹의 정보는 [ group | type : isSimple | state ] 
	 * 
	 *** Simple 컨슈머 그룹이란, consumer group이 제공하는 것보다 파티션의 소비를 더욱 높은 성능으로 처리
	 *  {@link https://cwiki.apache.org/confluence/display/KAFKA/0.8.0+SimpleConsumer+Example} 참고
	 *  
	 *   (장점)
	 *   메시지를 여러 번 읽기
	 *	  프로세스의 주제에 있는 파티션의 하위 집합만 사용
	 *	  메시지가 한 번만 처리되도록 트랜잭션을 관리
	 *	
	 *	 (단점)
	 *	 SimpleConsumer는 소비자 그룹에 필요하지 않은 상당한 양의 작업이 필요
	 *   consume을 중단한 부분을 알기 위해 애플리케이션의 오프셋을 추적해야 함
	 *	  어떤 브로커가 주제 및 파티션에 대한 리드 브로커인지 파악해야 함
	 *	  브로커 리더 변경 사항을 처리해야 함
	 * 
	 *** State는 6가지 enum으로 [ UNKNOWN | PREPARING_REBALANCE | COMPLETING_REBALANCE | STABLE | DEAD | EMPTY ] 
	 *  {@link #ConsumerGroupState} 
	 *  {@link https://intl.cloud.tencent.com/document/product/597/31590} 참고
	 * 
	 */
	public List<ConsumerRetrieveDTO> retrieveAll(AdminClient client) {
		
		try {
			Collection<ConsumerGroupListing> consumerGroupList = client.listConsumerGroups().all().get();
			
			List<ConsumerRetrieveDTO> dtos = consumerGroupList.stream().map(item -> ConsumerRetrieveDTO.builder()
													                                                   .group(item.groupId())
													                                                   .type(item.isSimpleConsumerGroup())
													                                                   .state(item.state())
													                                                   .build())
								      							       .collect(Collectors.toList());
			
			return dtos;
			
		} catch (InterruptedException | ExecutionException e) {
			log.info(e.getMessage());
			throw new MilestoneException(CLUSTER_EXECUTION);
		}
		
	}
	
	/**
	 * @param client
	 * @param groupName
	 * coordinator와 파티션, 파티션별  offset | lag 가 담긴 리스트를 dto list에 담아서 반환 
	 * 하나의 컨슈머 그룹이 가지는 (토픽)파티션은 -0 -1 -2 이런 식으로 여러 개가 될 수 있기 떄문 
	 * 
	 * @throws MilestoneException 그룹 이름이 존재하지 않을 때 예외 발생
	 */
	@Trace
	public List<ConsumerViewDTO> view(AdminClient client, String groupName, String servers) {
		
		try {
			
			boolean groupIsNotExist = client.describeConsumerGroups(Collections.singleton(groupName)).all().get().isEmpty();
			
			if(!groupIsNotExist) {
		
				Node coordinator = getInstanceOwnerByGroupId(client, groupName);
				List<Long> offsetList = getCurrentOffsetsByGroupId(client, groupName);
				List<OptionalLong> lagList = getLagsByGroupId(client, groupName, servers);
				List<TopicPartition> partitions = getPartition(client, groupName);
				
				List<ConsumerViewDTO> dtos = new ArrayList<ConsumerViewDTO>();
				
				IntStream.range(0, offsetList.size()).forEach(i -> dtos.add(ConsumerViewDTO.builder()
																						   .consumerOffset(offsetList.get(i))
																						   .partition(partitions.get(i).toString())
																						   .lag(lagList.get(i))
																						   .consumerInstanceOwner(coordinator.toString())
																						   .build()));
				return dtos;
			
			} else {
				throw new MilestoneException(NOT_EMPTY_ARGS);
			}
		
		} catch (InterruptedException | ExecutionException e) {
			log.info(e.getMessage());
			throw new MilestoneException(CLUSTER_EXECUTION);
		}
	}
	
	/**
	 * @param client
	 * @param groupName
	 * @return 해당 컨슈머 그룹을 담당하는 주키퍼 코디네이터
	 */
	public Node getInstanceOwnerByGroupId(AdminClient client, String groupName) {
		
		try {
		
			DescribeConsumerGroupsResult consumerGroupsResult = client.describeConsumerGroups(Collections.singletonList(groupName));
			
			Node instanceOwner = consumerGroupsResult.all().get().get(groupName).coordinator();
			
			log.info("io : {}", instanceOwner);
			
			return instanceOwner;
		
		} catch (InterruptedException | ExecutionException e) {
			log.info(e.getMessage());
			throw new MilestoneException(CLUSTER_EXECUTION);
		}
	}
	
	/**
	 * @param client
	 * @param groupName
	 * @return 해당 컨슈머 그룹의 파티션 (순서를 일치시키기 위해 keySet을 List로)
	 */
	public List<TopicPartition> getPartition(AdminClient client, String groupName) {
		
		try {
		
			ListConsumerGroupOffsetsResult groupsResult = client.listConsumerGroupOffsets(groupName);
		
			List<TopicPartition> partitionsByGroup = groupsResult.partitionsToOffsetAndMetadata().get().keySet().stream().collect(Collectors.toList());
	
			return partitionsByGroup;
		
		} catch (InterruptedException | ExecutionException e) {
			log.info(e.getMessage());
			throw new MilestoneException(CLUSTER_EXECUTION);
		}
		
	}
	
	/**
	 * @param client
	 * @param groupName
	 * @return 해당 컨슈머 그룹이 가진 파티션별 OFFSET
	 */
	public List<Long> getCurrentOffsetsByGroupId(AdminClient client, String groupName) {
		
		try {
			
			Map<TopicPartition, OffsetAndMetadata> future = client.listConsumerGroupOffsets(groupName)
															      .partitionsToOffsetAndMetadata()
															      .get();
			
			log.info("furuturne : {}", future);
			
			List<Long> offset = future.values().stream().map(a -> a.offset()).collect(Collectors.toList());
			
			return offset;
		
		} catch (InterruptedException | ExecutionException e) {
			log.info(e.getMessage());
			throw new MilestoneException(CLUSTER_EXECUTION);
		}
	}
	
	/**
	 * @param client
	 * @param groupName
	 * @return 해당 컨슈머 그룹이 가진 파티션별 LAG
	 */
	public List<OptionalLong> getLagsByGroupId(AdminClient client, String groupName, String servers) {
		
		try {
			
			Consumer<String, String> consumer = factory.setConsumerFactory(servers, groupName).createConsumer();
			
			ListConsumerGroupOffsetsResult groupsResult = client.listConsumerGroupOffsets(groupName);
			
			List<TopicPartition> partitionsByGroup = groupsResult.partitionsToOffsetAndMetadata().get().keySet().stream().collect(Collectors.toList());
	        
			List<String> partitionsByGroupConvertTopics = partitionsByGroup.stream().map(a -> a.toString().substring(0, a.toString().length()-2)).collect(Collectors.toList());
			
			List<OptionalLong> lagList = new ArrayList<>();
			
			for(String topic : partitionsByGroupConvertTopics) {
				
				List<PartitionInfo> partitionInfoList = consumer.partitionsFor(topic);
				
				List<TopicPartition> topicPartitions = partitionInfoList.stream().map(p -> new TopicPartition(topic, p.partition())).collect(Collectors.toList());
				
				consumer.assign(topicPartitions);
				
				topicPartitions.forEach(tp -> lagList.add(consumer.currentLag(tp)));
			}
			
			return lagList;
		
		} catch (InterruptedException | ExecutionException e) {
			log.info(e.getMessage());
			throw new MilestoneException(CLUSTER_EXECUTION);
		}
	}
	
}
