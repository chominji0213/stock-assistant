package com.example.stockassistant.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.stockassistant.domain.Member;
import com.example.stockassistant.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

/**
 * Spring Security가 로그인 시도할 때 "회원 정보를 어디서, 어떻게 찾을지" 알려주는 클래스.
 * UserDetailsService를 구현하면 Spring Security가 인증 과정에서 자동으로 호출해준다.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService{
	private final MemberRepository memberRepository;

	/**
	 * 로그인 폼에서 입력한 아이디(loginId)를 가지고 Spring Security가 자동으로 호출하는 메서드.
	 * 여기서 리턴한 UserDetails의 password랑, 사용자가 입력한 비밀번호(암호화 후)를
	 * Spring Security가 알아서 비교해서 로그인 성공/실패를 판단한다.
	 *
	 * @param loginId 로그인 폼에 입력된 아이디
	 * @return Spring Security가 인증에 사용할 사용자 정보
	 * @throws UsernameNotFoundException 해당 아이디의 회원이 없을 때
	 */
	@Override
	public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException{
		// DB에서 아이디로 회원 조회, 없으면 예외를 던져서 로그인 실패 처리
		Member member = memberRepository.findByLoginId(loginId)
				.orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 아이디입니다." + loginId));

		// 조회한 Member 엔티티를 Spring Security가 이해하는 UserDetails 객체로 변환
		return User.builder()
				.username(member.getLoginId())     // 로그인 아이디
				.password(member.getPassword())    // 이미 암호화(BCrypt)된 값이 저장되어 있음
				.roles(member.getRole())           // "USER" -> 내부적으로 "ROLE_USER" 권한으로 취급됨
				.build();
	}
}
