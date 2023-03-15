package com.amigo.users.authentication;

import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import com.amigo.users.dto.Login;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Component
public class JWTBuilder {
	
	private static final String SECRET = "c4c7ce3d18c2c8c52fc91179c2fd170fb66fcaf9db8c2e93fbf87d84107b7192";
	private static Key hmacKey = new SecretKeySpec(Base64.getDecoder().decode(SECRET), 
            SignatureAlgorithm.HS256.getJcaName());
	private static final int EXPIRY_DAYS = 1;
	Instant now = Instant.now();
	
	public String generateToken(Login login) {

		return Jwts.builder()
		        .claim("email", login.getEmail())
		        .claim("role", login.getRole())
		        .setSubject("amigo")
		        .setId(UUID.randomUUID().toString())
		        .setIssuedAt(Date.from(now))
		        .setExpiration(Date.from(now.plus(EXPIRY_DAYS, ChronoUnit.DAYS)))
		        .signWith(hmacKey)
		        .compact();
	}
	
	public Jws<Claims> parseJwt(String jwtString) {
	    
	    return Jwts.parserBuilder()
	            .setSigningKey(hmacKey)
	            .build()
	            .parseClaimsJws(jwtString);
	}
    
}