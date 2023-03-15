package com.amigo.wallet.controller;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

import com.amigo.wallet.dto.BankDTO;
import com.amigo.wallet.dto.WalletDTO;
import com.amigo.wallet.exception.CustomerException;
import com.amigo.wallet.exception.ErrorMessage;
import com.amigo.wallet.exception.GenericException;
import com.amigo.wallet.service.WalletService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class WalletController {
	Log logger = LogFactory.getLog(this.getClass());

	@Autowired
	WalletService loadWalletService;
	@Autowired
	DiscoveryClient discoveryClient;
	@Autowired
	RestTemplate restTemplate;
	private String authURI = "/customer/authenticate";
	private String tokenConstant = "token";
	private String errorMsg = "internal server error";
	private String success = "success";
	private String userMS = "ManageUsersMS";

	
	@PostMapping(value = "/wallet/add/{email}")
	public ResponseEntity<String> addWallet(@PathVariable String email) throws CustomerException {
		loadWalletService.createWallet(email);
		return ResponseEntity.status(HttpStatus.OK).body(success);
	}
	
	@GetMapping(value = "/wallet/load")
	public ResponseEntity<WalletDTO> loadWallet(@RequestHeader("token") String token) throws GenericException, JsonProcessingException, CustomerException {
		
		List<ServiceInstance> serviceInstancesList = discoveryClient.getInstances(userMS);
		ObjectMapper objectMapper = new ObjectMapper();
		if(!ObjectUtils.isEmpty(serviceInstancesList)) {
			try {
				String uri = serviceInstancesList.get(0).getUri().toString();
				HttpHeaders headers = new HttpHeaders();
				headers.set(tokenConstant, token);
				HttpEntity<String> requestEntity = new HttpEntity<>(headers);
				ResponseEntity<String> response = restTemplate.exchange(
						uri+authURI, HttpMethod.GET, requestEntity, String.class);

				String email = response.getBody();
				return ResponseEntity.status(HttpStatus.OK).body(loadWalletService.loadWallet(email));				
			} catch (HttpStatusCodeException exception) {
				throw new CustomerException((objectMapper.readValue(exception.getResponseBodyAsString(),ErrorMessage.class)).getMessage());
			}
		}
		else {
			throw new GenericException(errorMsg);
		}		
	}
	@PostMapping(value = "/wallet/add-bank") 
	public ResponseEntity<String> addBank(@RequestBody BankDTO bankDTO,@RequestHeader("token") String token) throws GenericException, JsonProcessingException, CustomerException{
		List<ServiceInstance> serviceInstancesList = discoveryClient.getInstances(userMS);
		ObjectMapper objectMapper = new ObjectMapper();
		if(!ObjectUtils.isEmpty(serviceInstancesList)) {
			try {
				String uri = serviceInstancesList.get(0).getUri().toString();
				HttpHeaders headers = new HttpHeaders();
				headers.set(tokenConstant, token);
				HttpEntity<String> requestEntity = new HttpEntity<>(headers);
				ResponseEntity<String> response = restTemplate.exchange(
						uri+authURI, HttpMethod.GET, requestEntity, String.class);
				String email = response.getBody();
				loadWalletService.addBank(bankDTO,email);
				return ResponseEntity.status(HttpStatus.OK).body(success);
			} catch (HttpStatusCodeException exception) {
				throw new CustomerException((objectMapper.readValue(exception.getResponseBodyAsString(),ErrorMessage.class)).getMessage());
			}
		}
		else {
			throw new GenericException(errorMsg);
		}	

	}
	@PostMapping(value = "/wallet/topup/{amount}") 
	public ResponseEntity<String> walletTopUp(@PathVariable Double amount,@RequestHeader("token") String token) throws GenericException, JsonProcessingException, CustomerException{
		List<ServiceInstance> serviceInstancesList = discoveryClient.getInstances(userMS);
		ObjectMapper objectMapper = new ObjectMapper();
		if(!ObjectUtils.isEmpty(serviceInstancesList)) {
			try {
				String uri = serviceInstancesList.get(0).getUri().toString();
				HttpHeaders headers = new HttpHeaders();
				headers.set(tokenConstant, token);
				HttpEntity<String> requestEntity = new HttpEntity<>(headers);
				ResponseEntity<String> response = restTemplate.exchange(
						uri+authURI, HttpMethod.GET, requestEntity, String.class);
				String email = response.getBody();
				if(amount<100) {
					throw new CustomerException("minimum amount cannot be less than 100 USD");
				}
				loadWalletService.addMoney(email,amount);
				return ResponseEntity.status(HttpStatus.OK).body(success);
			} catch (HttpStatusCodeException exception) {
				throw new CustomerException((objectMapper.readValue(exception.getResponseBodyAsString(),ErrorMessage.class)).getMessage());
			}
		}
		else {
			throw new GenericException(errorMsg);
		}			
	}
	@PostMapping(value = "/wallet/tranfer-bank/{email}") 
	public ResponseEntity<String> tranferToBank(@RequestBody BankDTO bankDTO,@PathVariable String email) throws JsonProcessingException, CustomerException, GenericException{
		loadWalletService.tranferToBank(bankDTO,email);
		return ResponseEntity.status(HttpStatus.OK).body(success);
	}
	@PostMapping(value = "/wallet/tranfer-wallet/{email}") 
	public ResponseEntity<String> tranferToWallet(@RequestBody WalletDTO walletDTO,@PathVariable String email) throws JsonProcessingException, GenericException, CustomerException{
		loadWalletService.tranferToWallet(walletDTO,email,0);
		return ResponseEntity.status(HttpStatus.OK).body(success);
	}
	@PostMapping(value = "/wallet/paybill/{email}") 
	public ResponseEntity<WalletDTO> payBill(@RequestBody WalletDTO walletDTO,@PathVariable String email) throws JsonProcessingException, GenericException, CustomerException{
		return ResponseEntity.status(HttpStatus.OK).body(loadWalletService.payBill(walletDTO,email));
	}
	@GetMapping(value = "/wallet/fetch-wallet-id/{email}")
	public ResponseEntity<Integer> getWalletId(@PathVariable String email) throws CustomerException{
		return ResponseEntity.status(HttpStatus.OK).body(loadWalletService.getWalletId(email)); 
	}
	@PostMapping(value = "/wallet/{email}/{amount}") 
	public ResponseEntity<String> addCashBack(@PathVariable String email,@PathVariable Double amount) throws JsonProcessingException, CustomerException, GenericException{
		loadWalletService.addCashBack(email,amount);
		return ResponseEntity.status(HttpStatus.OK).body(success);
	}
	
}
