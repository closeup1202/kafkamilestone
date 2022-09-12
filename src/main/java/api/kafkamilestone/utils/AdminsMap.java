package api.kafkamilestone.utils;

import java.util.Map;

import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.stereotype.Component;

@Component
public class AdminsMap {

	private Map<String, AdminClient> admins;
	
	public AdminsMap(Map<String, AdminClient> admins) {
		this.admins = admins;
	}
	
}
