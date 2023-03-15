package com.amigo.users.service;

import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amigo.users.dto.ChangePassword;
import com.amigo.users.dto.CustomerDTO;
import com.amigo.users.dto.Login;
import com.amigo.users.dto.ResetPassword;
import com.amigo.users.entity.Customer;
import com.amigo.users.exceptions.CustomerException;
import com.amigo.users.repository.ManageUsersRepository;

@Service
public class ManageUsersService {
	
	@Autowired
	ManageUsersRepository manageUsersRepository;
	
	@Autowired
	ModelMapper modelMapper;
	
	//sign up
	public void createCustomer(CustomerDTO customerDTO) {
		Customer customer = this.modelMapper.map(customerDTO, Customer.class);
		manageUsersRepository.save(customer);
	}
	
	// Login
	public boolean login(Login loginDTO) throws CustomerException {
		Optional<Customer> customerOptional=manageUsersRepository.findByEmail(loginDTO.getEmail());
		if(!customerOptional.isPresent() 
				|| (!customerOptional.get().getPassword().equals(loginDTO.getPassword()))) {
			throw new CustomerException("email or password is incorrect");
		}
		Customer customer = customerOptional.get();
		loginDTO.setRole(customer.getRole());
		return true;
	}

    public boolean exist(String email){
       return manageUsersRepository.existsByEmail(email);
    }

	public void changePassword(String email, String newPassword) {
		Optional<Customer> customerOptional=manageUsersRepository.findByEmail(email);
		if(customerOptional.isPresent()) {
			Customer customer = customerOptional.get();
			customer.setPassword(newPassword);
			manageUsersRepository.save(customer);
		}
	}

	public boolean verifyEmailAndPassword(ChangePassword changePasswordDTO) {
		Optional<Customer> customerOptional=manageUsersRepository.findByEmail(changePasswordDTO.getEmail());
		return customerOptional.isPresent() && customerOptional.get().getPassword().equals(changePasswordDTO.getCurrentPassword());
	}

	public String resetPassword(ResetPassword resetPassword) {
		String defaultNewPasswordString = "password";
		changePassword(resetPassword.getEmail(), defaultNewPasswordString);
		
		return defaultNewPasswordString;

		
	}

}
