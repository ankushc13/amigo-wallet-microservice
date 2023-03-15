package com.amigo.wallet.service;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.amigo.wallet.dto.BankDTO;
import com.amigo.wallet.dto.TransactionDTO;
import com.amigo.wallet.dto.WalletDTO;
import com.amigo.wallet.entity.Bank;
import com.amigo.wallet.entity.Wallet;
import com.amigo.wallet.exception.CustomerException;
import com.amigo.wallet.exception.ErrorMessage;
import com.amigo.wallet.exception.GenericException;
import com.amigo.wallet.repository.AddBankRepository;
import com.amigo.wallet.repository.WalletRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Service
public class WalletService {

	@Autowired
	WalletRepository loadWalletRepository;
	@Autowired
	AddBankRepository addBankRepository;
	@Autowired
	ModelMapper modelMapper;
	@Autowired
	DiscoveryClient discoveryClient;
	@Autowired
	RestTemplate restTemplate;
	private static final DecimalFormat decfor = new DecimalFormat("0.00");  
	private String credited = "credited";
	private String debited = "debited";
	private String walletInvalid = "given wallet email is incorrect";
	private String walletNotFound = "no wallet found";
    private Random random = new Random();

	public void createWallet(String email) throws CustomerException {
		Optional<Wallet> walletOptional=loadWalletRepository.findByEmail(email);
		if(!walletOptional.isPresent()) {
			Wallet wallet = new Wallet();
			wallet.setEmail(email);
			wallet.setWalletAmount(0.0);
			loadWalletRepository.saveAndFlush(wallet);
		}
		else {
			throw new CustomerException("wallet already exist");
		}
	}
	
	
	public WalletDTO loadWallet(String email) throws CustomerException {		
		Optional<Wallet> walletOptional=loadWalletRepository.findByEmail(email);
		if(!walletOptional.isPresent()) {
			throw new CustomerException(walletNotFound);
		}
		return modelMapper.map(walletOptional.get(), WalletDTO.class);
	}
		
	public void addBank(BankDTO bankDTO, String email) throws CustomerException {
		Optional<Bank> bankOptional=addBankRepository
				.findByBankCodeAndAccountNumberAndHolderName(bankDTO.getBankCode(),bankDTO.getAccountNumber(),bankDTO.getHolderName());
		Bank bank;
		if(bankOptional.isPresent()) {
			bank = bankOptional.get();
		}
		else {
			bankDTO.setAmount(getRandomNumber(10000, 30000));
			bank = modelMapper.map(bankDTO, Bank.class);
		}
		addBankRepository.saveAndFlush(bank);
		//add bank id to wallet
		WalletDTO walletDTO = loadWallet(email);
		walletDTO.setBankId(bank.getBankId());
		Wallet wallet = modelMapper.map(walletDTO, Wallet.class);
		loadWalletRepository.saveAndFlush(wallet);
	}
	public void addMoney(String email, Double amount) throws CustomerException, JsonProcessingException, GenericException {
		Optional<Wallet> walletOptional=loadWalletRepository.findByEmail(email);
		if(!walletOptional.isPresent()) {
			throw new CustomerException(walletNotFound);
		}
		if(walletOptional.get().getBankId()==null) {
			throw new CustomerException("no bank account found");
		}
		Optional<Bank> bankOptional=addBankRepository.findById(walletOptional.get().getBankId());
		if(!bankOptional.isPresent()) {
			throw new CustomerException("no bank account found");
		}
		Bank bank = bankOptional.get();
		if(bank.getAmount()<amount) {
			throw new CustomerException("insufficent bank balance");
		}
		bank.setAmount(Double.parseDouble(decfor.format(bank.getAmount()-amount)));
		addBankRepository.save(bank);
		Wallet wallet = walletOptional.get();
		wallet.setWalletAmount(wallet.getWalletAmount()+amount);
		loadWalletRepository.save(wallet);
		addBankRepository.flush();
		loadWalletRepository.flush();

		createTransaction(wallet.getWalletId(),email,credited,amount,bank.getAccountNumber(),debited);
		
	}


