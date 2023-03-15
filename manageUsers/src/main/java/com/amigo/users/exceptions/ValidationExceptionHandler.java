package com.amigo.users.exceptions;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;

 
@RestControllerAdvice
public class ValidationExceptionHandler
{
	
	//Handler for validation failures w.r.t DTOs
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorMessage> methodArgumentExceptionHandler(MethodArgumentNotValidException ex) 
	{
		return createErrorMessage(ex.getBindingResult().getAllErrors().stream().map(ObjectError::getDefaultMessage)
                .collect(Collectors.joining(", ")),HttpStatus.BAD_REQUEST);
	}
	@ExceptionHandler(CustomerException.class)
	public ResponseEntity<ErrorMessage> customerExceptionHandler(CustomerException ex) 
	{
		
		return createErrorMessage(ex.getMessage(),HttpStatus.BAD_REQUEST);
	}
	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<ErrorMessage> unauthorizedExceptionHandler(UnauthorizedException ex) 
	{
		
		return createErrorMessage(ex.getMessage(),HttpStatus.UNAUTHORIZED);
	}
	@ExceptionHandler(ServletRequestBindingException.class)
    protected ResponseEntity<ErrorMessage> handleServletRequestBindingException(ServletRequestBindingException ex) {
		return createErrorMessage(ex.getMessage()+", for token user first need to login",HttpStatus.NOT_FOUND);
    }
	@ExceptionHandler({MalformedJwtException.class,SignatureException.class})
    protected ResponseEntity<ErrorMessage> jwtException(Exception ex) {
		return createErrorMessage("user not logged ",HttpStatus.UNAUTHORIZED);
    }
	
	private ResponseEntity<ErrorMessage> createErrorMessage(String msg,HttpStatus errorCode) {
		ErrorMessage error = new ErrorMessage();
		error.setErrorCode(errorCode.value());
		error.setMessage(msg);
		return new ResponseEntity<>(error, errorCode);
	}
	
} 