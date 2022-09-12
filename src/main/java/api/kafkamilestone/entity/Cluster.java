package api.kafkamilestone.entity;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter@Builder(builderClassName = "cluster")
@Entity
@Table(name = "GHMS_CLUSTER")
public class Cluster {
	@Id @GeneratedValue
	@Column(name = "cluster_id")
	private Long id;
	
	private String clusterName;
	
	@Builder.Default
	@OneToMany(mappedBy = "cluster", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Broker> servers = new ArrayList<>();
	
	private String version;
	
	@Enumerated(EnumType.STRING)
	private Active active;
	
	public void addServers(Broker broker) {
		broker.setCluster(this);
		servers.add(broker);
	}
	
	public void removeServers(Broker broker) {
		broker.setCluster(this);
		servers.remove(broker);
	}
	
	@Builder(builderMethodName = "clusterModify")
	public void modifyInfoExceptServers(Active active, String clusterName, String version) {
		this.active = active;
		this.clusterName = clusterName;
		this.version = version;
	}
	
	@Builder(builderMethodName = "clusterModifyVersion")
	public void setVersion(String version) {
		this.version = version;
	}
}
