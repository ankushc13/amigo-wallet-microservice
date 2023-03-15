package com.amigo.wallet.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.amigo.wallet.entity.Wallet;

public interface WalletRepository extends JpaRepository<Wallet, Integer> {

	boolean existsByEmail(String email);

	Optional<Wallet> findByEmail(String email);

	
}
