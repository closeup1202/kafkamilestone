package api.kafkamilestone.controller;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import api.kafkamilestone.aop.Trace;
import api.kafkamilestone.dto.cluster.ClusterDTO;
import api.kafkamilestone.dto.cluster.ClusterOverviewDTO;
import api.kafkamilestone.dto.cluster.ClusterUpdateDTO;
import api.kafkamilestone.dto.response.ErrorResponseDTO;
import api.kafkamilestone.dto.response.ResponseDTO;
import api.kafkamilestone.entity.Broker;
import api.kafkamilestone.entity.Cluster;
import api.kafkamilestone.exception.ErrorCode;
import api.kafkamilestone.exception.MilestoneException;
import api.kafkamilestone.service.ClusterDBService;
import api.kafkamilestone.utils.HealthCheck;
import api.kafkamilestone.service.ClusterService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static org.springframework.util.StringUtils.*;
import static api.kafkamilestone.exception.ErrorCode.*;

/**
 * @static_Import
 * 직접적으로 객체가 변경될 일 없는 static import로 {@link ErrorCode} ErroCode, StringUtils, kafka Configs, Enum, Utils class {@link ClusterIOValues} 등이 있음
 * 컨트롤단 뿐만 아니라 해당 객체들이 쓰이는 곳마다 적용
 * 
 * @DI_class clusterDBService는 카프카의 실행과 무관하게 DB와 연동되어 있는 데이터를 추가 및 추출하기 위함
 * @DI_class clusterService는 실제 카프카 클러스터를 구성 및 설정, 카프카 자체 데이터를 추출하기 위함
 */

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ClusterController {
	
	private final ClusterDBService clusterDBService;
	private final ClusterService clusterService;
	private final HealthCheck health;
	
	private static final String kafkaVersion = "version";
	
	@Trace
	@PostMapping("cluster")
	public ResponseEntity<?> createCluster(final @Valid @RequestBody ClusterDTO dto){
		
		try {
		
			Cluster cluster = ClusterDTO.toCluster(dto);
			
			List<Broker> servers = ClusterDTO.toBrokerList(dto.getServers());
			
			servers.stream().forEach(cluster::addServers);
			
			ResponseDTO<?> response = clusterDBService.create(cluster);
			
			return ResponseEntity.ok().body(response);		
			
		} catch (Exception e){
		
			ErrorResponseDTO response = ErrorResponseDTO.builder().error(e.getMessage()).build();
			
			return ResponseEntity.badRequest().body(response);
		
		} 
	}
	
	@GetMapping("cluster")
	public ResponseEntity<?> retrieveClusterAll(){
		
		ResponseDTO<?> response = clusterDBService.retrieveAll();
		
		return ResponseEntity.ok().body(response);
	}
	
	@Transactional
	@GetMapping("{id}/overview")
	public ResponseEntity<?> retrieveClusterById(final @PathVariable Long id) {

		/* 1. DB에서 추출할 데이터 */
		
		Cluster cluster = clusterDBService.retrieveById(id);
		
		String servers = ClusterDTO.toStringServers(cluster.getServers());
		
		/* 2. 카프카 내에서 추출할 데이터  */
		
		/* 2.1 클러스터 전체 상태를 보여주는 화면 단을 클릭했을 때 client 서버가 재생성되어 metric정보가 초기화되는 것을 방지하기 위함 */
		AdminClient client = clusterService.clientInit(servers);
		
		/* 2.2 매트릭 정보를 위한 producer 및 consumer 서버 생성 및 리스너 등록 */
		KafkaProducer<String, Object> producer = clusterService.ProducerAndConsumerForMetrics(servers);
		
		/** 2.3 생성된 client 서버의 버전과 사용자가 입력한 버전이 일치하지 않을 때 자동 변경  
		 *** 	
		 * 2.2에서 생성한 producer 와 consumer도 필요하기 때문에 (그렇지 않으면 NPE 발생) 2.3에 위치
		 * {@link #HealthCheck} line 51, 52
		 * */
		String clientVersion = health.health().getDetails().get(kafkaVersion).toString();
		
		if(!cluster.getVersion().equals(clientVersion)) {
			cluster.clusterModify()
			.clusterName(cluster.getClusterName())
			.version(clientVersion)
			.active(cluster.getActive())
			.build();
		}
			
		/* 2.4 cluster관한 정보를 가져옴 [ 브로커 수, 브로커의 트래픽 정보, 토픽 수, 파티션 수, Out Of Sync Replicas, 클러스터 상태, 매트릭 정보를 확인할 수 있는 카프카 객체 ]*/
		ClusterOverviewDTO viewDTO = new ClusterOverviewDTO();
		
		viewDTO = clusterService.overview(client, viewDTO, producer, servers);
		
		
		/* 3. 반환 데이터  */
		
		ClusterDTO dto = new ClusterDTO(cluster);
		
		List<Object> dtos = new ArrayList<Object>();
		dtos.add(dto);
		dtos.add(viewDTO);
		
		ResponseDTO<?> response = ResponseDTO.builder().datas(dtos).build();
	
		return ResponseEntity.ok().body(response);
	}
	
	@Trace
	@PutMapping("modifyInvalidUrl")
	public ResponseEntity<?> modifyClusterUrlById(final @Valid @RequestBody ClusterUpdateDTO dto){
		
		Cluster cluster = ClusterUpdateDTO.toCluster(dto);
		
		ResponseDTO<?> response = clusterDBService.modifyServersById(cluster, dto.getServers());
		
		return ResponseEntity.ok().body(response);
	}
	
	@Trace
	@PutMapping("modifyCluster")
	public ResponseEntity<?> modifyClusterById(final @Valid @RequestBody ClusterUpdateDTO dto){
		
		if(!hasText(dto.getClusterName()) || !hasText(dto.getServers())) {
			throw new MilestoneException(NOT_EMPTY_ARGS);
		}	
		
		Cluster cluster = ClusterUpdateDTO.toCluster(dto);
		
		ResponseDTO<?> response = clusterDBService.modifyClusterById(cluster, dto.getServers());
		
		return ResponseEntity.ok().body(response);
	}
	
	@Trace
	@DeleteMapping("{id}/deleteCluster")
	public ResponseEntity<?> deleteClusterById(final @PathVariable Long id){
		
		ResponseDTO<?> response = clusterDBService.deleteClusterById(id);
		
		return ResponseEntity.ok().body(response);
	}
}
