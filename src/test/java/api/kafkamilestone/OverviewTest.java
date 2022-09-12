package api.kafkamilestone;

import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.internals.KafkaConsumerMetrics;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.Metrics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;

import api.kafkamilestone.factory.KafkaFactory;
import api.kafkamilestone.service.ClusterDBService;
import api.kafkamilestone.service.ClusterService;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
public class OverviewTest {
	
	@Autowired
	ClusterService service;

	
	@Test
	public void overview() {
		//given
		String servers = "10.47.39.124:9092,10.47.39.125:9092,10.47.39.67:9092";
		
		//when
	}
}
