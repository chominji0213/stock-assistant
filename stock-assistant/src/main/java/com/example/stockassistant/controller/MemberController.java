package com.example.stockassistant.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.stockassistant.dto.SignupForm;
import com.example.stockassistant.service.MemberService;

import lombok.RequiredArgsConstructor;


@Controller
@RequiredArgsConstructor
public class MemberController {
	
	private final MemberService memberService;
	
	/**
	 * 로그인 화면
	 * @return
	 */
	@GetMapping("/login")
	public String login() {

		return "member/login";
	}
	
	/**
	 * 회원가입 화면
	 * @param model
	 * @return
	 */
	@GetMapping("/signup")
	public String signup(Model model) {

		return "member/signup";
	}

	/**
	 * 회원가입 저장
	 * @param form
	 * @return
	 */
	@PostMapping("/signup")
	public String signup(SignupForm form, Model model) {
		
		try {
			memberService.regist(form);
		} catch (IllegalStateException  e) {
			// TODO: handle exception
			model.addAttribute("errorMessage", e.getMessage());
			
			return "member/signup";
		}
				
		return "redirect:/login";
	}
	
	/**
	 * 로그인 아이디 중복 체크
	 * @param value
	 * @return
	 */
	@GetMapping("/api/members/check-login-id")
	@ResponseBody
	public boolean checkLoginId(@RequestParam String value) {
		return memberService.isCheckLoginId(value);
	}
	
	/**
	 * 닉네임 중복 체크
	 * @param value
	 * @return
	 */
	@GetMapping("/api/members/check-nickname")
	@ResponseBody
	public boolean checkNickname(@RequestParam String value) {
		return memberService.isCheckNickName(value);
	}
}
