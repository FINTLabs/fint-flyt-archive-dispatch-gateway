package no.fintlabs;

import io.netty.channel.ChannelOption;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "fint.client")
public class WebClientConfiguration {

    private String baseUrl;
    private String username;
    private String password;
    private String registrationId;

    @Bean
    public Authentication dummyAuthentication() {
        return new UsernamePasswordAuthenticationToken("fint", "client", Collections.emptyList());
    }

    @Bean
    @ConditionalOnProperty(name = "fint.dispatch-gateway.authorization.enable", havingValue = "true")
    public ReactiveOAuth2AuthorizedClientManager authorizedClientManager(ReactiveClientRegistrationRepository clientRegistrationRepository,
                                                                         ReactiveOAuth2AuthorizedClientService authorizedClientService) {

        ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider = ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                .password()
                .refreshToken()
                .build();

        AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager =
                new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientService);

        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        authorizedClientManager.setContextAttributesMapper(contextAttributesMapper());

        return authorizedClientManager;
    }

    private Function<OAuth2AuthorizeRequest, Mono<Map<String, Object>>> contextAttributesMapper() {
        return authorizeRequest -> {
            Map<String, Object> contextAttributes = new HashMap<>();
            contextAttributes.put(OAuth2AuthorizationContext.USERNAME_ATTRIBUTE_NAME, username);
            contextAttributes.put(OAuth2AuthorizationContext.PASSWORD_ATTRIBUTE_NAME, password);
            return Mono.just(contextAttributes);
        };
    }

    @Bean
    public ClientHttpConnector clientHttpConnector() {
        return new ReactorClientHttpConnector(HttpClient.create(
                        ConnectionProvider
                                .builder("laidback")
                                .maxLifeTime(Duration.ofMinutes(30))
                                .maxIdleTime(Duration.ofMinutes(5))
                                .build())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 300000)
                .responseTimeout(Duration.ofMinutes(5)
                ));
    }

    @Bean
    public WebClient webClient(Optional<ReactiveOAuth2AuthorizedClientManager> authorizedClientManager, ClientHttpConnector clientHttpConnector) {
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(-1))
                .build();

        WebClient.Builder webClientBuilder = WebClient.builder();

        authorizedClientManager.ifPresent(presentAuthorizedClientManager -> {
            ServerOAuth2AuthorizedClientExchangeFilterFunction authorizedClientExchangeFilterFunction =
                    new ServerOAuth2AuthorizedClientExchangeFilterFunction(presentAuthorizedClientManager);
            authorizedClientExchangeFilterFunction.setDefaultClientRegistrationId(registrationId);
            webClientBuilder.filter(authorizedClientExchangeFilterFunction);
        });

        return webClientBuilder
                .clientConnector(clientHttpConnector)
                .exchangeStrategies(exchangeStrategies)
                .baseUrl(baseUrl)
                .build();
    }

}