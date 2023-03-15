package com.amigo.transfer.controller;

import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.amigo.transfer.dto.BankDTO;
import com.amigo.transfer.dto.WalletDTO;
import com.amigo.transfer.exceptions.CustomerException;
import com.amigo.transfer.exceptions.ErrorMessage;
import com.amigo.transfer.exceptions.GenericException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class TransferController {

	@Autowired
	RestTemplate restTemplate;
	@Autowired
	DiscoveryClient discoveryClient;

	@PostMapping(value = "/tranfer/bank")
	public ResponseEntity<String> tranferToBank(@Valid @RequestBody BankDTO bankDTO,
			@RequestHeader("token") String token) throws GenericException, JsonProcessingException, CustomerException {
		List<ServiceInstance> serviceInstancesList = discoveryClient.getInstances("ManageUsersMS");
		List<ServiceInstance> walletInstances = discoveryClient.getInstances("WalletMS");
		ObjectMapper objectMapper = new ObjectMapper();
		if (!ObjectUtils.isEmpty(serviceInstancesList) && !ObjectUtils.isEmpty(walletInstances)) {
			try {
				String uri = serviceInstancesList.get(0).getUri().toString();
				HttpHeaders headers = new HttpHeaders();
				headers.set("token", token);
				HttpEntity<String> requestEntity = new HttpEntity<>(headers);
				ResponseEntity<String> response = restTemplate.exchange(uri + "/customer/authenticate", HttpMethod.GET,
						requestEntity, String.class);
				String email = response.getBody();

				headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_JSON);
				requestEntity = new HttpEntity<>(objectMapper.writeValueAsString(bankDTO), headers);
				String walletURI = walletInstances.get(0).getUri().toString();
				response = restTemplate.postForEntity(walletURI + "/wallet/tranfer-bank/" + email, requestEntity,
						String.class);
				return response;
			} catch (HttpStatusCodeException exception) {
				throw new CustomerException(
						(objectMapper.readValue(exception.getResponseBodyAsString(), ErrorMessage.class)).getMessage());
			}
		} else {
			throw new GenericException("internal server error");
		}
	}

	@PostMapping(value = "/tranfer/wallet")
	public ResponseEntity<String> tranferToWallet(@Valid @RequestBody WalletDTO walletDTO,
			@RequestHeader("token") String token) throws GenericException, JsonProcessingException, CustomerException {
		List<ServiceInstance> serviceInstancesList = discoveryClient.getInstances("ManageUsersMS");
		List<ServiceInstance> walletInstances = discoveryClient.getInstances("WalletMS");
		ObjectMapper objectMapper = new ObjectMapper();
		if (!ObjectUtils.isEmpty(serviceInstancesList) && !ObjectUtils.isEmpty(walletInstances)) {
			try {
				String uri = serviceInstancesList.get(0).getUri().toString();
				HttpHeaders headers = new HttpHeaders();
				headers.set("token", token);
				HttpEntity<String> requestEntity = new HttpEntity<>(headers);
				ResponseEntity<String> response = restTemplate.exchange(uri + "/customer/authenticate", HttpMethod.GET,
						requestEntity, String.class);
				String email = response.getBody();

				headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_JSON);
				requestEntity = new HttpEntity<>(objectMapper.writeValueAsString(walletDTO), headers);
				String walletURI = walletInstances.get(0).getUri().toString();
				response = restTemplate.postForEntity(walletURI + "/wallet/tranfer-wallet/" + email, requestEntity,
						String.class);
				return response;
			} catch (HttpStatusCodeException exception) {
				throw new CustomerException(
						(objectMapper.readValue(exception.getResponseBodyAsString(), ErrorMessage.class)).getMessage());
			}
		} else {
			throw new GenericException("internal server error");
		}
	}

}
