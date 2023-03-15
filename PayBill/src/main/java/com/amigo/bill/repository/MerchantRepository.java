package com.amigo.bill.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.amigo.bill.entity.MerchantDetails;


public interface MerchantRepository extends JpaRepository<MerchantDetails, Integer> {

	Optional<MerchantDetails> findByNameAndUtilityType(String name, String utilityType);


}
