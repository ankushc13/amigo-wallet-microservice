package com.amigo.cashback.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.amigo.cashback.dto.TransactionDTO;
import com.amigo.cashback.exceptions.CustomerException;
import com.amigo.cashback.exceptions.ErrorMessage;
import com.amigo.cashback.exceptions.GenericException;
import com.amigo.cashback.service.CashBackService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class CashBackController {
	
	@Autowired
	RestTemplate restTemplate;
	@Autowired
	CashBackService cashBackService;
	@Autowired
	DiscoveryClient discoveryClient;

	
	@GetMapping(value = "/cashback/{transactionId}/{offerCode}")
	public ResponseEntity<String> getCashBack(@PathVariable String transactionId,@PathVariable String offerCode, @RequestHeader("token") String token) throws JsonProcessingException, CustomerException, GenericException {
		List<ServiceInstance> usersInstances = discoveryClient.getInstances("ManageUsersMS");
		List<ServiceInstance> walletInstances = discoveryClient.getInstances("WalletMS");
		List<ServiceInstance> offerInstances = discoveryClient.getInstances("OfferMS");
		List<ServiceInstance> transactionInstances = discoveryClient.getInstances("TransactionMS");


		ObjectMapper objectMapper = new ObjectMapper();
		if (!ObjectUtils.isEmpty(usersInstances) && !ObjectUtils.isEmpty(walletInstances)
				&& !ObjectUtils.isEmpty(offerInstances) && !ObjectUtils.isEmpty(transactionInstances)) {
			try {
				//authenticate user
				String userURI = usersInstances.get(0).getUri().toString();
				HttpHeaders headers = new HttpHeaders();
				headers.set("token", token);
				HttpEntity<String> requestEntity = new HttpEntity<>(headers);
				ResponseEntity<String> emailEntity = restTemplate.exchange(userURI + "/customer/authenticate", HttpMethod.GET, requestEntity, String.class);
				String email = emailEntity.getBody();
				
				//get offer amount 
				String offerURI = offerInstances.get(0).getUri().toString();
				ResponseEntity<Integer> offerAmount = restTemplate.
					      getForEntity(offerURI+"/offers/"+offerCode, Integer.class);
				//get wallet id
				String walletURI = walletInstances.get(0).getUri().toString();
				ResponseEntity<Integer> response = restTemplate.
					      getForEntity(walletURI+"/wallet/fetch-wallet-id/"+email, Integer.class);
				Integer walletId = response.getBody();
				
				//get transaction
				String transactionURI = transactionInstances.get(0).getUri().toString();
				ResponseEntity<TransactionDTO> transaction = restTemplate.
					      getForEntity(transactionURI+"/transaction/"+transactionId+"/"+walletId, TransactionDTO.class);
				double cashBackAmount = cashBackService.getCashBackAmount(transaction.getBody(), offerAmount.getBody());
				
				//add cash-back to wallet
				headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_JSON);
				HttpEntity<String> entity = new HttpEntity<>(headers);
				restTemplate.postForEntity(walletURI+"/wallet/"+email+"/"+cashBackAmount, entity, String.class);
				
				return ResponseEntity.ok("congrats! cashback amount: "+cashBackAmount+" has been added to you wallet");
			} catch (HttpStatusCodeException exception) {
				throw new CustomerException(
						(objectMapper.readValue(exception.getResponseBodyAsString(), ErrorMessage.class)).getMessage());
			}
		} else {
			throw new GenericException("internal server error");
		}
	}
}
