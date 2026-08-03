package com.example.stockassistant.service;


import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.stockassistant.domain.Member;
import com.example.stockassistant.dto.SignupForm;
import com.example.stockassistant.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * 회원가입 폼 저장
     */
    @Override
    public void regist(SignupForm form) {   	
    	
    	// 비밀번호 확인
    	if(form.getPassword().equals(form.getPasswordConfirm()) == false) {
    		throw new IllegalStateException("비밀번호가 일치하지 않습니다.");
    	}
    	
    	// 필수 약관들 동의
    	if(form.isAgreeTerms() == false) {
    		throw new IllegalStateException("이용약관에 동의해야합니다.");
    	}
    	
    	if(form.isAgreePrivacy() == false) {
    		throw new IllegalStateException("개인정보 수집 및 이용에 동의해야 합니다.");
    	}
    	
    	// 아이디, 닉네임 중복체크
    	if(memberRepository.existsByLoginId(form.getUserId()) == true) {
    		throw new IllegalStateException("이미 사용 중인 아이디입니다.");
    	}
    	
    	if(memberRepository.existsByNickname(form.getNickname()) == true) {
    		throw new IllegalStateException("이미 사용 중인 닉네임입니다.");
    	}
    	
    	if(memberRepository.existsByEmail(form.getEmail()) == true) {
    		throw new IllegalStateException("이미 사용 중인 이메일입니다.");
    	}
    	
    	Member member = new Member();
    	member.setLoginId(form.getUserId());
    	member.setPassword(passwordEncoder.encode(form.getPassword()));
    	member.setNickname(form.getNickname());
    	member.setEmail(form.getEmail());
    	member.setPhone(form.getPhone());
    	member.setAgreeTermsAt(form.isAgreeTerms() ? LocalDateTime.now() : null);
    	member.setAgreePrivacyAt(form.isAgreePrivacy() ? LocalDateTime.now() : null);
    	member.setAgreeMarketingYn(form.isAgreeMarketing() ? "Y" : "N");
    	
    	memberRepository.save(member);
    }
    
    /**
     * 로그인 아이디 중복 체크
     */
    @Override
    public boolean isCheckLoginId(String loginId) {
    	return memberRepository.existsByLoginId(loginId);
    }
    
    /**
     * 닉네임 중복 체크
     */
    public boolean isCheckNickName(String nickname) {
    	return memberRepository.existsByNickname(nickname);
    }
}
