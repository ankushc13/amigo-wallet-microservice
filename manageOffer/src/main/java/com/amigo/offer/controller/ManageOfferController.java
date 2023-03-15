package com.amigo.offer.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.amigo.offer.dto.OffersDTO;
import com.amigo.offer.exceptions.CustomerException;
import com.amigo.offer.exceptions.ErrorMessage;
import com.amigo.offer.exceptions.GenericException;
import com.amigo.offer.service.ManageOffersService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class ManageOfferController {

	@Autowired
	ManageOffersService manageOffersService;
	@Autowired
	DiscoveryClient discoveryClient;
	@Autowired
	RestTemplate restTemplate;

	@GetMapping(value = "/offers/all")
	public ResponseEntity<List<OffersDTO>> showAllOffers() {

		return ResponseEntity.status(HttpStatus.OK).body(manageOffersService.showAll());

	}

	@GetMapping(value = "/offers/{offerCode}")
	public ResponseEntity<Integer> getOffer(@PathVariable String offerCode) {

		return ResponseEntity.status(HttpStatus.OK).body(manageOffersService.getOfferAmount(offerCode));

	}

	@PostMapping(value = "/offers/add")
	public ResponseEntity<String> addOffer(@RequestBody OffersDTO offersDTO, @RequestHeader("token") String token)
			throws GenericException, JsonProcessingException, CustomerException {
		List<ServiceInstance> serviceInstancesList = discoveryClient.getInstances("ManageUsersMS");
		ObjectMapper objectMapper = new ObjectMapper();
		if (!ObjectUtils.isEmpty(serviceInstancesList)) {
			try {
				String uri = serviceInstancesList.get(0).getUri().toString();
				HttpHeaders headers = new HttpHeaders();
				headers.set("token", token);
				HttpEntity<String> requestEntity = new HttpEntity<>(headers);
				restTemplate.exchange(uri + "/customer/authenticate", HttpMethod.GET,
						requestEntity, String.class);
				ResponseEntity<Boolean> isAdmin = restTemplate.exchange(uri + "/customer/authorize/admin",
						HttpMethod.GET, requestEntity, Boolean.class);
				
				if (manageOffersService.exist(offersDTO.getOfferCode())) {
					return ResponseEntity.status(HttpStatus.OK).body("offer already exist");
				}
				if (Boolean.TRUE.equals(isAdmin.getBody())) {
					manageOffersService.addOffer(offersDTO);
					return ResponseEntity.status(HttpStatus.OK).body("Success");
				}
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("only admin has access");
			} catch (HttpStatusCodeException exception) {
				throw new CustomerException(
						(objectMapper.readValue(exception.getResponseBodyAsString(), ErrorMessage.class)).getMessage());
			}
		} else {
			throw new GenericException("internal server error");
		}

	}

}
