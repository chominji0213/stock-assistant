package com.example.stockassistant.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

//회원 테이블
@Entity
@Table(name = "MEMBER")
@Getter
@Setter
@NoArgsConstructor
public class Member {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "member_no")
	private Long memberNo;
	
	@Column(name = "login_id", nullable = false, unique = true, length = 20)
	private String loginId;
	
	@Column(name = "password", nullable = false, length = 200)
	private String password;
	
	@Column(name = "nickname", nullable = false, unique = true, length = 20)
	private String nickname;
	
	@Column(name = "email", nullable = false, unique = true, length = 100)
	private String email;
	
	@Column(name = "phone", length = 20)
	private String phone;
	
	@Column(name = "profile_image_url", length = 500)
	private String profileImageUrl;
	
	@Column(name = "role", nullable = false, length=20)
	private String role = "USER";
	
	@Column(name = "status", nullable = false, length = 20)
	private String status = "ACTIVE";
	
	@Column(name = "agree_terms_at")
	private LocalDateTime agreeTermsAt;
	
	@Column(name = "agree_privacy_at")
	private LocalDateTime agreePrivacyAt;
	
	@Column(name = "agree_marketing_yn", nullable = false)
	private String agreeMarketingYn = "N";
	
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();
	
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt = LocalDateTime.now();
	
	@Column(name = "withdrawn_at")
	private LocalDateTime withdrawnAt;
}
