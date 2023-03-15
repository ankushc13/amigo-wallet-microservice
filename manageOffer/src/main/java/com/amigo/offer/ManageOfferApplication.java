package com.amigo.offer;

import java.time.Duration;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import com.amigo.offer.dto.OffersDTO;
import com.amigo.offer.entity.Offers;
import com.amigo.offer.repository.ManageOffersRepository;

@SpringBootApplication
public class ManageOfferApplication implements CommandLineRunner {
	@Autowired
	ManageOffersRepository manageOffersRepository;

	public static void main(String[] args) {
		SpringApplication.run(ManageOfferApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		if (manageOffersRepository.findAll().isEmpty()) {
			addOffer();
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

	private void addOffer() {
		// add offers if not exist
		OffersDTO offersDTO1 = new OffersDTO("ABC10", 10);
		OffersDTO offersDTO2 = new OffersDTO("ABC30", 30);
		OffersDTO offersDTO3 = new OffersDTO("ABC50", 50);
		manageOffersRepository.save(modelMapper().map(offersDTO1, Offers.class));
		manageOffersRepository.save(modelMapper().map(offersDTO2, Offers.class));
		manageOffersRepository.save(modelMapper().map(offersDTO3, Offers.class));
	}
}
