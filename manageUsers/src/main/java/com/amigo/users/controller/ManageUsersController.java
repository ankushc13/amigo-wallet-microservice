package com.amigo.users.controller;

import java.util.List;

import javax.validation.Valid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.amigo.users.authentication.JWTBuilder;
import com.amigo.users.dto.ChangePassword;
import com.amigo.users.dto.CustomerDTO;
import com.amigo.users.dto.Login;
import com.amigo.users.dto.ResetPassword;
import com.amigo.users.exceptions.CustomerException;
import com.amigo.users.exceptions.ErrorMessage;
import com.amigo.users.exceptions.UnauthorizedException;
import com.amigo.users.service.ManageUsersService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;


@RestController
public class ManageUsersController {
	Log logger = LogFactory.getLog(this.getClass());

	@Autowired
	RestTemplate restTemplate;
	@Autowired
	ManageUsersService manageUsersService;
	@Autowired
	JWTBuilder jwtBuilder;
	@Autowired
	DiscoveryClient discoveryClient;
	
	@GetMapping(value = "/customer/authenticate")
	public ResponseEntity<String> verifyUser(@RequestHeader("token") String token) throws UnauthorizedException{
		
		try {
			Claims userJws = jwtBuilder.parseJwt(token).getBody();

			String email = (String) userJws.get("email");
			if(manageUsersService.exist(email)) {
				return ResponseEntity.ok(email);
			}
			else {
				throw new UnauthorizedException("email does not exist");
			}
		} catch (Exception e) {
			throw new UnauthorizedException("user not logged in");
		}
		
	}
	@GetMapping(value = "/customer/authorize/admin")
	public ResponseEntity<Boolean> verifyAdmin(@RequestHeader("token") String token){
		
		Claims userJws = jwtBuilder.parseJwt(token).getBody();
		String role = (String) userJws.get("role");
		return ResponseEntity.ok(role.equalsIgnoreCase("admin"));
	}

	@PostMapping(value = "/customer", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> createUser(@Valid @RequestBody CustomerDTO customerDTO) throws JsonProcessingException, CustomerException {
		if (manageUsersService.exist(customerDTO.getEmail())) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already exist");
		}
		ObjectMapper objectMapper = new ObjectMapper();
		List<ServiceInstance> serviceInstancesList = discoveryClient.getInstances("WalletMS");
		manageUsersService.createCustomer(customerDTO);
		try {
			String uri = serviceInstancesList.get(0).getUri().toString();
			// adding wallet for customer
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<String> entity = new HttpEntity<>(headers);
			restTemplate.postForEntity(uri+"/wallet/add/"+customerDTO.getEmail(), entity, String.class);
		} catch (HttpStatusCodeException exception) {
			throw new CustomerException((objectMapper.readValue(exception.getResponseBodyAsString(),ErrorMessage.class)).getMessage());
		}
		return ResponseEntity.ok("Success");
	}

	@PostMapping(value = "/customer/login", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> loginUser(@RequestBody Login login)
			throws CustomerException {
		// logout previous customer
		if (manageUsersService.login(login)) {
			String token = jwtBuilder.generateToken(login);
			return ResponseEntity.ok().header("token",token).body("login success");
		}
		return ResponseEntity.internalServerError().body("login failed");
	}

	@PostMapping(value = "/customer/change-password", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePassword changePassword) {
		if (!manageUsersService.verifyEmailAndPassword(changePassword)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("email or current passowrd is incorrect");
		}
		manageUsersService.changePassword(changePassword.getEmail(), changePassword.getNewPassword());
		return ResponseEntity.ok("Success");
	}

	@PostMapping(value = "/customer/reset-password", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> changePassword(@Valid @RequestBody ResetPassword resetPassword) {
		if (!manageUsersService.exist(resetPassword.getEmail())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("email is incorrect");
		}
		return ResponseEntity.ok("new password: " + manageUsersService.resetPassword(resetPassword));
	}
}
