package api.kafkamilestone.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Entity
@Builder
@Table(name = "GHMS_BROKER")
public class Broker {
	@Id @GeneratedValue
	@Column(name = "broker_id")
	private Long id; 
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cluster_id")
	@Setter
	private Cluster cluster;
	
	private String server;
	
	public static Broker toBrokerServer(String server) {
		return Broker.builder()
				.server(server)
				.build();
	}
}
