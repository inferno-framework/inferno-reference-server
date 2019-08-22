package gov.onc.authorization;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/*
 * Controller that serves the src/main/webapp/WEB-INF/templates/authorization.html template 
 */
@Controller
@RequestMapping("/oauth")
public class AuthorizationViewController  {
	
	@RequestMapping("/authorization")
	public String showAuthorizationView()
	{
		return "authorization"; //String will be mapped to corresponding template html file
	}

}
