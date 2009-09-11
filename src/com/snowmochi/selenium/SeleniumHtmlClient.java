/* Copyright 2009 Daiji Takamori
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.snowmochi.selenium;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;
import com.thoughtworks.selenium.*;

public class SeleniumHtmlClient {
    static class BadUsageException extends RuntimeException {
        public BadUsageException(String message) {
            super(message);
        }
    }

	public static void main(String args[]) {
		try {
			SeleniumHtmlClient client = new SeleniumHtmlClient();
			String testFile = null;
			String testSuite = null;
			String resultsFilename = null;
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("--host")) {
					i++;
					if (i < args.length) {
						client.setHost(args[i]);
					} else {
						throw new BadUsageException("--host must be followed by a hostname");
					}
				} else if (args[i].equals("--port")) {
					i++;
					if (i < args.length) {
						client.setPort(Integer.parseInt(args[i]));
					} else {
						throw new BadUsageException("--port must be followed by a port number");
					}
				} else if (args[i].equals("--browser")) {
					i++;
					if (i < args.length) {
						client.setBrowser(args[i]);
					} else {
						throw new BadUsageException("--browser must be followed by a browser spec");
					}
				} else if (args[i].equals("--out")) {
					i++;
					if (i < args.length) {
						resultsFilename = args[i];
					} else {
						throw new BadUsageException("--out must be followed by a filename");
					}
					/*
				} else if (args[i].equals("--outdir")) {
					i++;
					if (i < args.length) {
						client.setResultsDir(new File(args[i]));
					} else {
						throw new BadUsageException("--outdir must be followed by a path");
					}
					*/
				} else if (args[i].equals("--baseurl")) {
					i++;
					if (i < args.length) {
						client.setBaseUrl(args[i]);
					} else {
						throw new BadUsageException("--baseurl must be followed by a URL");
					}
				} else if (args[i].equals("--test")) {
					i++;
					if (i < args.length) {
						if (testFile == null) {
							testFile = args[i];
						} else {
							throw new BadUsageException("only one test file permitted");
						}
					} else {
						throw new BadUsageException("--test must be followed by a test filepath");
					}
				} else if (args[i].equals("--testsuite")) {
					i++;
					if (i < args.length) {
						testSuite = args[i];
					} else {
						throw new BadUsageException("--testsuite must be followed by a testsuite filepath");
					}
				} else if (args[i].equals("--verbose") || args[i].equals("-v")) {
					client.setVerbose(true);
				} else if (args[i].equals("--help") || args[i].equals("-h")) {
    				printUsage();
					System.exit(0);
				} else {
					throw new BadUsageException("Unknown parameter " + args[i]);
				}
			}
			if (testFile == null && testSuite == null) {
				throw new BadUsageException("No test or testsuite file specified");
			} else if (testFile != null && testSuite != null) {
				throw new BadUsageException("A test and testsuite file cannot both be specified");
			}
			Writer resultsWriter = null;
			if (resultsFilename != null) {
				resultsWriter = new FileWriter(resultsFilename);
			} else /* if (client.resultsDir == null) */ {
				resultsWriter = new OutputStreamWriter(System.out);
			}
			client.setResultsWriter(resultsWriter);
			if (testFile != null) {
				client.runTest(testFile);
			} else {
				client.runSuite(testSuite);
			}
			if (resultsWriter != null) resultsWriter.close();
		} catch (BadUsageException e) {
		    System.err.println("Error: " + e.getMessage());
		    System.err.println();
			printUsage();
		    System.exit(1);
		} catch (Exception e) {
			e.printStackTrace();
		    System.exit(1);
		}
	}
	
	static void printUsage() {
	    System.out.println("Selenium HTML Client");
	    System.out.println("Usage:");
	    System.out.println("\t--host hostname\tSpecify Selenium server (default: localhost)");
	    System.out.println("\t--port portnumber\tSpecify Selenium server port (default: 4444)");
	    System.out.println("\t--browser browserspec\tSpecify Selenium browser (default: *opera)");
	    System.out.println("\t--out outputfilename\tSpecify a file for output");
	    //System.out.println("\t--outdir outputpath");
	    System.out.println("\t--baseurl testurlbase\tSpecify the base URL for any relative URLs");
	    System.out.println("\t--test testfile\tExecute a single test file");
	    System.out.println("\t--testsuite testsuitefile\tExecute a test suite file");
	    System.out.println("\t--verbose, -v\tSet verbose mode on");
	    System.out.println("\t--help, -h\tDisplay this message");
	}

	String host = "localhost";
	int port = 4444;
	String browser = "*opera";
	String baseUrl;
	File resultsDir;
	Writer resultsWriter;
	boolean verbose;
	Document document;
	CommandProcessor commandProcessor;

	public SeleniumHtmlClient() {
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setBrowser(String browser) {
		this.browser = browser;
	}

	public void setResultsDir(File outdir) {
		this.resultsDir = outdir;
	}

	public void setResultsWriter(Writer out) {
		this.resultsWriter = out;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	class TestSuite {
		public File file;
		public String name;
		public Test tests[];
		public boolean result;
	}
	class Test {
		public String label;
		public File file;
		public String name;
		public Command commands[];
		public boolean result;
	}
	class Command {
		public String cmd;
		public String args[];
		public String result;
		public boolean error;
		public boolean failure;
	}

	public boolean runSuite(String filename) throws Exception {
		if (this.verbose) {
			System.out.println("Running test suite " + filename + 
				" against " + this.host + ":" + this.port +
				" with " + this.browser);
		}
		TestSuite suite = new TestSuite();
		suite.file = new File(filename);
		File suiteDirectory = suite.file.getParentFile();
		this.document = parseDocument(filename);
		Element table = (Element) this.document.getElementsByTagName("table").item(0);
		NodeList tableRows = table.getElementsByTagName("tr");
		Element tableNameRow = (Element) tableRows.item(0);
		suite.name = tableNameRow.getTextContent();
		suite.result = true;
		suite.tests = new Test[tableRows.getLength() - 1];
		for (int i = 1; i < tableRows.getLength(); i++) {
			Element tableRow = (Element) tableRows.item(i);
			Element cell = (Element) tableRow.getElementsByTagName("td").item(0);
			Element link = (Element) cell.getElementsByTagName("a").item(0);
			Test test = new Test();
			test.label = link.getTextContent();
			test.file = new File(suiteDirectory, link.getAttribute("href"));

			SeleniumHtmlClient subclient = new SeleniumHtmlClient();
			subclient.setHost(this.host);
			subclient.setPort(this.port);
			subclient.setBrowser(this.browser);
			//subclient.setResultsWriter(this.resultsWriter);
			subclient.setBaseUrl(this.baseUrl);
			subclient.setVerbose(this.verbose);
			subclient.runTest(test);
			suite.result &= test.result;
			suite.tests[i - 1] = test;
		}
		if (this.resultsWriter != null) {
			this.resultsWriter.write("<html><head>");
			this.resultsWriter.write("<title>" + suite.name + "</title>");
			this.resultsWriter.write("<style>");
			this.resultsWriter.write(".ran { background-color: #eeffee; }");
			this.resultsWriter.write(".passed { background-color: #ccffcc; }");
			this.resultsWriter.write(".failed { background-color: #ffcccc; }");
			this.resultsWriter.write(".error { background-color: #ffeeee; }");
			this.resultsWriter.write("</style>");
			this.resultsWriter.write("</head><body>");
			this.resultsWriter.write("<div>\n");
			this.resultsWriter.write("<h1>Selenium Test Suite Results</h1>\n");
			this.resultsWriter.write("<h4>date:</h4> " + new Date() + "\n");
			this.resultsWriter.write("<h4>browser:</h4> " + this.browser + "\n");
			this.resultsWriter.write("<h4>result:</h4> " + (suite.result ? "PASSED" : "FAILED") + "\n");
			this.resultsWriter.write("</div>\n");
			this.resultsWriter.write("<div>\n");
			this.resultsWriter.write("<h2>" + suite.file.getName() + "</h2>\n");
			this.resultsWriter.write("<table border=\"1\">\n");
			this.resultsWriter.write("<tr class=\"" + (suite.result ? "passed" : "failed") + "\"><td colspan=\"2\">" + suite.name + "</td></tr>\n");
			for (Test test : suite.tests) {
				this.resultsWriter.write("<tr class=\"" + (test.result ? "passed" : "failed") + "\"><td>");
				this.resultsWriter.write("<a href=\"#" + test.label + "\">" + test.label + "</td><td>" + test.result + "\n");
				this.resultsWriter.write("</td></tr>\n");
			}
			this.resultsWriter.write("</table>\n");
			this.resultsWriter.write("</div>\n");
			for (Test test : suite.tests) {
				this.resultsWriter.write("<div>\n");
				this.resultsWriter.write("<h3><a name=\"" + test.label + "\">" + test.file.getName() + "</a></h3>\n");
				this.resultsWriter.write("<table border=\"1\">\n");
				this.resultsWriter.write("<tr class=\"" + (test.result ? "passed" : "failed") + "\"><td colspan=\"3\">" + test.name + "</td></tr>\n");
				for (Command command : test.commands) {
					boolean result = command.result.startsWith("OK");
					this.resultsWriter.write("<tr class=\"" + (result ? "passed" : "failed") + "\"><td>");
					this.resultsWriter.write(command.cmd);
					this.resultsWriter.write("</td><td>");
					if (command.args != null) {
						this.resultsWriter.write(Arrays.asList(command.args).toString());
					}
					this.resultsWriter.write("</td><td>");
					this.resultsWriter.write(command.result);
					this.resultsWriter.write("</td></tr>\n");
					if (command.failure) break;
				}
				this.resultsWriter.write("</table>\n");
				this.resultsWriter.write("</div>\n");
			}
			this.resultsWriter.write("</body></html>");
		}
		return suite.result;
	}

	public boolean runTest(String filename) throws Exception {
		Test test = new Test();
		test.file = new File(filename);
		runTest(test);
		
		// Print the DOM node
		if (this.resultsWriter != null) {
			this.resultsWriter.write("<html><head>");
			this.resultsWriter.write("<title>" + test.name + "</title>");
			this.resultsWriter.write("<style>");
			this.resultsWriter.write(".ran { background-color: #eeffee; }");
			this.resultsWriter.write(".passed { background-color: #ccffcc; }");
			this.resultsWriter.write(".failed { background-color: #ffcccc; }");
			this.resultsWriter.write(".error { background-color: #ffeeee; }");
			this.resultsWriter.write("</style>");
			this.resultsWriter.write("</head><body>");
			this.resultsWriter.write("<div>\n");
			this.resultsWriter.write("<h3><a name=\"" + test.label + "\">" + test.file.getName() + "</a></h3>\n");
			this.resultsWriter.write("<table border=\"1\">\n");
			this.resultsWriter.write("<tr class=\"" + (test.result ? "passed" : "failed") + "\"><td colspan=\"3\">" + test.name + "</td></tr>\n");
			for (Command command : test.commands) {
				boolean result = command.result.startsWith("OK");
				this.resultsWriter.write("<tr class=\"" + (result ? "passed" : "failed") + "\"><td>");
				this.resultsWriter.write(command.cmd);
				this.resultsWriter.write("</td><td>");
				if (command.args != null) {
					this.resultsWriter.write(Arrays.asList(command.args).toString());
				}
				this.resultsWriter.write("</td><td>");
				this.resultsWriter.write(command.result);
				this.resultsWriter.write("</td></tr>\n");
				if (command.failure) break;
			}
			this.resultsWriter.write("</table>\n");
			this.resultsWriter.write("</div>\n");
			this.resultsWriter.write("</body></html>");
			//outputDocument(this.resultsWriter);
		}
		return test.result;
	}

	public boolean runTest(Test test) throws Exception {
		String filename = test.file.toString();
		if (this.verbose) {
			System.out.println("Running " + filename + 
				" against " + this.host + ":" + this.port +
				" with " + this.browser);
		}
		this.document = parseDocument(filename);

		if (this.baseUrl == null) {
			NodeList links = this.document.getElementsByTagName("link");
			if (links.getLength() != 0) {
				Element link = (Element) links.item(0);
				setBaseUrl(link.getAttribute("href"));
			}
		}
		if (this.verbose) {
			System.out.println("Base URL=" + this.baseUrl);
		}

		Node body = this.document.getElementsByTagName("body").item(0);
		Element resultContainer = document.createElement("div");
		resultContainer.setTextContent("Result: ");
		Element resultElt = document.createElement("span");
		resultElt.setAttribute("id", "result");
		resultElt.setIdAttribute("id", true);
		resultContainer.appendChild(resultElt);
		body.insertBefore(resultContainer, body.getFirstChild());

		Element executionLogContainer = document.createElement("div");
		executionLogContainer.setTextContent("Execution Log:");
		Element executionLog = document.createElement("div");
		executionLog.setAttribute("id", "log");
		executionLog.setIdAttribute("id", true);
		executionLog.setAttribute("style", "white-space: pre;");
		executionLogContainer.appendChild(executionLog);
		body.appendChild(executionLogContainer);

		NodeList tableRows = document.getElementsByTagName("tr");
		Element theadRow = (Element) tableRows.item(0);
		test.name = theadRow.getTextContent();
		appendCellToRow(theadRow, "Result");

		this.commandProcessor = new HtmlCommandProcessor(this.host, this.port, 
			this.browser, this.baseUrl);
		String resultState;
		String resultLog;
		test.result = true;
		try {
			this.commandProcessor.start();
			test.commands = new Command[tableRows.getLength() - 1];
			for (int i = 1; i < tableRows.getLength(); i++) {
				Element stepRow = (Element) tableRows.item(i);
				Command command = executeStep(stepRow);
				appendCellToRow(stepRow, command.result);
				test.commands[i - 1] = command;
				if (command.error) {
					test.result = false;
				}
				if (command.failure) {
					test.result = false;
					break;
				}
			}
			resultState = test.result ? "PASSED" : "FAILED";
			resultLog = (test.result ? "Test Complete" : "Error");
			this.commandProcessor.stop();
		} catch (Exception e) {
			test.result = false;
			resultState = "ERROR";
			resultLog = "Failed to initialize session\n" + e;
			e.printStackTrace();
		}
		document.getElementById("result").setTextContent(resultState);
		Element log = document.getElementById("log");
		log.setTextContent(log.getTextContent() + resultLog + "\n");
		return test.result;
	}

	public Command executeStep(Element stepRow) throws Exception {
		Command command = new Command();
		NodeList stepFields = stepRow.getElementsByTagName("td");
		String cmd = stepFields.item(0).getTextContent().trim();
		command.cmd = cmd;
		ArrayList<String> argList = new ArrayList<String>();
		if (stepFields.getLength() == 1) {
			// skip comments
			command.result = "OK";
			return command;
		}
		for (int i = 1; i < stepFields.getLength(); i++) {
			String content = stepFields.item(i).getTextContent();
content = content.replaceAll(" +", " ");
content = content.replace('\u00A0', ' ');
content = content.trim();
			argList.add(content);
		}
		String args[] = argList.toArray(new String[0]);
		command.args = args;
		if (this.verbose) {
			System.out.println(cmd + " " + Arrays.asList(args));
		}
		try {
			command.result = this.commandProcessor.doCommand(cmd, args);
			command.error = false;
		} catch (Exception e) {
			command.result = e.getMessage();
			command.error = true;
		}
		command.failure = command.error && !cmd.startsWith("verify");
		return command;
	}

	Document parseDocument(String filename) throws Exception {
		FileReader reader = new FileReader(filename);
		String firstLine = new BufferedReader(reader).readLine();
		reader.close();
		Document document = null;
		if (firstLine.startsWith("<?xml")) {
			System.err.println("XML detected; using default XML parser.");
		} else {
			try {
				Class nekoParserClass = Class.forName("org.cyberneko.html.parsers.DOMParser");
				Object parser = nekoParserClass.newInstance();
				Method parse = nekoParserClass.getMethod("parse", new Class[] { String.class });
				Method getDocument = nekoParserClass.getMethod("getDocument", new Class[0]);
				parse.invoke(parser, filename);
				document = (Document) getDocument.invoke(parser);
			} catch (Exception e) {
				System.err.println("NekoHTML HTML parser not found; HTML4 support disabled.");
			}
		}
		if (document == null) {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			try { // http://www.w3.org/blog/systeam/2008/02/08/w3c_s_excessive_dtd_traffic
                factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", 
                               false);
            } 
            catch (ParserConfigurationException e) {
                System.err.println("Warning: Could not disable external DTD loading");
            }
			DocumentBuilder builder = factory.newDocumentBuilder();
			document = builder.parse(filename);
		}
		return document;
	}

	void appendCellToRow(Element tableRow, String content) {
		Element newCell = document.createElement("td");
		newCell.setTextContent(content);
		tableRow.appendChild(newCell);
	}

	void outputDocument(Writer out) throws Exception {
		// Set up the output transformer
		TransformerFactory transfac = TransformerFactory.newInstance();
		Transformer trans = transfac.newTransformer();
//		trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		trans.setOutputProperty(OutputKeys.INDENT, "yes");
		trans.setOutputProperty(OutputKeys.METHOD, "html");

		// Print the DOM node
		StreamResult result = new StreamResult(out);
		DOMSource source = new DOMSource(this.document);
		trans.transform(source, result);
	}
	
	class HtmlCommandProcessor extends HttpCommandProcessor {
		final static String INDEX_SPECIFIER = "index=";
		final static String ID_SPECIFIER = "id=";
		final static String LABEL_SPECIFIER = "label=";
		final static String VALUE_SPECIFIER = "value=";

		boolean expectError;

		public HtmlCommandProcessor(String host, int port, String browser, String baseUrl) {
			super(host, port, browser, baseUrl);
		}

		public String doCommand(String cmd, String args[]) {
			if (cmd.equals("store")) {
				cmd += "Expression";
			} else if (cmd.equals("assertSelected") || cmd.equals("verifySelected")) {
				if (args[1].startsWith(INDEX_SPECIFIER)) {
					cmd += "Index";
					args[1] = args[1].substring(INDEX_SPECIFIER.length());
				} else if (args[1].startsWith(ID_SPECIFIER)) {
					cmd += "Id";
					args[1] = args[1].substring(ID_SPECIFIER.length());
				} else if (args[1].startsWith(LABEL_SPECIFIER)) {
					cmd += "Label";
					args[1] = args[1].substring(LABEL_SPECIFIER.length());
				} else if (args[1].startsWith(VALUE_SPECIFIER)) {
					cmd += "Value";
					args[1] = args[1].substring(VALUE_SPECIFIER.length());
				} else {
					cmd += "Label";
				}
			} else if (cmd.endsWith("ErrorOnNext") || cmd.endsWith("FailureOnNext")) {
				expectError = true;
				return "OK";
			} else if (cmd.equals("echo")) {
				return "OK," + args[0];
			} else if (cmd.equals("pause")) {
				try {
					Thread.sleep(Integer.parseInt(args[0]));
					return "OK";
				} catch (InterruptedException e) {
					return "ERROR: pause interrupted";
				}
			}
			try {
				String result = super.doCommand(cmd, args);
				if (expectError) {
					throw new SeleniumException("ERROR: Error expected");
				} else {
					return result;
				}
			} catch (SeleniumException e) {
				if (expectError) {
					expectError = false;
					return "OK";
				} else {
					throw e;
				}
			}
			
		}
	}
}
