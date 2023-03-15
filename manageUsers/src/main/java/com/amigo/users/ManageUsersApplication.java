package com.amigo.users;

import java.time.Duration;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import com.amigo.users.dto.CustomerDTO;
import com.amigo.users.entity.Customer;
import com.amigo.users.repository.ManageUsersRepository;

@SpringBootApplication
public class ManageUsersApplication implements CommandLineRunner {
	private String password = "12345";
	private String adminRole = "admin";
	private String merchantRole = "merchant";
	@Autowired
	ManageUsersRepository manageUsersRepository;
	public static void main(String[] args) {
		SpringApplication.run(ManageUsersApplication.class, args);
	}
	@Override
	public void run(String... args) throws Exception {
		if (manageUsersRepository.findAll().isEmpty()) {
			addCustomer();
		}
	}
	
	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder.setConnectTimeout(Duration.ofMillis(60000)).setReadTimeout(Duration.ofMillis(60000)).build();
	}

	@Bean
	public ModelMapper modelMapper() {
		return new ModelMapper();
	}

	private void addCustomer() {
		// add admins if not exist
		CustomerDTO customer1 = new CustomerDTO("Ankush", "abc@abc.com", "9876543210", password, adminRole);
		CustomerDTO customer2 = new CustomerDTO("Messi", "def@abc.com", "9876543211", password, adminRole);
		CustomerDTO customer3 = new CustomerDTO("Chauhan", "ghi@abc.com", "9876543212", password, adminRole);

		// add merchant if not exist
		CustomerDTO customer4 = new CustomerDTO("JIO", "jio@abc.com", "9876543210", password, merchantRole);
		CustomerDTO customer5 = new CustomerDTO("TATA SKY", "tata@abc.com", "9876543211", password, merchantRole);
		CustomerDTO customer6 = new CustomerDTO("Public Health Engineering", "gov@raj.com", "9876543212", password,merchantRole);
		manageUsersRepository.save(modelMapper().map(customer1, Customer.class));
		manageUsersRepository.save(modelMapper().map(customer2, Customer.class));
		manageUsersRepository.save(modelMapper().map(customer3, Customer.class));
		manageUsersRepository.save(modelMapper().map(customer4, Customer.class));
		manageUsersRepository.save(modelMapper().map(customer5, Customer.class));
		manageUsersRepository.save(modelMapper().map(customer6, Customer.class));
	}

}
