package api.kafkamilestone.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import api.kafkamilestone.entity.Cluster;

public interface ClusterRepository extends JpaRepository<Cluster, Long>{

}
