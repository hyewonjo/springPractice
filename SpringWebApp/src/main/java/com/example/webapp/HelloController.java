package com.example.webapp;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

public class HelloController implements Controller {
	// 부모 컨텍스트인 루트 컨텍스트로부터 HelloSpring 빈을 DI 받을 수 있다.
	@Autowired HelloSpring helloSpring;

	@Override
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		// 1. 사용자 요청 해석(get방식으로 보내도 받아짐)
		String name = request.getParameter("name");
		
		// 2. 비즈니스 로직 호출
		String message = this.helloSpring.sayHello(name);
		
		// 3. 모델 정보 생성
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("message", message);
		
		// 4. 뷰 지정
		return new ModelAndView("/WEB-INF/views/hello.jsp", model);
	}

}
