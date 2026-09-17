package com.example.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor

public class JWT_AuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    // we need to create bean of it .
    private final UserDetailsService userDetailsService;


    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        System.out.println("JWT FILTER HIT");

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail ;

        if(authHeader == null || !authHeader.startsWith("Bearer ")){
            filterChain.doFilter(request , response);
            return;
        }

        //extracting token from authentication/authorization header
        jwt = authHeader.substring(7);

        //extracting userEmail from jwtheader (we need a class to manipulate jwttoken)
        userEmail = jwtService.extractUsername(jwt);

        //validate jwt (if user is already authenticated we do not need to check again)
        if(userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null){
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            //if the user is valid , token is valid then we store the context.
            if(jwtService.isTokenValid(jwt , userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                //update security context holder
                SecurityContextHolder.getContext().setAuthentication(authToken);

            }
        }

        //always call (important)
        filterChain.doFilter(request , response);




    }
}
