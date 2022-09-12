package api.kafkamilestone.utils;

/**
 * 매트릭 정보를 가져올 떄 전체를 계산해서 클러스터에 적용할지 개별적으로 브로커에 적용할지에 따른 구분
 */
public enum IsCluster {
	CLUSTER,
	BROKER,
}
