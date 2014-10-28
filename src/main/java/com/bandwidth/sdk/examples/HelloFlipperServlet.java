package com.bandwidth.sdk.examples;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.HashMap;

import com.bandwidth.sdk.model.Bridge;
import com.bandwidth.sdk.model.Call;
import com.bandwidth.sdk.model.DtmfEvent;
import com.bandwidth.sdk.model.Event;
import com.bandwidth.sdk.model.EventType;
import com.bandwidth.sdk.model.EventBase;
import com.bandwidth.sdk.model.GatherEvent;
import com.bandwidth.sdk.model.AnswerEvent;
import com.bandwidth.sdk.model.HangupEvent;
import com.bandwidth.sdk.model.IncomingCallEvent;
import com.bandwidth.sdk.model.PlaybackEvent;
import com.bandwidth.sdk.model.RejectEvent;
import com.bandwidth.sdk.model.RecordingEvent;
import com.bandwidth.sdk.model.SpeakEvent;
import com.bandwidth.sdk.model.SmsEvent;
import com.bandwidth.sdk.model.TimeoutEvent;
import com.bandwidth.sdk.model.Visitor;

/**
 * This app is an event server for the Bandwidth App Platform SDK It processes
 * events within a jetty web app using the SDK
 * 
 * 
 * 
 */
public class HelloFlipperServlet extends HttpServlet {
	public static final Logger logger = Logger
			.getLogger(com.bandwidth.sdk.examples.HelloFlipperServlet.class.getName());

	// Setup logging programmatically to make initial deployment easier. Replace
	// this with logging config
	static {
		ConsoleHandler consoleHandler = new ConsoleHandler();

		// create a single line log output
		consoleHandler.setFormatter(new SimpleFormatter() {

			@Override
			public String format(LogRecord record) {
				StringBuilder sb = new StringBuilder();

				sb.append(record.getLevel().getLocalizedName()).append(": ")
						.append(formatMessage(record)).append("\n");

				return sb.toString();
			}
		});
		// change the log level here
		consoleHandler.setLevel(Level.FINEST);
		// and here
		logger.setLevel(Level.FINEST);

		logger.addHandler(consoleHandler);
	}
	
	private static String OUTGOING_NUMBER = "BANDWIDTH_OUTGOING_NUMBER";

	private String callbackUrl;

	private EventHandler eventHandler;

	// The number of worker threads
	private static int NUMWORKERS = 1;
	private ArrayList<Thread> workerThreads;
	
	// edit this to your phone number. 
	// //This would be replaced by a db to lookup give a from number
	// Alternatively you can set this in the environment variable
	// BANDWIDTH_APPPLATFORM_OUTGOING_NUMBER
	private String outgoingNumber; 
	

	// The concurrent blocking queue allows worker threads to respond to the
	// requests
	// without blocking or delaying new events
	private final BlockingQueue<Event> queue = new LinkedBlockingQueue<Event>();
	

	/**
	 * Initialize the servlet.
	 * 
	 * This method starts the worker threads
	 */
	@Override
	public void init() {
		logger.finer("init(ENTRY)");
		
		workerThreads = new ArrayList<Thread>();

		// start the worker threads
		for (int i = 0; i < NUMWORKERS; i++) {
			
			Thread thread = new Thread(new EventWorker(queue));
			thread.start();
			
			workerThreads.add(thread);
		}

		eventHandler = new EventHandler();
				
		Map<String, String> env = System.getenv();
		
		String outgoingNumber = env.get(OUTGOING_NUMBER);
		
		if (outgoingNumber != null && outgoingNumber.length() > 0) {
			this.outgoingNumber = outgoingNumber;
		}
		
		logger.fine("outgoingNumber:" + this.outgoingNumber);
		
		

		logger.finer("init(EXIT)");
	}
	
	/**
	 * Clean up the threads
	 */
	public void destroy() {
		
		// clean up nicely
		for (Thread thread : workerThreads) {
			thread.interrupted();
		}
	}

