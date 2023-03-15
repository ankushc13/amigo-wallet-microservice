package com.amigo.wallet.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.amigo.wallet.entity.Bank;


public interface AddBankRepository extends JpaRepository<Bank, Integer> {

	Optional<Bank> findByBankCodeAndAccountNumberAndHolderName(String bankCode, String accountNumber,
			String holderName);

}
