package api.kafkamilestone.service;

import java.util.List;
import java.util.stream.Collectors;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import api.kafkamilestone.dto.cluster.ClusterDTO;
import api.kafkamilestone.dto.response.ResponseDTO;
import api.kafkamilestone.entity.Broker;
import api.kafkamilestone.entity.Cluster;
import static api.kafkamilestone.exception.ErrorCode.*;
import api.kafkamilestone.exception.MilestoneException;
import api.kafkamilestone.repository.ClusterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ClusterDBService {
	
	private final ClusterRepository repository;
	
	@Transactional
	public ResponseDTO<?> create(final Cluster cluster) {
		
		repository.save(cluster);
		
		ClusterDTO dto = new ClusterDTO(cluster);
		
		return ResponseDTO.<ClusterDTO>builder().data(dto).build();
	}
	
	public ResponseDTO<?> retrieveAll(){
		
		List<Cluster> clusters = repository.findAll();
		
		List<ClusterDTO> dtos = clusters.stream().map(ClusterDTO::new).collect(Collectors.toList());
		
		return ResponseDTO.<ClusterDTO>builder().datas(dtos).build();
	}
	
	/**
	 * @throws MilestoneException : DB에 등록된 ID가 없을 때 예외 발생
	 * @see validation 역할도 겸함
	 */
	public <Optinal>Cluster retrieveById(final Long id){
		return repository.findById(id).orElseThrow(() -> new MilestoneException(CLUSTER_NOT_FOUND));
	}
	
	@Transactional
	public ResponseDTO<?> modifyServersById(final Cluster cluster, String dtoServers) {
		
		Cluster findCluster = retrieveById(cluster.getId());
		
		List<Broker> servers = ClusterDTO.toBrokerList(dtoServers);
		
		findCluster.getServers().clear();
		
		servers.stream().forEach(findCluster::addServers);
		
		ClusterDTO dto = new ClusterDTO(findCluster);
		
		return ResponseDTO.<ClusterDTO>builder().data(dto).build();
	}

	@Transactional
	public ResponseDTO<?> modifyClusterById(final Cluster cluster, String dtoServers) {
		
		Cluster findCluster = retrieveById(cluster.getId());
		
		List<Broker> servers = ClusterDTO.toBrokerList(dtoServers);
		
		findCluster.getServers().clear();
		
		servers.stream().forEach(findCluster::addServers);
		
		findCluster.clusterModify()
						.clusterName(cluster.getClusterName())
						.version(cluster.getVersion())
						.active(cluster.getActive())
						.build();
		
		log.info("fc = {}", findCluster);
		
		ClusterDTO dto = new ClusterDTO(findCluster);
		
		return ResponseDTO.<ClusterDTO>builder().data(dto).build();
	}

	@Transactional
	public ResponseDTO<?> deleteClusterById(final Long id) {
		
		Cluster findCluster = retrieveById(id);
		
		try {
			
			repository.deleteById(findCluster.getId());
			
			ClusterDTO dto = new ClusterDTO(findCluster);
			
			return ResponseDTO.<ClusterDTO>builder().data(dto).build();
		
		} catch(RuntimeException e) {
			throw new MilestoneException(UNEXPECTED_SERVER_ERROR);
		}
	}
}