	/**
	 * This method is the main event handler when events are posted. It creates
	 * an event object from the body of the post and puts it on the queue for
	 * the workers to process
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
		logger.finer("doPost(ENTRY)");

		 //displayHeaders(req);
		 //displayParameters(req);

		try {
			String body = getBody(req);
			logger.finest(body);
			Event event = (Event) EventBase.createEventFromString(body);

			String callLeg = req.getParameter("callLeg");
			String requestUrl = req.getRequestURL().toString();
			String requestUri = req.getRequestURI();
			String contextPath = req.getContextPath();
			callbackUrl = requestUrl + "?callLeg=outgoing"; // used for outgoing
			String baseUrl = requestUrl.substring(0, requestUrl.length()
					- requestUri.length()) + contextPath;

			logger.finer("requestUrl:" + requestUrl);
			logger.finer("requestUri:" + requestUri);
			logger.finer("contextPath:" + contextPath);
			logger.finer("callbackUrl:" + callbackUrl);
			logger.finer("baseUrl:" + baseUrl);

			String fromNumber = req.getParameter("fromNumber");
			event.setProperty("fromNumber", fromNumber);
			event.setProperty("callLeg", callLeg);
			event.setProperty("baseUrl", baseUrl);

			logger.fine("adding event to queue");
			queue.add(event);

			resp.setStatus(HttpServletResponse.SC_OK);

		} catch (Exception e) {
			logger.severe(e.toString());
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			e.printStackTrace();

		}

		logger.finer("doPost(EXIT)");
	}

	/**
     * 
     */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		logger.finer("doGet(ENTRY)");
		
		String callLeg = req.getParameter("callLeg");
		String requestUrl = req.getRequestURL().toString();
		String requestUri = req.getRequestURI();
		String contextPath = req.getContextPath();

		logger.finer("requestUrl:" + requestUrl);
		logger.finer("requestUri:" + requestUri);
		logger.finer("contextPath:" + contextPath);
		
		String baseUrl = requestUrl.substring(0, requestUrl.length()
				- requestUri.length());


		callbackUrl = requestUrl + "?callLeg=outgoing"; // used for outgoing
														// calls
		logger.finer("callbackUrl:" + callbackUrl);
		
		logger.finer("baseUrl:" + baseUrl);


