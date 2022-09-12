package api.kafkamilestone.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import api.kafkamilestone.entity.Broker;

public interface BrokerRepository extends JpaRepository<Broker, Long> {
	List<Broker> findAllByClusterId(Long id);
}
