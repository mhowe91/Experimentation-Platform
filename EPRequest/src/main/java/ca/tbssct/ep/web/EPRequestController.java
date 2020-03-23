package ca.tbssct.ep.web;

import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import ca.tbssct.ep.Notification;
import ca.tbssct.ep.Util;

@Controller
public class EPRequestController {

	Logger logger = LoggerFactory.getLogger(this.getClass());

	@GetMapping("/r-r")
	public ModelAndView request(Model model) {
		ModelAndView mav = new ModelAndView();
		logger.info("Vistor has accessed the form.");
		mav.addObject("request", new EPRequest());
		mav.setViewName("requestForm");
		return mav;
	}

	@GetMapping("/")
	public View index(Model model) {
		RedirectView view = new RedirectView("r-r");
		return view;
	}

	@PostMapping("/requestPost")
	public RedirectView handleRequest(@ModelAttribute EPRequest request) throws Exception {
		// avoid long domain names cap it to 30 characters
		String domainNamePrefix = request.getDomainNamePrefix().toLowerCase();
		domainNamePrefix = domainNamePrefix.substring(0, Math.min(domainNamePrefix.length(), 30));
		request.setDomainNamePrefix(domainNamePrefix);
		String requestName = request.getDomainNamePrefix();

		try {
			try (XMLEncoder encoder = new XMLEncoder(
					new BufferedOutputStream(new FileOutputStream(Util.getRequestPath() + requestName + ".xml")));) {
				logger.info("Writing file:" + requestName);
				encoder.writeObject(request);
			} catch (FileNotFoundException fileNotFound) {
				Util.handleError("ERROR: While Creating or Opening the request file: " + requestName + ".xml",
						requestName, logger);
			}

			logger.info("Sending email through notify:" + request.getEmailAddress());
			Map<String, String> personalisation = new HashMap<>();
			personalisation.put("name", request.getYourName());
			personalisation.put("link", Util.GetVerificationURL() + "/verification?id=" + requestName);
			Notification.getNotificationClient().sendEmail(Notification.CONFIRMATION_TEMPLATE_ID,
					request.getEmailAddress(), personalisation, requestName);
			personalisation = new HashMap<>();
			personalisation.put("domainNamePrefix", request.getDomainNamePrefix());
			personalisation.put("department", request.getDepartment());
			personalisation.put("emailAddress", request.getEmailAddress());
			personalisation.put("endDate", request.getEndDate());
			personalisation.put("experimentDescription", request.getExperimentDesc());
			personalisation.put("experimentName", request.getExperimentName());
			personalisation.put("name", request.getYourName());
			personalisation.put("password", request.getPassword());
			personalisation.put("link", Util.GetVerificationURL() + "/verification?id=" + requestName);
			Notification.getNotificationClient().sendEmail(Notification.INFORMATION_TEMPLATE_ID, Util.getAdminEmail(),
					personalisation, requestName);
		} catch (Exception e) {
			Util.handleError(e.getMessage(), request.getDomainNamePrefix(), logger);
		}
		RedirectView view = new RedirectView("es-ee");
		return view;
	}

	@GetMapping("/es-ee")
	public String handleGetRequest() {
		return "mailSent";
	}

}
