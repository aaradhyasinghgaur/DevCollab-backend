package com.example.backend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;


@Service
public class JwtService {
    private static final String SECRET_KEY = "9a36badd031e945e7a8f00b9d60c37f91e77c1d3b7488751bd4f0ee2c73a0f7f";
    public String extractUsername(String token) {
        return extractClaim(token , Claims::getSubject);
    }

    public <T> T extractClaim(String token , Function<Claims , T> claimsResolver){
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    //gnerating token without the cliams.
    public String generateToken(UserDetails userDetails){
        return generateToken(new HashMap<>() , userDetails);
    }

    //to generate a token
    public String generateToken(Map<String , Object> extractClaims , UserDetails userDetails){
        return Jwts
                .builder()
                .claims(extractClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // for a day.
                .signWith(getSignInKey()) // deprecated signaturealgorithm (automatic).
                .compact();
    }

    //validating token
    public boolean isTokenValid(String token , UserDetails userDetails){
        final String username = extractUsername(token);
        boolean matches = username.equals(userDetails.getUsername());
        if (!matches && userDetails instanceof com.example.backend.entity.User user) {
            matches = username.equals(user.getEmail());
        }
        return matches && !isTokenExpired(token);
    }

    //token expired (by checking expiration date)
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token , Claims::getExpiration);
    }


    private Claims extractAllClaims(String token){
        return Jwts
                .parser()
                .verifyWith(getSignInKey()) // we need a sign in key of size of at least 256 bit.
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