	public void tranferToBank(BankDTO bankDTO, String email) throws CustomerException, JsonProcessingException, GenericException {
		Optional<Bank> bankOptional=addBankRepository
				.findByBankCodeAndAccountNumberAndHolderName(bankDTO.getBankCode(),bankDTO.getAccountNumber(),bankDTO.getHolderName());
		if(!bankOptional.isPresent()) {
			throw new CustomerException("bank code or account number or holder name is incorrect");
		}
		Optional<Wallet> walletOptional=loadWalletRepository.findByEmail(email);
		if(!walletOptional.isPresent()) {
			throw new CustomerException(walletNotFound);
		}
		Wallet wallet = walletOptional.get();

		if(wallet.getWalletAmount()<bankDTO.getAmount()) {
			throw new CustomerException("insufficent wallet balance");
		}

		Bank bank = bankOptional.get();
		bank.setAmount(bank.getAmount()+bankDTO.getAmount());
		addBankRepository.save(bank);
		wallet.setWalletAmount(Double.parseDouble(decfor.format(wallet.getWalletAmount()-bankDTO.getAmount())));
		loadWalletRepository.save(wallet);
		
		addBankRepository.flush();
		loadWalletRepository.flush();
		
		createTransaction(wallet.getWalletId(),email,debited,bankDTO.getAmount(),bank.getAccountNumber(),credited);

	}
	public Wallet tranferToWallet(WalletDTO walletDTO, String email, Integer rewardPoint) throws JsonProcessingException, GenericException, CustomerException{
			Optional<Wallet> walletReciverOptional=loadWalletRepository.findByEmail(walletDTO.getEmail());
			if(!walletReciverOptional.isPresent()) {
				throw new CustomerException(walletInvalid);
			}
			Optional<Wallet> walletOptional=loadWalletRepository.findByEmail(email);
			if(!walletOptional.isPresent()) {
				throw new CustomerException("no wallet found for client");
			}
			Wallet wallet = walletOptional.get();

			if(wallet.getWalletAmount()<walletDTO.getWalletAmount()) {
				throw new CustomerException("insufficent wallet balance");
			}

			Wallet walletReciver = walletReciverOptional.get();
			walletReciver.setWalletAmount(walletReciver.getWalletAmount()+walletDTO.getWalletAmount());
			loadWalletRepository.save(walletReciver);
			wallet.setWalletAmount(Double.parseDouble(decfor.format(wallet.getWalletAmount()-walletDTO.getWalletAmount())));
			wallet.setRewardPoint(wallet.getRewardPoint()+rewardPoint);
			loadWalletRepository.save(wallet);
			loadWalletRepository.flush();
			
			createTransaction(wallet.getWalletId(),email,debited,walletDTO.getWalletAmount(),walletReciver.getEmail(),credited);
			createTransaction(walletReciver.getWalletId(),walletReciver.getEmail(),credited,walletDTO.getWalletAmount(),wallet.getEmail(),debited);

			return wallet;
	}
	
	public Integer getWalletId(String email) throws CustomerException {
		Optional<Wallet> walletOptional=loadWalletRepository.findByEmail(email);
		if(!walletOptional.isPresent()) {
			throw new CustomerException(walletInvalid);
		}
		return walletOptional.get().getWalletId();
	}
	
	
	private void createTransaction(Integer walletId, String email, String type1, Double amount, String holderId,
			String type2) throws GenericException, JsonProcessingException, CustomerException {
		
	    LocalDateTime dateTimeNow = LocalDateTime.now();
		String info = String.format("wallet %s %s for %s on %s; %s %s", email,type1,amount,dateTimeNow,holderId,type2);
		
		TransactionDTO transactionDTO = new TransactionDTO();
		transactionDTO.setAmount(amount);
		transactionDTO.setInfo(info);
		transactionDTO.setStatus("success");
		transactionDTO.setWalletId(walletId);
		transactionDTO.setTransactionDateTime(LocalDateTime.now());
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(transactionDTO),headers);
		List<ServiceInstance> serviceInstancesList = discoveryClient.getInstances("TransactionMS");
		if(!ObjectUtils.isEmpty(serviceInstancesList)) {
			try {
				String uri = serviceInstancesList.get(0).getUri().toString();
				restTemplate.postForEntity(uri+"/transaction/add", entity, String.class);
			} catch (HttpStatusCodeException exception) {
				throw new CustomerException((objectMapper.readValue(exception.getResponseBodyAsString(),ErrorMessage.class)).getMessage());
			}
		}
		else {
			throw new GenericException("internal server error");
		}	
		
	}


	public String getEmail(Integer walletId) throws CustomerException {
		Optional<Wallet> walletReciverOptional=loadWalletRepository.findById(walletId);
		if(!walletReciverOptional.isPresent()) {
			throw new CustomerException("given wallet id is incorrect");
		}
		return walletReciverOptional.get().getEmail();
	}


	public WalletDTO payBill(WalletDTO walletDTO, String emailId) throws JsonProcessingException, GenericException, CustomerException{
		Integer rewardPoint= calculateRewardPoints(walletDTO.getWalletAmount());
		Wallet wallet = tranferToWallet(walletDTO, emailId, rewardPoint);
		
		return modelMapper.map(wallet,WalletDTO.class);
	}
	public void addCashBack(String email, Double amount) throws CustomerException, JsonProcessingException, GenericException {
		Optional<Wallet> walletOptional=loadWalletRepository.findByEmail(email);
		if(!walletOptional.isPresent()) {
			throw new CustomerException(walletInvalid);
		}
		Wallet wallet = walletOptional.get();
		wallet.setWalletAmount(wallet.getWalletAmount()+amount);
		loadWalletRepository.saveAndFlush(wallet);
		
		createTransaction(getWalletId(email),email,credited,amount,"","");
	}
	private double getRandomNumber(double min, double max) {
	    double num = random.nextDouble()*(max - min) + min;
	    return Double.parseDouble(decfor.format(num));
	}

	private Integer calculateRewardPoints(Double walletAmount) {
		return (int) (getRandomNumber(0.0,10.0)*(walletAmount/200.0));
	}
	
	
}