		logger.finer("doGet(EXIT)");
	}

	/**
	 * Displays the request headers
	 * 
	 * @param req
	 */
	protected void displayHeaders(HttpServletRequest req) {
		logger.finest("displayHeaders(ENTRY)");

		Enumeration names = req.getHeaderNames();

		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			StringBuffer buf = new StringBuffer(name + ":");

			Enumeration headers = req.getHeaders(name);

			while (headers.hasMoreElements()) {
				String header = (String) headers.nextElement();

				buf.append(header + ",");
			}
			logger.finest(buf.toString());
		}

		logger.finest("displayHeaders(EXIT)");
	}

	/**
	 * Displays the parameters from the request
	 * 
	 * @param req
	 */
	protected void displayParameters(HttpServletRequest req) {
		logger.finest("displayParameters(ENTRY)");

		Enumeration keys = req.getParameterNames();

		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();

			// To retrieve a single value
			String value = req.getParameter(key);
			logger.finer(key + ":" + value);

			// If the same key has multiple values (check boxes)
			String[] valueArray = req.getParameterValues(key);

			for (int i = 0; i > valueArray.length; i++) {
				logger.finest("VALUE ARRAY" + valueArray[i]);
			}

		}

		logger.finest("displayParameters(EXIT)");
	}

	/**
	 * Extracts the JSON body from the request
	 * 
	 * @param req
	 * @return
	 */
	protected String getBody(HttpServletRequest req) {
		logger.finest("getBody(ENTRY)");

		StringBuilder sb = new StringBuilder();
		try {
			InputStream in = req.getInputStream();

			InputStreamReader is = new InputStreamReader(in);

			BufferedReader br = new BufferedReader(is);
			String read = br.readLine();

			while (read != null) {

				// System.out.println(read);
				sb.append(read);
				read = br.readLine();
			}
		} catch (Exception e) {
			logger.severe(e.toString());
			e.printStackTrace();
		}

		logger.finest("getBody(EXIT)");
		return sb.toString();
	}

	/**
	 * Worker thread
	 * 
	 * @author smitchell
	 * 
	 */
	public class EventWorker implements Runnable {
		protected BlockingQueue<Event> queue = null;

		public EventWorker(BlockingQueue<Event> queue) {
			this.queue = queue;
		}

		/**
		 * Processes each event
		 */
		public void run() {
			logger.finer("run(ENTRY)");
			while (true) {
				try {
					logger.fine("taking event from queue");
					Event event = (Event) queue.take();

					EventType eventType = event.getEventType();
					logger.finer("got event from queue, eventType:"
							+ eventType.toString());

					event.execute(eventHandler);

				} // try
				catch (InterruptedException e) {
					logger.severe(e.getMessage());
					e.printStackTrace();
					Thread.currentThread().interrupt();
					break;
				}

			} // while

		} // run

	} // end EventConsumer

	/**
	 * Events Dispatched here
	 * 
	 * @author smitchell
	 * 
	 */
	public class EventHandler implements Visitor {

		/**
		 * Handles the incoming call event
		 * 
		 * This is necessary for when auto-answer is off. Otherwise process the
		 * new call in the answer event
		 * 
		 */
		public void processEvent(IncomingCallEvent event) {
			logger.finer("processIncomingCallEvent(ENTRY)");

			// Processes event when autoanswer is off

			logger.finer("processIncomingCallEvent(EXIT)");
		}

		/**
		 * Handles answer event. Distinguishes between incoming callLeg and
		 * outgoing callLeg
		 * 
		 */
		public void processEvent(AnswerEvent event) {
			logger.finer("processAnswerEvent(ENTRY)");

			String callLeg = event.getProperty("callLeg");

			logger.finer("callLeg:" + callLeg);

			if ("incoming".equalsIgnoreCase(callLeg)) {

				String callId = event.getProperty("callId");

				try {
					logger.finer("creating call");
					Call call = Call.get(callId);

					logger.finer("speaking sentence");
					call.speakSentence("Hello Flipper", "hello-flipper");

				} catch (Exception e) {
					logger.severe(e.toString());
					e.printStackTrace();
				}
			} else if ("outgoing".equalsIgnoreCase(callLeg)) {
				logger.finer("outgoing call...");
				String outgoingCallId = event.getProperty("callId");

				String incomingCallId = event.getProperty("tag");

				logger.fine("outoingCallId:" + outgoingCallId);
				logger.fine("incomingCallId:" + incomingCallId);

				try {
					Call call1 = Call.get(incomingCallId);

					Call call2 = Call.get(outgoingCallId);
					call2.speakSentence(
							"You have a dolphin on the line. Watch out, he's hungry!",
							"whisper-to-the-fish");
					Bridge.create(call1, call2);

				} catch (Exception e) {
					logger.severe(e.toString());
					e.printStackTrace();
				}

				logger.finer("processAnswerEvent(EXIT)");
			}

		}

		/**
        * 
       */

		public void processEvent(GatherEvent event) {
			logger.finer("processGatherEvent(ENTRY)");

			String digits = event.getProperty("digits");
			String callId = event.getProperty("callId");
			String reason = event.getProperty("reason");

			logger.finer("digits:" + digits);
			logger.finer("callId:" + callId);
			try {

				if ("inter-digit-timeout".equalsIgnoreCase(reason)) {
					Call call = Call.get(callId);

					call.createGather("I'm sorry, I didn't get your response. Please enter 1 to speak to the fish. Enter 2 to say goodbye to flipper");
				} else {
					if ("1".equals(digits)) {
						Call call1 = Call.get(callId);

						logger.fine("Got a 1 - speaking sentence....");
						call1.speakSentence("We're connecting you to your fish");

						logger.finer("making outgoing call:" + callbackUrl);
						
						Call call2 = Call.create(outgoingNumber, call1.getTo(),
								callbackUrl + "?fromNumber=" + callId, callId);

						// will whisper to the outgoing call when they answer
					} else if ("2".equals(digits)) {
						Call call = Call.get(callId);
						call.speakSentence("Bye bye flipper!");

						call.hangUp();
					}
				}
			} catch (Exception e) {
				logger.severe(e.toString());
				e.printStackTrace();
			}

			logger.finer("processGatherEvent(EXIT)");
		}

		/**
		 * Handles speak event
		 */
		public void processEvent(SpeakEvent event) {
			logger.finer("processSpeakEvent(ENTRY)");

			String tag = event.getProperty("tag");
			String status = event.getProperty("status");
			String callId1 = event.getProperty("callId");
			logger.finer("callId1:" + callId1);

			logger.finer("tag" + tag);

			
			if ("whisper-to-the-fish".equalsIgnoreCase(tag)
					&& "done".equalsIgnoreCase(status)) {
				try {
					logger.finer("processing whisper...");

					Call call1 = Call.get(callId1);

					String callId2 = event.getProperty("fromNumber");
					logger.finer("callId2:" + callId2);

					Call call2 = Call.get(callId2);

				} catch (Exception e) {
					logger.severe(e.getMessage());
					e.printStackTrace();
				}
			}
			else if ("hello-flipper".equalsIgnoreCase(tag) ) {
				try {
					
					logger.finer("play recording");
					Call call = Call.get(callId1);
				
					call.playRecording(event.getProperty("baseUrl") + "/dolphin.mp3");
				}
				catch (Exception e) {
					logger.severe(e.getMessage());
					e.printStackTrace();
				}

			}


			logger.finer("processSpeakEvent(EXIT)");
		}

		public void processEvent(HangupEvent event) {
			logger.finer("processHangupEvent(ENTRY)");

			logger.finer("processHangupEvent(EXIT)");
		}

		public void processEvent(RejectEvent event) {
			logger.finer("processRejectEvent(ENTRY)");

			logger.finer("processRejectEvent(EXIT)");
		}

		/**
		 * Handles playback event
		 */
		public void processEvent(PlaybackEvent event) {
			logger.finer("processPlaybackEvent(ENTRY):" + event);
			
			
			String callId = event.getProperty("callId");
			String status = event.getProperty("status");
			logger.finer("callId1:" + callId);

			if ("done".equalsIgnoreCase(status)) {
				try {
					Call call = Call.get(callId);
					// logger.finer("create gather");
					call.createGather("Press 1 to connect with your fish. Press 2 to let it go");
				}
				catch(Exception e) {
					logger.severe(e.getMessage());
					e.printStackTrace();
				}
			}
			
			logger.finer("processPlaybackEvent(EXIT)");
		}

		/**
		 * Handles both stop and start of the recording
		 */
		public void processEvent(RecordingEvent event) {
			logger.finer("processRecordingEvent(ENTRY)");

			logger.finer("processRecordingEvent(EXIT)");
		}

		/**
		 * Handles dtmf events
		 */
		public void processEvent(DtmfEvent event) {
			logger.finer("processDtmfEvent(ENTRY)");

			logger.finer("processDtmfEvent(EXIT)");
		}

		/**
		 * Handles default events
		 */
		public void processEvent(Event event) {
			logger.finer("processEvent(ENTRY)");

			logger.finer("processEvent(EXIT)");

		}

		/**
		 * Handles sms events
		 */
		public void processEvent(SmsEvent event) {
			logger.finer("processSmsEvent(ENTRY)");

			logger.finer("processSmsEvent(EXIT)");

		}

		public void processEvent(TimeoutEvent event) {
			logger.finer("processTimeoutEvent(ENTRY)");

			logger.finer("processTimeoutEvent(EXIT)");

		}

	}

}
