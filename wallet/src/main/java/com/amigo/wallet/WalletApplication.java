package com.amigo.wallet;

import java.time.Duration;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import com.amigo.wallet.dto.BankDTO;
import com.amigo.wallet.dto.WalletDTO;
import com.amigo.wallet.entity.Bank;
import com.amigo.wallet.entity.Wallet;
import com.amigo.wallet.repository.AddBankRepository;
import com.amigo.wallet.repository.WalletRepository;

@SpringBootApplication
public class WalletApplication implements CommandLineRunner {

	@Autowired
	WalletRepository loadWalletRepository;
	@Autowired
	AddBankRepository addBankRepository;

	public static void main(String[] args) {
		SpringApplication.run(WalletApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		if (addBankRepository.findAll().isEmpty()) {
			addBank();
		}
		if (loadWalletRepository.findAll().isEmpty()) {
			addWallet();
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

	private void addBank() {
		// add offers if not exist
		BankDTO bankDTO1 = new BankDTO("ABCD123456", "123456", "Ankush", 10000d);
		BankDTO bankDTO2 = new BankDTO("ABCD123789", "456456", "Messi", 20000d);
		BankDTO bankDTO3 = new BankDTO("ABCD123321", "789456", "Chauhan", 30000d);
		addBankRepository.save(modelMapper().map(bankDTO1, Bank.class));
		addBankRepository.save(modelMapper().map(bankDTO2, Bank.class));
		addBankRepository.save(modelMapper().map(bankDTO3, Bank.class));
	}

	private void addWallet() {
		// add wallets if not exist
		WalletDTO walletDTO1 = new WalletDTO("abc@abc.com", 200d);
		WalletDTO walletDTO2 = new WalletDTO("def@abc.com", 300d);
		WalletDTO walletDTO3 = new WalletDTO("ghi@abc.com", 250d);

		// add merchant wallet if not exist
		WalletDTO walletDTO4 = new WalletDTO("jio@abc.com", 10000d);
		WalletDTO walletDTO5 = new WalletDTO("tata@abc.com", 10000d);
		WalletDTO walletDTO6 = new WalletDTO("gov@raj.com", 10000d);

		loadWalletRepository.save(modelMapper().map(walletDTO1, Wallet.class));
		loadWalletRepository.save(modelMapper().map(walletDTO2, Wallet.class));
		loadWalletRepository.save(modelMapper().map(walletDTO3, Wallet.class));
		loadWalletRepository.save(modelMapper().map(walletDTO4, Wallet.class));
		loadWalletRepository.save(modelMapper().map(walletDTO5, Wallet.class));
		loadWalletRepository.save(modelMapper().map(walletDTO6, Wallet.class));
	}
}
