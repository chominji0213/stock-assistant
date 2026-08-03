package com.example.stockassistant.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 회원가입 폼을 받을 DTO
 */
@Getter
@Setter
public class SignupForm {
	/**
	 * 아이디
	 */
	private String userId;
	
	/**
	 * 비밀번호
	 */
	private String password;
	
	/**
	 * 비밀번호 확인
	 */
	private String passwordConfirm;
	
	/**
	 * 닉네임
	 */
	private String nickname;
	
	/**
	 * 이메일
	 */
	private String email;
	
	/**
	 * 전화번호
	 */
	private String phone;
	
	/**
	 * 필수 이용약관 동의
	 */
	private boolean agreeTerms;

	/**
	 * 필수 개인정보 수집 동의
	 */
	private boolean agreePrivacy;
	
	/**
	 * 마케팅 정보 동의
	 */
	private boolean agreeMarketing;
}
