package ca.canada.treasury.testbed.web.controller;

import static ca.canada.treasury.testbed.web.service.impl.SolrUtil.COLLECTION_COVID19;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.SortClause;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.SolrInputDocument;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ProblemController {

	public static final String SOLR_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	public static final SimpleDateFormat format = new SimpleDateFormat(SOLR_DATE_FORMAT);
	private static final Logger LOG = LoggerFactory.getLogger(ProblemController.class);
	public static final String COLLECTION_PROBLEM = "problem";

	@Autowired
	private SolrClient solr;

	@PostMapping(value = "/addProblem")
	public @ResponseBody String addProblem(HttpServletRequest request) {
		try {
			String urlString = "http://localhost:8983/solr/problem";
			SolrClient Solr = new HttpSolrClient.Builder(urlString).build();
			SolrInputDocument toIndexDoc = new SolrInputDocument();
			toIndexDoc.addField("id", System.currentTimeMillis());
			toIndexDoc.addField("url", request.getParameter("url"));
			toIndexDoc.addField("date", format.format(new Date()));
			toIndexDoc.addField("problem", request.getParameter("problem"));
			toIndexDoc.addField("problemDetails", request.getParameter("problemDetails"));
			toIndexDoc.addField("department", "Health Canada");
			toIndexDoc.addField("language", request.getParameter("language"));
			Solr.add(toIndexDoc);
			Solr.commit();
			return "Problem added.";
		} catch (Exception e) {
			return "Error:" + e.getMessage();
		}
	}

	public String getData() throws Exception {
		StringBuilder builder = new StringBuilder();
		LOG.debug("Probelm request...");

		SolrQuery q = new SolrQuery();
		q.setRequestHandler("/" + COLLECTION_PROBLEM + "/select");

		q.set("wt", "xml");
		q.set("omitHeader", "true");
		q.setQuery("*:*");

		String fileLang = "";
		String lang = "en";
		if (StringUtils.isNotBlank(lang)) {
			q.set("fq", "language:" + lang);
			fileLang = "_" + lang;
		} else {
			q.set("qf", "id");
		}

		q.setFields("url", "problem", "problemDetails", "date", "language", "department");

		q.setStart(0);
		q.setRows(10000);

		q.set("defType", "edismax");

		LOG.debug("Solr {} query: {}", COLLECTION_PROBLEM, q);

		QueryRequest solrReq = new QueryRequest(q);
		NoOpResponseParser rawXMLResponseParser = new NoOpResponseParser();
		rawXMLResponseParser.setWriterType("xml");
		solrReq.setResponseParser(rawXMLResponseParser);
		String xml = (String) (solr.request(solrReq).get("response"));
		Document document = DocumentHelper.parseText(xml);
		List<Element> results = document.selectNodes("//doc");
		for (Element elem : results) {
			builder.append("<tr><td>" + elem.selectSingleNode("//str[@name='department']").getText() + "</td>");
			builder.append("<td>" + elem.selectSingleNode("//str[@name='url']").getText() + "</td>");
			builder.append("<td>" + elem.selectSingleNode("//str[@name='problem']").getText() + "</td>");
			builder.append("<td>" + elem.selectSingleNode("//str[@name='problemDetails']").getText() + "</td>");
			builder.append("<td>" + elem.selectSingleNode("//date[@name='date']").getText() + "</td>");
			builder.append("<td>" + "</td>");
			builder.append("<td>" + "</td>");
			builder.append("</tr>");
		}
		return builder.toString();
	}

	@GetMapping(value = "/dashboard")
	public ModelAndView dashboard() throws Exception {
		ModelAndView mav = new ModelAndView();
		mav.addObject("data", this.getData());
		mav.setViewName("problemDashboard");
		return mav;
	}

	@GetMapping(value = "/testForm")
	public String testForm() {
		return "testForm";
	}
}