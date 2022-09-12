package api.kafkamilestone.utils;

import static api.kafkamilestone.exception.ErrorCode.*;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.utils.AppInfoParser;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import api.kafkamilestone.exception.InvalidUrlException;
import api.kafkamilestone.factory.KafkaFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring actuator를 디펜던시에 추가하면 HealIndicator를 구현할 수 있다. 
 * 액츄에이터는 애플리케이션의 상태 정보를 나타낼 때 쓰는 라이브러리
 * {@link KafkaFactory} 여기서 카프카 client 를 생성하는데 생성 당시에는 exception을 잡지 못함 
 * 즉, 생성 즉시 예외를 잡고 종료가 불가능. 이후 계속해서 metadate update fail 메시지가 AdminMetadataManager로부터 알림.
 * 이 문제가 클러스터 주소 유효성 검사의 핵심
 * 그래서 HealthCheck는 client 생성 이후 발생하는 Exception을 잡아서 @throw invalidUrlException 던지고 및 client 종료
 * 나아가 client의 상태값들을 지정할 수 있음 (이 부분은 현재 미진한 수준)
 */

@RequiredArgsConstructor
@Component
@Slf4j
public class HealthCheck implements HealthIndicator{
	
	private final KafkaFactory factory;
	
	@Override
	public Health health() {
		
	   List<String> keys = factory.getKafkaAdmins().keySet().stream().collect(Collectors.toList());
	   String servers = keys.get(keys.size()-1);
	   
	   AdminClient client = factory.getAdminClients().get(servers);
	   
	   try {
		
			DescribeClusterOptions options = new DescribeClusterOptions().timeoutMs(1000);
			DescribeClusterResult clusterDesc = client.describeCluster(options);
			
			Health.Builder builder = new Builder();
			
			boolean producerIsNotRunning = factory.getProducers().get(servers).metrics().isEmpty();
			boolean consumerIsNotRunning = factory.getConsumers().get(servers).metrics().isEmpty();
			
			log.info("producer is not running : {}", producerIsNotRunning);
			log.info("consumer is not running : {}", producerIsNotRunning);
			
			boolean b = clusterDesc.nodes().get().isEmpty();
			
			log.info("b : {}", AppInfoParser.getCommitId());
			
			AppInfoParser.getCommitId();
			
			/* broker와의 연결이 끊어졌으면 [ down | disconnected | throw invalidUrlException ] */
			if(clusterDesc.nodes().get().isEmpty()) {
				
				return builder.down(new InvalidUrlException(SERVERS_URL_INVALID, servers))
							  .withDetail("disconnected", clusterDesc.nodes())
							  .build();
			} else {
				
				String flag = "none";
																							  /*				  P    C    */
				flag = (!producerIsNotRunning && !consumerIsNotRunning) ? "both" : flag;      /* running의 관점에서  true true  */
				flag = (!producerIsNotRunning &&  consumerIsNotRunning) ? "producer" : flag ; /* running의 관점에서  true false */
				flag = ( producerIsNotRunning && !consumerIsNotRunning) ? "consumer" : flag ; /* running의 관점에서  false true */
				
				return builder.up()
							  .withDetail("clusters", clusterDesc.nodes().get().size())
							  .withDetail("version", AppInfoParser.getVersion())
							  .withDetail("connected", flag)
							  .build();
			}
		
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			log.info(e.getMessage());
			factory.getProducers().get(servers).close();
			factory.getConsumers().get(servers).close();
			client.close();
			log.info("ffkfkfkfkfkfk");
			factory.getAdminClients().remove(servers);
			factory.getKafkaAdmins().remove(servers);
			throw new InvalidUrlException(SERVERS_URL_INVALID, servers);
		}
	}

}
