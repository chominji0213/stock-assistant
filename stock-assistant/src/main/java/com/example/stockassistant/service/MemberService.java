package com.example.stockassistant.service;

import com.example.stockassistant.dto.SignupForm;

public interface MemberService {
	
	/**
     * 회원가입 폼 저장
     */
	void regist(SignupForm form);
	
	/**
	 * 로그인 아이디 중복 체크
	 * @param loginId
	 * @return
	 */
	boolean isCheckLoginId(String loginId);
	
	/**
	 * 닉네임 중복 체크
	 * @param nickname
	 * @return
	 */
	boolean isCheckNickName(String nickname);
}
