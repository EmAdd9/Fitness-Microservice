package com.fitness.activityservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserValidationService {
    private final WebClient userServiceWebClient;

    public boolean validateUser(String userId){
        log.info("Calling USER Validation API for userId: {}", userId);
        try {
            return userServiceWebClient.get()
                    .uri("/api/users/{userId}/validate",userId)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();
        }catch (WebClientResponseException ex){
          if(ex.getStatusCode() == HttpStatus.NOT_FOUND)
              throw new RuntimeException("USER NOT FOUND WITH ID: "+userId);
          else  if(ex.getStatusCode() == HttpStatus.BAD_REQUEST)
                throw new RuntimeException("INVALID USER ID: "+userId);
        }
        return false;
    }
}
