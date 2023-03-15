package com.amigo.bill;

import java.time.Duration;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import com.amigo.bill.dto.MerchantDetailsDTO;
import com.amigo.bill.entity.MerchantDetails;
import com.amigo.bill.exceptions.GenericException;
import com.amigo.bill.repository.MerchantRepository;

@SpringBootApplication
public class PayBillApplication implements CommandLineRunner {
	@Autowired
	MerchantRepository merchantRepository;

	public static void main(String[] args) {
		SpringApplication.run(PayBillApplication.class, args);
	}
	@Override
	public void run(String... args) throws Exception {
		if (merchantRepository.findAll().isEmpty()) {
			addMerchant();
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
	private void addMerchant() throws GenericException {
		MerchantDetailsDTO merchantDetailsDTO1 = new MerchantDetailsDTO("JIO","mobile bill payment","jio@abc.com");
		MerchantDetailsDTO merchantDetailsDTO2 = new MerchantDetailsDTO("TATA SKY","tv bill payment","tata@abc.com");
		MerchantDetailsDTO merchantDetailsDTO3 = new MerchantDetailsDTO("Public Health Engineering","water bill payment","gov@raj.com");
		try {
			merchantRepository.save(modelMapper().map(merchantDetailsDTO1, MerchantDetails.class));
			merchantRepository.save(modelMapper().map(merchantDetailsDTO2, MerchantDetails.class));
			merchantRepository.save(modelMapper().map(merchantDetailsDTO3, MerchantDetails.class));

		} catch (Exception e) {
			throw new GenericException("merchant already added");
		}
	}
}
