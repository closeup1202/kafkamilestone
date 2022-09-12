package api.kafkamilestone.service;

import static api.kafkamilestone.exception.ErrorCode.*;
import static org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.common.config.TopicConfig.*;
import static api.kafkamilestone.utils.SingletonListIndex.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.validation.Valid;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.CreatePartitionsResult;
import org.apache.kafka.clients.admin.CreateTopicsOptions;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.NewPartitionReassignment;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.admin.AlterConfigOp.OpType;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.stereotype.Service;

import api.kafkamilestone.dto.topic.TopicAddPartitionDTO;
import api.kafkamilestone.dto.topic.TopicCreateDTO;
import api.kafkamilestone.dto.topic.TopicModifyConfigsDTO;
import api.kafkamilestone.dto.topic.TopicModifyLeaderDTO;
import api.kafkamilestone.dto.topic.TopicModifyReplicasDTO;
import api.kafkamilestone.dto.topic.TopicRetrieveDTO;
import api.kafkamilestone.dto.topic.TopicViewDTO;
import api.kafkamilestone.exception.MilestoneException;
import api.kafkamilestone.factory.KafkaFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 카프카의 오픈 소스 메서드들은 쓰레드로 동작하기 때문에
 * Custome한 Map컬렉션은 대부분 (카프카 내장 메소드 제외)
 * 멀티쓰레드 환경에서 안전한 ConcurrentHashMap을 HashMap대신 사용
 * HashMap은 동기화되지 않아서 멀티스레드 환경에 적합하지 않으며, key value에 null을 허용한다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TopicService {
	
	private final KafkaFactory factory;
	
	/**
	 * @return TopicDTO = name | internal | partitions | cleanup policy (from {@link #topicConfigs(List, AdminClient)}) 
	 * 
	 * cleanup.policy = compact 혹은 delete 둘중 하나. {@link https://kafka.apache.org/documentation/#topicconfigs}
	 * 로그 세그먼트에 사용할 데이터 보존 정책을 지정 
	 * 이 설정은 로그의 retention policy를 어떻게 할지를 결정한다.
	 *   (1) compact : key별로 가장 최근의 value만 저장한다. (주로 KTable에서 사용)
	 *   (2) delete  : retention.ms를 지나거나, retention.bytes 사이즈 제한을 넘어선 경우, 오래된 세그먼트를 삭제한다.
	 * retention은 데이터의 유실과 지속을 결정하는 카프카의 중요한 개념 
	 * 카프카는 데이터를 영원히 보존하지 않으며, 메시지 삭제 전에 모든 컨슈머가 읽기를 기다리지도 않기 때문.
	 * 
	 * 따라서 토픽의 여러 컨피그 중에 cleanup.policy를 대표로 토픽 리스트에 나타내고자 했음
	 */
	public List<TopicRetrieveDTO> retrieveAll(AdminClient client){
		
		try {
			
			Collection<String> topics = client.listTopics().names().get();
			
			List<String> topicList = topics.stream().collect(Collectors.toList());
			
			Map<String, TopicDescription> topicInfo = client.describeTopics(topicList).all().get();
			
			/* 토픽 cleanup policy 정보 */
			List<Map<ConfigResource, Config>> configs = topicConfigsByTopicList(topicList, client);
			
			List<String> cleanupPolicyList = new ArrayList<String>();
			
			configs.forEach(config -> cleanupPolicyList.add(config.values()
																  .stream()
																  .map(entry -> entry.get(CLEANUP_POLICY_CONFIG).value())
																  .findAny().get()));
			
			/* [ name | cleanupPolicy | partitionCount | partition | replicas ] 포함하는 토픽 리스트를 TopicDTO 로 변환  */
			List<TopicDescription> topicValues = topicInfo.values().stream().collect(Collectors.toList());

			List<TopicRetrieveDTO> dtos = topicValues.stream().map(topic ->  TopicRetrieveDTO.builder()
																							 .name(topic.name())
																							 .internal(topic.isInternal())
																							 .partitionInfo(TopicRetrieveDTO.toConvertPartition(topic.partitions()))
																							 .partitionCount(topic.partitions().stream().map(TopicPartitionInfo::partition).count())
																							 .build())
													  		  .collect(Collectors.toList());
			
			IntStream.range(0, dtos.size())
					 .forEach(i -> dtos.get(i).setCleanupPolicy(cleanupPolicyList.get(i)));
			
			return dtos;
		
		} catch (InterruptedException | ExecutionException e) {
			log.info(e.getMessage());
			throw new MilestoneException(CLUSTER_EXECUTION);
		}
	}

	/**
	 * @param topicList 
	 * @param AdminClient
	 * @return topicList에 있는 topic들 각각의 topicConfigs를 반환 {@link TopicConfig}
	 */
	public List<Map<ConfigResource, Config>> topicConfigsByTopicList(List<String> topicList, AdminClient client){
		
		List<Map<ConfigResource, Config>> topicConfigs = new ArrayList<Map<ConfigResource, Config>>();
		
		topicList.forEach(topic -> {
			
			try {
			
				ConfigResource configResource = new ConfigResource(ConfigResource.Type.TOPIC, topic);
				DescribeConfigsResult describeConfigsResult = client.describeConfigs(Collections.singleton(configResource));
				
				Map<ConfigResource, Config> configs = describeConfigsResult.all().get();
				topicConfigs.add(configs);

			} catch (InterruptedException | ExecutionException e) {
				log.info(e.getMessage());
				throw new MilestoneException(CLUSTER_EXECUTION);
			}
			
		});
		
		return topicConfigs;
	}
	
	/**
	 * @param TopicCreateDTO
	 * @return topic을 만들 때 필요한 주요 config를 Map으로 반환 {@link create}
	 */
	
	public Map<String, String> createTopicConfigs(final TopicCreateDTO dto){
		
		Map<String, String> topicConfigs = new ConcurrentHashMap<String, String>();
		topicConfigs.put(CLEANUP_POLICY_CONFIG, dto.getCleanupPolicy());
		topicConfigs.put(DELETE_RETENTION_MS_CONFIG, dto.getDeleteRetention());
		topicConfigs.put(FILE_DELETE_DELAY_MS_CONFIG, dto.getFileDeleteDelay());
		topicConfigs.put(MAX_MESSAGE_BYTES_CONFIG, dto.getMaxMessage());
		topicConfigs.put(COMPRESSION_TYPE_CONFIG, dto.getCompressionType());
		topicConfigs.put(SEGMENT_BYTES_CONFIG, dto.getSegment());
		topicConfigs.put(MIN_IN_SYNC_REPLICAS_CONFIG, dto.getMinIsr());
		
		return topicConfigs;
	}
	
	/**
	 * @param dto
	 * @param client
	 * 토픽 이름이 중복 시 예외 발생 @throw MilestoneException
	 */
	public TopicCreateDTO create(final TopicCreateDTO dto, AdminClient client) {
		
		try {
			
			Collection<String> topics = client.listTopics().names().get();
			
			boolean DuplTopicName = topics.contains(dto.getTopicName());
		
			if(!DuplTopicName) {
				
				Collection<NewTopic> newTopics = new ArrayList<NewTopic>();
				newTopics.add(TopicBuilder.name(dto.getTopicName())
											.partitions(dto.getPartitions())
											.replicas(dto.getReplication())
											.configs(createTopicConfigs(dto))
											.build());
				
				CreateTopicsResult createResult = client.createTopics(newTopics, new CreateTopicsOptions());
				
				log.info("savedTopic : {}, partitions : {}, replication : {} ", dto.getTopicName(), 
																				createResult.numPartitions(dto.getTopicName()).get(),
																				createResult.replicationFactor(dto.getTopicName()).get());
				
				return dto;
	
			} else {
				throw new MilestoneException(TOPIC_NAME_DUPLICATE);
			}
		
		} catch (InterruptedException | ExecutionException e) {
			log.info(e.getMessage());
			throw new MilestoneException(CLUSTER_EXECUTION);
		}
	}
	
	/**
	 * 
	 * @param client
	 * @param topicName
	 * @return 한 개의 토픽에 관한 정보 반환 [토픽 이름, 총 파티션 수, 파티션들에 대한 정보(leader, replicas, isr), 총 오프셋 수, 토픽의 컨피그]
	 * 토픽 이름 없을 시 예외 발생 @throw MilestoneException
	 */
	public TopicViewDTO view(AdminClient client, String topicName, String servers) {
		
		try {
			
			Collection<String> topics = client.listTopics().names().get();
			
			Map<String, TopicDescription> topicInfo = client.describeTopics(topics).all().get();
			
			
			if(topicInfo.containsKey(topicName)) {
				
				ConfigResource configResource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);
				DescribeConfigsResult describeConfigsResult = client.describeConfigs(Collections.singleton(configResource));
				
				Collection<Config> configs = describeConfigsResult.all().get().values();
				List<Config> configsList = configs.stream().collect(Collectors.toList());
				
				Map<String, String> configMapByTopic = new ConcurrentHashMap<String, String>();
				configsList.get(VALUE_LIST).entries().forEach(action -> configMapByTopic.put(action.name(), action.value()));
				
				TopicViewDTO dto = TopicViewDTO.builder()
											   .topicName(topicInfo.get(topicName).name())
											   .partitionCount(topicInfo.get(topicName).partitions().stream().map(TopicPartitionInfo::partition).count())
											   .partitionInfo(TopicViewDTO.toConvertPartition(topicInfo.get(topicName).partitions()))
											   .configs(configMapByTopic)
											   .build();
				
				 Map<TopicPartition, Long> offsets = getEndOffsets(client, topicName, servers);
				 
				 Long sumofOffsets = offsets.values().stream().mapToLong(i -> i).sum();
				 
				 dto.setOffsetsSum(sumofOffsets);
				 
				 return dto;
				
			} else {
				throw new MilestoneException(TOPIC_NAME_NOT_FOUND);
			}
		
		} catch (InterruptedException | ExecutionException e) {
			log.info(e.getMessage());
			throw new MilestoneException(CLUSTER_EXECUTION);
		}
	}
	
	/**
	 * @param client
	 * @param topic
	 * 토픽에 있는 파티션을 가져오는 방법에는 consumer의 partitionsFor메서드의 사용이 있다.
	 * consumer의 endOffsets를 통해 구할 수 있음 토픽 내 각 파티션의 전체 offset 개수를 구할 수 있음
	 * consumer를 자동 커밋으로 설정해두었기 때문에 파티션 내 오프셋 수는 Consumer.endOffsets 수와 동일  
	 * 그룹아이디가 지정되지 않은 consumer 생성 후 
	 * @return topic에 저장된 partition들의 offset을 map에 담아서 반환
	 */
	public Map<TopicPartition, Long> getEndOffsets(AdminClient client, String topic, String servers) {
		
		Consumer<String, String> consumer = factory.getConsumers().get(servers);
		
		List<PartitionInfo> partitionInfoList = consumer.partitionsFor(topic);
		
		List<TopicPartition> topicPartitions = partitionInfoList.stream().map(p -> new TopicPartition(topic, p.partition())).collect(Collectors.toList());
		
		Map<TopicPartition, Long> endOffsets = consumer.endOffsets(topicPartitions);
		
		return endOffsets;
		
	}
	
	/**
	 * @param client
	 * @param topicName
	 * 중복되지 않는 토픽명을 id로 삼아 토픽 삭제 (중복 시 @throw MilestoneException)
	 * 토픽 이름 없을 시 예외 발생 @throw MilestoneException
	 */
	public String delete(AdminClient client, String topicName) {
		
		try {
			
			Collection<String> topics = client.listTopics().names().get();
			Collection<String> findTopics = new ArrayList<String>();
			
			String deleteTopicName = topics.stream().filter(topic -> topic.equals(topicName))
										    		.findAny()
										    		.orElseThrow(()-> new MilestoneException(TOPIC_NAME_NOT_FOUND));
			
			findTopics.add(deleteTopicName);
			
			client.deleteTopics(findTopics);
			
			findTopics.clear();

			log.info("deleteTopic : {}", deleteTopicName);
			
			return deleteTopicName;
		
		} catch (InterruptedException | ExecutionException e) {
			log.info(e.getMessage());
			throw new MilestoneException(CLUSTER_EXECUTION);
		}
	}

	/**
	 * @param dto (토픽명과 파티션 수가 담긴 DTO) {@link TopicAddPartitionDTO}
	 * @param client
	 * 토픽 이름 없을 시 예외 발생 @throw MilestoneException
	 */
	public TopicAddPartitionDTO addPartition(final TopicAddPartitionDTO dto, AdminClient client) {
		
		try {
			
			Collection<String> topics = client.listTopics().names().get();
		
			if(topics.contains(dto.getTopicName())) {
				
				Map<String, NewPartitions> addPartitionMap = new HashMap<String, NewPartitions>();
				addPartitionMap.put(dto.getTopicName(), NewPartitions.increaseTo(dto.getAddCount()));
				
				CreatePartitionsResult addPartitionResult = client.createPartitions(addPartitionMap);

				log.info("addPartitionResult : {}", addPartitionResult.values());
				
				return dto;
				
			} else {
				throw new MilestoneException(TOPIC_NAME_NOT_FOUND);
			}
			
		} catch (InterruptedException | ExecutionException e) {
			log.info(e.getMessage());
			throw new MilestoneException(CLUSTER_EXECUTION);
		}
	}
	
	/**
	 * @param dto
	 * @param client
	 * @see 토픽 config 변경
	 * 토픽 이름 없을 시 예외 발생 @throw MilestoneException
	 * 
	 * Collection<AlterConfigOp>에 들어갈 값인 new AlterConfigOp 객체에서
	 * OpType을 통해 Config의 전환 형태를 결정한다. OpType에는 [ SET | DELETE | APPEND | SUBSTRACT ] 로 4가지 enum이 있다. {@link AlterConfigOp}
	 * (1) SET은 Config를 기본적으로 구성함
	 * (2) DELETE는 기본값으로 되돌린다.(NULLABLE)
	 * (3) APPEND는 지정된 값을 추가함
	 * (4) SUBSTRACT는 지정된 값을 현재 값에서 제거. 모든 항목이 제거되더라도 기본값으로 되돌리지 않는다.
	 * 
	 * ** 
	 * {@link AdminClient} 의 메소드인 alterConfigs가 Config 변경을 수행했는데 Deprecated 되었음
	 * 이를 대체하는 것이 incrementalAlterConfigs
	 */
	public TopicModifyConfigsDTO modifyConfigs(final @Valid TopicModifyConfigsDTO dto, AdminClient client) {
		
		Map<String, String> newConfigs = new ConcurrentHashMap<String, String>();
		dto.getKeyValueList().forEach( config -> newConfigs.put(config.getKey(), config.getValue()));

		try {

			Collection<String> topics = client.listTopics().names().get();

			if(topics.contains(dto.getTopicName())) {
				
				ConfigResource configResource = new ConfigResource(ConfigResource.Type.TOPIC, dto.getTopicName());
				
				Collection<AlterConfigOp> alterConfigOps = new ArrayList<>();
				
				newConfigs.entrySet()
						  .forEach(entry -> alterConfigOps.add(new AlterConfigOp(new ConfigEntry(entry.getKey(), entry.getValue()), OpType.SET)));
				
				Map<ConfigResource, Collection<AlterConfigOp>> modifyTopicConfigs = new HashMap<ConfigResource, Collection<AlterConfigOp>>();
				modifyTopicConfigs.put(configResource, alterConfigOps);
				
				client.incrementalAlterConfigs(modifyTopicConfigs);
				
				return dto;
				
			} else {
				throw new MilestoneException(TOPIC_NAME_NOT_FOUND);
			}
			
		} catch (InterruptedException | ExecutionException e) {
			log.info(e.getMessage());
			throw new MilestoneException(CLUSTER_EXECUTION);
		}
	}
	
	/**
	 * @param dto
	 * @param client
	 * 
	 * 토픽의 리플리케이션 팩터 수 변경 
	 * partition-0의 리플리케이션 factor를 변경한다.
	 * 왜냐하면 토픽 생성 시 하나 이상의 파티션을 가져야 하기 때문에 replication factor는 첫 번째 파티션인 partition-0에 기준을 둔다.
	 * ex) 파티션이 2개일 때 partition-0의 replicas는 2개이고 partition-1의 replicas가 3개 더라도 replication factor는 2다.
	 *  
	 * 변경할 리플리케이션 노드 {@link #replicationFactorModifyNodeList(AdminClient, int)}
	 * 리플리케이션 변경 {@link #modifyReplication(AdminClient, String, List, int)}
	 * 토픽 이름 없을 시 예외 발생 @throw MilestoneException
	 */
	public TopicModifyReplicasDTO modifyReplicationFactor(final @Valid TopicModifyReplicasDTO dto, AdminClient client, String servers) {
		
		try {
			
			Collection<String> topics = client.listTopics().names().get();

			if(topics.contains(dto.getTopicName())) {

				String topicName = dto.getTopicName();
				
				/* 변경할 리플리케이션 노드 리스트 */
				List<Integer> newReplicasList = replicationFactorModifyNodeList(client, dto.getReplication());
				
				/* 파티션의 리플리케이션 변경 */
				modifyReplication(client, topicName, newReplicasList, VALUE_LIST, servers);
				
				return dto;
				
			} else {
				throw new MilestoneException(TOPIC_NAME_NOT_FOUND);
			}
			
		} catch (InterruptedException | ExecutionException e) {
			log.info(e.getMessage());
			throw new MilestoneException(CLUSTER_EXECUTION);
		}
	}
	
	/**
	 * @param client
	 * @param modifyReplicationCount
	 * @return 변경할 리플리케이션 개수에 관한 node 리스트
	 */
	public List<Integer> replicationFactorModifyNodeList(AdminClient client, int modifyReplicationCount){
		
		try {
			
			Collection<Node> nodes  = client.describeCluster().nodes().get();
			
			List<Node> node = nodes.stream().collect(Collectors.toList());
			
			int removeCount = node.size()-modifyReplicationCount; 
			
			if(removeCount != 0) {
				for(int i=0; i<removeCount; i++) {
					node.remove(node.size()-1);
				}
			}
			
			List<Integer> newReplicasList = new ArrayList<Integer>();
			
			for(int i=1; i<=node.size(); i++) {
				newReplicasList.add(i);
			}
			
			return newReplicasList;
		
		} catch (InterruptedException | ExecutionException e) {
			log.info(e.getMessage());
			throw new MilestoneException(CLUSTER_EXECUTION);
		}
	}

	/**
	 * @param client
	 * @param topicName (특정 토픽)
	 * @param newReplicasList
	 * @param partition (특정 파티션)
	 * 
	 * 파티션의 리플리케이션 변경 (리플리케이션 팩터 수 | 파티션의 리더를 변경할 때 쓰임)
	 */
	public void modifyReplication(AdminClient client, String topicName, List<Integer> newReplicasList, int partition, String servers) {
		
		Map<String, TopicDescription> topic = factory.getKafkaAdmins().get(servers).describeTopics(topicName);
		
		List<TopicPartitionInfo> currPartitionInfo = topic.get(topicName).partitions();
		
		Optional<NewPartitionReassignment> newPartitionReassignMent = Optional.of(new NewPartitionReassignment(newReplicasList));

		TopicPartition topicPartition = new TopicPartition(topicName, currPartitionInfo.get(partition).partition());
		
		Map<TopicPartition, Optional<NewPartitionReassignment>> alterPartitionsMap = new ConcurrentHashMap<TopicPartition, Optional<NewPartitionReassignment>>();
		alterPartitionsMap.put(topicPartition, newPartitionReassignMent);
		
		client.alterPartitionReassignments(alterPartitionsMap);
		
	}
	
	/**
	 * @param dto
	 * @param client
	 * 토픽 내 복수의 파티션 중 하나를 선정하여 리더 변경 
	 * 
	 * 1) replication의 순서를 변경, replication의 첫 번째가 리더인 것으로 가정
	 * 2) 가지고 있는 replication 수보다 높은 숫자로 정해진 리더 변경 시 replication이 증가되도록. 
	 *    이를테면, replication이 1이고 leader가 1이면 leader를 2나 3으로 변경할 수 없다. replication 수가 하나밖에 없으므로
	 *    그렇기 때문에 replication수가 1일떄 leader를 2나 3으로 변경하자고 한다면 replication수를 leader로 지정된 수만큼 증가시키고자 한다
	 *    
 	 * 3) 순서는 변경했지만 isr은 변경되지 않으므로 replication 수를 조정
 	 * 4) isr 변경 및 leader 변경
	 * 5) 다시 기존의 replication 수로 변경 
	 * 
	 * 이와 같은 부단한 과정을 설계한 이유는 특정 토픽 내 특정 파티션의 leader변경을 위한 적절한 메소드나 로직을 찾지 못했기 때문
	 * 
	 * 토픽 이름 없을 시 예외 발생
	 * @throws MilestoneException
	 * 
	 */
	public TopicModifyLeaderDTO modifyLeader(@Valid TopicModifyLeaderDTO dto, AdminClient client, String servers) {
		
		String topicName = dto.getTopicName();
		int partition = dto.getPartition();
		int newLeader = dto.getLeader();
		
		try {
			
			Collection<String> topics = client.listTopics().names().get();
	
			if(topics.contains(dto.getTopicName())) {
			
				Map<String, TopicDescription> topic = factory.getKafkaAdmins().get(servers).describeTopics(topicName);
				
				List<TopicPartitionInfo> currPartitionInfoList = topic.get(topicName).partitions();
		
				TopicPartitionInfo currPartitionInfo = currPartitionInfoList.get(partition);
				
				/* 지정된 newLeader가 기존 replication 수 보다 크면  newLeader만큼  replication 수 증가 */
				if(newLeader > currPartitionInfo.replicas().size()) {
					
					List<Integer> newReplicasCount = replicationFactorModifyNodeList(client, newLeader);
					
					modifyReplication(client, topicName, newReplicasCount, partition, servers);
				}
				
				/* newLeader만 존재하는 newReplicasList */
				List<Integer> newReplicasList  = new ArrayList<>();
				newReplicasList.add(newLeader);
				
				modifyReplication(client, topicName, newReplicasList, partition, servers);
				
				/* newLeader에 현재 replicas와 일치하지 않는 것들을 넣어서 변환 */
				currPartitionInfo.replicas().stream().filter(replicas -> replicas.id() != newLeader)
										    .findAny()
										    .map(replicas -> newReplicasList.add(replicas.id()));
				
				/* 카프카 서버가 leader를 변경하는 동안 다음 코드로 넘어가는 것을 방지하기 위함 */
				TimeUnit.MILLISECONDS.sleep(1000);
				
				modifyReplication(client, topicName, newReplicasList, partition, servers);
			
				return dto;
				
			} else {
				throw new MilestoneException(TOPIC_NAME_NOT_FOUND);
			}
		
		} catch (InterruptedException | ExecutionException e) {
			log.info(e.getMessage());
			throw new MilestoneException(CLUSTER_EXECUTION);
		}
	}
}
