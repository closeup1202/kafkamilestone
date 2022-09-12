package api.kafkamilestone.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartitionInfo;
import org.springframework.boot.actuate.health.Status;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.ProducerFactory.Listener;
import org.springframework.stereotype.Service;

import static api.kafkamilestone.exception.ErrorCode.*;
import static api.kafkamilestone.utils.ClusterIOValues.*;
import static api.kafkamilestone.utils.IsCluster.*;
import static api.kafkamilestone.utils.SingletonListIndex.*;
import static org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG;

import api.kafkamilestone.dto.cluster.ClusterOverviewDTO;
import api.kafkamilestone.exception.MilestoneException;
import api.kafkamilestone.factory.KafkaFactory;
import api.kafkamilestone.utils.HealthCheck;
import api.kafkamilestone.utils.IsCluster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClusterService {
	
	private final HealthCheck health;
	
	private final KafkaFactory factory;
	
	/**
	 * @see 클러스터 전체 상태를 보여주는 화면 단을 클릭했을 때 client 서버가 재생성되어 metric정보가 초기화되는 것을 방지하기 위함
	 * @param servers
	 * @return AdminClient
	 */
	public AdminClient clientInit(String servers) {
		
		Map<String, AdminClient> admins = factory.getAdminClients();
		
		log.info("ds :{}", admins.isEmpty());
		
		if(!admins.isEmpty()) {
			
			if(admins.containsKey(servers)) {
				
				return admins.get(servers);
				
			} else {
     			factory.setAdminClient(servers);
     			return admins.get(servers);
			}
			
		} else {
			factory.setAdminClient(servers);
			return admins.get(servers);
		}
	}

	/**
	 * @see 매트릭 정보를 위한 producer 및 consumer 서버 생성 및 리스너 등록
	 * 
	 * @param servers
	 * 
	 * @return 
	 * 컨트롤 단에서 현재 메서드에 이어 수행될 로직은 {@link #overview(AdminClient, ClusterOverviewDTO, KafkaProducer, String)}
	 * 클러스터 전체 상태를 나타내는 데 있어 Producer객체가 필요하기 떄문에 KafkaProducer를 반환한다.
	 * 
	 * @throws MilestoneException 프로듀서, 컨슈머, 프로듀서리스너, 컨슈머리스너 중 하나라도 구성에 실패했을 때 예외 발생
	 */
	public KafkaProducer<String, Object> ProducerAndConsumerForMetrics(String servers) {
		
		/* producer | producerListener */
		KafkaProducer<String, Object> producer;
		
		if(factory.getProducers().containsKey(servers)) {
			
			producer = factory.getProducers().get(servers);
			
		} else {
			log.info("dsdjiasdsai2222");
			factory.setProducer(servers);
			producer = factory.getProducers().get(servers);
		}
		
		
		log.info("producer : {}", producer);
		
		
		List<Listener<String, Object>> producerListener = factory.getProducerFactories().get(servers).getListeners();
		producerListener.get(VALUE_LIST).producerAdded("producerMetrics", producer);

		
		/* consumer | consumerListener */
		Consumer<String, String> consumer;
		
		if(factory.getConsumers().containsKey(servers)) {
			
			consumer = factory.getConsumers().get(servers);
			
		} else {
			log.info("dsdjiasdsai4441321321");
			factory.setConsumer(servers);
			consumer = factory.getConsumers().get(servers);
		}
		
		log.info("consumer : {}", consumer);
		
		List<org.springframework.kafka.core.ConsumerFactory.Listener<String, String>> consumerListener = factory.getConsumerFactories().get(servers).getListeners();
		consumerListener.get(VALUE_LIST).consumerAdded("consumerMetrics", consumer);
		
		if(!producer.metrics().isEmpty() && !consumer.metrics().isEmpty() && !producerListener.isEmpty() && !consumerListener.isEmpty()) {
			return producer;
			
		} else {
			throw new MilestoneException(CLUSTER_EXECUTION);		
		}
	}
	
	/**
	 * @return 클러스터 전체 정보를 반환. 
	 * 이에 관한 정보는 {@link #ClusterOverviewDTO} 의 필드값 참고
	 * 
	 * @see 아래 메서드들에서 정보 가져옴
	 * {@link #brokerMetricsValues(AdminClient, String)} 
	 * {@link #totalPartitions(AdminClient)} {@link #outOfSyncReplicasByCluster(AdminClient)}
	 */
	
	public ClusterOverviewDTO overview(AdminClient client, ClusterOverviewDTO dto, KafkaProducer<String, Object> producer, String servers) {
		
		try {
			
			Collection<Node> brokers = client.describeCluster().nodes().get();

			Collection<String> topics = client.listTopics().names().get();
			
			dto.setBrokers(brokers.size());
			dto.setInComingByte(brokerMetricValues(client, INCOMING_BYTE_RATE, CLUSTER).get(VALUE_LIST));
			dto.setOutGoingByte(brokerMetricValues(client, OUTGOING_BYTE_RATE, CLUSTER).get(VALUE_LIST));
			dto.setTopics(topics.size());
			dto.setPartitions(totalPartitions(client));
			dto.setOutOfSyncReplicas(outOfSyncReplicasByCluster(client, producer));
			dto.setHealthState(health.health().getStatus().toString());
			dto.setAvailableMetrics(health.health().getDetails().get("connected").toString());
			
			return dto;
			
		} catch (InterruptedException | ExecutionException e) {
			throw new MilestoneException(CLUSTER_EXECUTION);
		} 
	}
	
	/**
	 * @param client
	 * @param clusterMetricsNameIO (I-O 구분하기 위함)
	 * @param flag 개별 브로커의 값을 구하기 위해서 cluster 와 broker를 구분하는 flag {@link IsCluster} {@link BrokerService #retrieveAll(AdminClient)}
	 *
	 * *** 소수점 둘째 자리까지만 표시 
	 * @return 클러스터의 경우, 매트릭 정보 중에서 incoming-byte-rate | outgoing-byte-rate 를 각 브로커 마다 가져와 합산 후 평균을 담은 리스트 반환
	 * @return 각 브로커의 경우, 소수점 둘째 자리만 나타나도록 변환 후 리스트를 반환 
	 */
	public List<Double> brokerMetricValues(AdminClient client, String clusterMetricsNameIO, IsCluster flag) {
		
		try {
			
			DescribeClusterResult brokerInfo = client.describeCluster();
	
			Collection<Node> brokers = brokerInfo.nodes().get();
			
			List<Double> clusterIOvaluesList = new ArrayList<Double>();
			
			for(Map.Entry<MetricName, ? extends Metric> entry : client.metrics().entrySet()) {
				
				if(clusterMetricsNameIO.equals(entry.getKey().name())) {
					double metricValue = Double.parseDouble(entry.getValue().metricValue().toString());
					clusterIOvaluesList.add(metricValue);
				}
			}
		
			if(flag == CLUSTER) {
				
				double ioByteRateAverage = clusterIOvaluesList.stream().mapToDouble(i -> i).sum() / brokers.size();
		
				List<Double> clusterIOValue = new ArrayList<Double>();
				clusterIOValue.add(Double.parseDouble(String.format("%.2f", ioByteRateAverage)));
				
				return clusterIOValue;
			
			} else {
				
				List<Double> brokerIOValues = clusterIOvaluesList.stream().map(value -> Double.parseDouble(String.format("%.2f", value)))
																		  .collect(Collectors.toList());
				
				return brokerIOValues;
			}
		
		} catch (InterruptedException | ExecutionException e) {
			throw new MilestoneException(CLUSTER_EXECUTION);
		} 
	}

	
	/**
	 * @param client
	 * @return 총 파티션 개수
	 */
	public Long totalPartitions(AdminClient client) {
		
		try {
			
			Collection<String> topics = client.listTopics().names().get();
			
			Map<String, TopicDescription> topicInfo = client.describeTopics(topics).all().get();
			
			List<TopicDescription> topicValues = topicInfo.values().stream().collect(Collectors.toList());
			List<Long> partitions = topicValues.stream().map(topicVal -> topicVal.partitions().stream().map(TopicPartitionInfo::partition).count()).collect(Collectors.toList());

			return partitions.stream().mapToLong(partition -> partition).sum();
			
		} catch (InterruptedException | ExecutionException e) {
			throw new MilestoneException(CLUSTER_EXECUTION);
		} 
	}

	/**
	 * @param client
	 * @return 총 replicas에서 isr을 뺀 값 (out of sync replicas)
	 * 컬렉션을 소거하는 방식에서는 순서가 중요하지 않으므로, 성능 상의 이점을 얻기 위해 list 대신 Set을 사용
	 */
	
	public int outOfSyncReplicasByCluster(AdminClient client, KafkaProducer<String, Object> producer) {
		
		try {
			
			Collection<String> topics = client.listTopics().names().get();
			
			List<List<PartitionInfo>> partitionInfoList = topics.stream().map(producer::partitionsFor).collect(Collectors.toList());
			
			Set<String> replicas = new HashSet<String>();
			Set<String> inSyncReplicas = new HashSet<String>();
			
			for(List<PartitionInfo> partitionInfos : partitionInfoList) {
					
				partitionInfos.forEach(partitionInfo -> replicas.addAll(Arrays.stream(partitionInfo.replicas()).map(Node::idString)
																					   			               .collect(Collectors.toSet())));
				
				partitionInfos.forEach(partitionInfo -> inSyncReplicas.addAll(Arrays.stream(partitionInfo.inSyncReplicas()).map(Node::idString)
																											   .collect(Collectors.toSet())));
				
			}
		
			Set<String> outOfSyncReplicas = new HashSet<>(replicas);
			outOfSyncReplicas.removeAll(inSyncReplicas);
			
			return outOfSyncReplicas.size();
		
		} catch (InterruptedException | ExecutionException e) {
			throw new MilestoneException(CLUSTER_EXECUTION);
		} 
	}
	
}
