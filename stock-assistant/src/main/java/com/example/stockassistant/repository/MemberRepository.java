package com.example.stockassistant.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.stockassistant.domain.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {
	
	/**
	 * 아이디로 회원 조회
	 * @param loginId
	 * @return
	 */
	Optional<Member> findByLoginId(String loginId);
	
	/**
	 * 아이디 중복 체크
	 * @param loginId
	 * @return
	 */
	boolean existsByLoginId(String loginId);
	
	/**
	 * 닉네임 중복 체크
	 * @param nickname
	 * @return
	 */
	boolean existsByNickname(String nickname);
	
	/**
	 * 이메일 중복 체크
	 * @param email
	 * @return
	 */
	boolean existsByEmail(String email);
}
