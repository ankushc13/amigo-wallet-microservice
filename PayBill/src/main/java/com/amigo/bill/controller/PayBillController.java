package com.amigo.bill.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.amigo.bill.dto.MerchantDetailsDTO;
import com.amigo.bill.dto.WalletDTO;
import com.amigo.bill.exceptions.CustomerException;
import com.amigo.bill.exceptions.ErrorMessage;
import com.amigo.bill.exceptions.GenericException;
import com.amigo.bill.service.PayBillService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class PayBillController {

	@Autowired
	RestTemplate restTemplate;
	@Autowired
	PayBillService payBillService;
	@Autowired
	DiscoveryClient discoveryClient;

	@GetMapping(value = "/paybill/merchant/load")
	public ResponseEntity<List<MerchantDetailsDTO>> showMerchants(@RequestHeader("token") String token)
			throws GenericException, JsonProcessingException, CustomerException {
		List<ServiceInstance> serviceInstancesList = discoveryClient.getInstances("ManageUsersMS");
		ObjectMapper objectMapper = new ObjectMapper();
		if (!ObjectUtils.isEmpty(serviceInstancesList)) {
			try {
				String uri = serviceInstancesList.get(0).getUri().toString();
				HttpHeaders headers = new HttpHeaders();
				headers.set("token", token);
				HttpEntity<String> requestEntity = new HttpEntity<>(headers);
				restTemplate.exchange(uri + "/customer/authenticate", HttpMethod.GET, requestEntity, String.class);

				return ResponseEntity.ok(payBillService.getMerchant());
			} catch (HttpStatusCodeException exception) {
				throw new CustomerException(
						(objectMapper.readValue(exception.getResponseBodyAsString(), ErrorMessage.class)).getMessage());
			}
		} else {
			throw new GenericException("internal server error");
		}
	}

	@PostMapping(value = "/paybill")
	public ResponseEntity<String> tranferToWallet(@RequestBody MerchantDetailsDTO merchantDetailsDTO,
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
				HttpEntity<String> entity = new HttpEntity<>(
						objectMapper.writeValueAsString(payBillService.prepareWallet(merchantDetailsDTO)), headers);
				String walletURI = walletInstances.get(0).getUri().toString();
				ResponseEntity<WalletDTO> walletResponse = restTemplate
						.postForEntity(walletURI + "/wallet/paybill/" + email, entity, WalletDTO.class);
				WalletDTO walletDTO = walletResponse.getBody();
				if(!ObjectUtils.isEmpty(walletDTO)) {
					return ResponseEntity.ok("bill successfully paid, your total reward point: "
							+ walletDTO.getRewardPoint());
				}
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
			} catch (HttpStatusCodeException exception) {
				throw new CustomerException(
						(objectMapper.readValue(exception.getResponseBodyAsString(), ErrorMessage.class)).getMessage());
			}
		} else {
			throw new GenericException("internal server error");
		}
	}

}
