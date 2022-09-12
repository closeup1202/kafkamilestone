package api.kafkamilestone.factory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.MicrometerConsumerListener;
import org.springframework.kafka.core.MicrometerProducerListener;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.stereotype.Component;

import static api.kafkamilestone.exception.ErrorCode.*;

import api.kafkamilestone.aop.Retry;
import api.kafkamilestone.exception.InvalidUrlException;
import api.kafkamilestone.repository.ClusterRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Getter
@RequiredArgsConstructor
@Slf4j
public class KafkaFactory {
	
	private final ClusterRepository repository;
	private final MeterRegistry meterRegistry;
	
	private Map<String, AdminClient> adminClients = new LinkedHashMap<>();
	private Map<String, KafkaAdmin> kafkaAdmins = new LinkedHashMap<>();
	
	private Map<String, KafkaProducer<String, Object>> producers = new LinkedHashMap<>();
	private Map<String, Consumer<String, String>> consumers = new LinkedHashMap<>();
	
	private Map<String, ProducerFactory<String, Object>> producerFactories = new LinkedHashMap<>();
	private Map<String, ConsumerFactory<String, String>> consumerFactories = new LinkedHashMap<>();
	
	
	@Retry(4)
	public void setAdminClient(final String servers) {
		
		Map<String, Object> props = new HashMap<String, Object>();
		
		props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
		props.put(AdminClientConfig.METADATA_MAX_AGE_CONFIG, 0);
		
		try {
			
			if(!adminClients.containsKey(servers)) {
				log.info("servers dsadsadsadsadsa::: {}", servers);
				AdminClient createdClient = AdminClient.create(props);
				adminClients.put(servers, createdClient);
				kafkaAdmins.put(servers, new KafkaAdmin(props));
			}
			
		} catch (TimeoutException e) {
			log.info(e.getMessage());
			adminClients.remove(servers);
			kafkaAdmins.remove(servers);
			throw new InvalidUrlException(SERVERS_URL_INVALID, servers);
		} 
	}
	
	
    /* Producer */
    public void setProducer(final String servers){
		Map<String, Object> props = new HashMap<String, Object>();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
		props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 2000); /* 유효하지 않은 브로커 서버로 인한 error시 빠른 종료를 위해  */
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
		
		KafkaProducer<String, Object> producer =  new KafkaProducer<>(props);
		
		DefaultKafkaProducerFactory<String, Object> producerFactory = new DefaultKafkaProducerFactory<>(props);
		producerFactory.addListener(new MicrometerProducerListener<>(meterRegistry));
		
		if(!producers.containsKey(servers)) {
    		producers.put(servers, producer);
    	} 
    	
    	if(!producerFactories.containsKey(servers)) {
    		producerFactories.put(servers, producerFactory);
    	}
    	
    }
    
	/* Consumer | ConsumerFactory 
	 * 프로듀서와 달리 컨슈머 팩토리를 따로 둔 이유는 개별 컨슈머 그룹의 LAG 정보를 확인하기 위해서 GroupId를 필요로 하는 ConsumerFactory를 둠 
	 * 만일 컨슈머 팩토리만 있으면 같은 그룹명이 계속해서 생성되어서 매트릭 정보를 뽑아낼 때 에러가 남. AlreadyInstanceExist 
	 * 매트릭 정보 = Consumer
	 * 특정 그룹의 LAG 정보 = ConsumerFactory
	 * */
    public void setConsumer(final String servers) {
    	Map<String, Object> props = new HashMap<String, Object>();
    	props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
    	props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    	props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

    	Consumer<String, String> consumer = new KafkaConsumer<>(props);
    	
    	DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(props);
    	consumerFactory.addListener(new MicrometerConsumerListener<>(meterRegistry));
    	
    	if(!consumers.containsKey(servers)) {
    		consumers.put(servers, consumer);
    	}
    	
    	if(!consumerFactories.containsKey(servers)) {
    		consumerFactories.put(servers, consumerFactory);
    	}
    	
        
    }
    
    public ConsumerFactory<String, String> setConsumerFactory(final String servers, final String groupId){
    	Map<String, Object> props = new HashMap<String, Object>();
    	props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
    	props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    	props.put(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, groupId);
    	props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    	props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    	
    	DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(props);
    	consumerFactory.addListener(new MicrometerConsumerListener<>(meterRegistry));
    	
    	return consumerFactory;
    }
	
}
