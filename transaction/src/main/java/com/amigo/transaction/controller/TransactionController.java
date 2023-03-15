package com.amigo.transaction.controller;

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

import com.amigo.transaction.dto.TransactionDTO;
import com.amigo.transaction.excpetions.CustomerException;
import com.amigo.transaction.excpetions.ErrorMessage;
import com.amigo.transaction.excpetions.GenericException;
import com.amigo.transaction.service.TransactionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class TransactionController {
	
	@Autowired
	TransactionService transactionService;
	@Autowired
	RestTemplate restTemplate;
	@Autowired
	DiscoveryClient discoveryClient;
	
	@PostMapping(value = "/transaction/add")
	public ResponseEntity<String> addWallet(@RequestBody TransactionDTO transactionDTO) {
		transactionService.addTransaction(transactionDTO);
		return ResponseEntity.status(HttpStatus.OK).body("success");
	}
	@GetMapping(value = "/transaction/page/{pageNo}")
	public ResponseEntity<List<TransactionDTO>> viewTransaction(@PathVariable Integer pageNo,@RequestHeader("token") String token) throws GenericException, JsonProcessingException, CustomerException {
		List<ServiceInstance> serviceInstancesList = discoveryClient.getInstances("ManageUsersMS");
		List<ServiceInstance> walletInstances = discoveryClient.getInstances("WalletMS");

		ObjectMapper objectMapper = new ObjectMapper();
		if(!ObjectUtils.isEmpty(serviceInstancesList) 
				&& !ObjectUtils.isEmpty(walletInstances)) {
			try {
				String uri = serviceInstancesList.get(0).getUri().toString();
				HttpHeaders headers = new HttpHeaders();
				headers.set("token", token);
				HttpEntity<String> requestEntity = new HttpEntity<>(headers);
				ResponseEntity<String> response = restTemplate.exchange(
						uri+"/customer/authenticate", HttpMethod.GET, requestEntity, String.class);
				String email = response.getBody();
				String walletURI = walletInstances.get(0).getUri().toString();
				ResponseEntity<Integer> walletId = restTemplate.
					      getForEntity(walletURI+"/wallet/fetch-wallet-id/"+email, Integer.class);
				return ResponseEntity.status(HttpStatus.OK).body(transactionService.showTransaction(walletId.getBody(),pageNo-1));
			} catch (HttpStatusCodeException exception) {
				throw new CustomerException((objectMapper.readValue(exception.getResponseBodyAsString(),ErrorMessage.class)).getMessage());
			}
		}
		else {
			throw new GenericException("internal server error");
		}	
	}
	
	@GetMapping(value = "/transaction/{transactionId}/{walletId}")
	public ResponseEntity<TransactionDTO> getTransaction(@PathVariable Integer transactionId,@PathVariable Integer walletId) throws CustomerException {
		return ResponseEntity.status(HttpStatus.OK).body(transactionService.getTransaction(transactionId,walletId));

	}
	

}
