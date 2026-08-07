package com.example.stockassistant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
	@Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
	        .authorizeHttpRequests(auth -> auth
	        	.requestMatchers("/", "/login", "/signup", "/css/**", "/api/members/**", "/api/stocks/**").permitAll()
	            .requestMatchers("/admin/**").hasRole("ADMIN")
	            .anyRequest().authenticated()
	        )
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form
                    .loginPage("/login")             	
                    .loginProcessingUrl("/login")     // 폼이 제출되면 이 주소로
                    .usernameParameter("username")
                    .passwordParameter("password")
                    .defaultSuccessUrl("/", true)     // 성공하면 홈으로
                    .failureUrl("/login?error")       // 실패하면 로그인 화면으로 (?error 붙여서)
                    .permitAll()
                )
            .logout(logout -> logout		// 로그아웃
        	    .logoutUrl("/logout")
        	    .logoutSuccessUrl("/login")
        	    .permitAll()
        	);
        return http.build();
    }
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();	// 비밀번호를 암호화해서 저장하기 위함
	}
}
