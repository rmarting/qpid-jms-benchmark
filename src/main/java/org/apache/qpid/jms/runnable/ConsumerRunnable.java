/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.qpid.jms.runnable;

import java.io.FileWriter;
import java.io.PrintWriter;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.activemq.artemis.jms.client.ActiveMQConnection;
import org.apache.qpid.jms.JmsConnection;

public class ConsumerRunnable implements Runnable {

	private String fileName = null;
	private String name = "consumer-";
	private int count = 0;
	private String user = null;
	private String password = null;
	private String destinationType = null;

	public ConsumerRunnable(String fileName, int thread, int count, String user, String password,
			String destinationType) {
		this.fileName = fileName;
		this.name += thread;
		this.count = count;
		this.user = user;
		this.password = password;
		this.destinationType = destinationType;
	}

	public void run() {
		try {
			// The configuration for the Qpid InitialContextFactory has been supplied in
			// a jndi.properties file in the classpath, which results in it being picked
			// up automatically by the InitialContext constructor.
			Context context = new InitialContext();

			ConnectionFactory factory = (ConnectionFactory) context.lookup("connectionFactory");
			try (Connection connection = factory.createConnection(user, password)) {
				// connection.setExceptionListener(new MyExceptionListener());
				connection.start();

				try (Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
					if (connection instanceof JmsConnection) {
						System.out.println(this.name + " - JMS - Consumer connected to Server: " + ((JmsConnection) connection).getConfiguredURI());
					} else if (connection instanceof ActiveMQConnection) {
						String host = ((ActiveMQConnection) connection).getSessionFactory().getConnectorConfiguration()
								.getParams().get("host").toString();
						String port = ((ActiveMQConnection) connection).getSessionFactory().getConnectorConfiguration()
								.getParams().get("port").toString();
						System.out.println(this.name + " - ActiveMQConnection - Consumer connected to Server: " + host + ":" + port);
					}

					Destination destination = null;
					if ("queue".equals(destinationType)) {
						destination = (Destination) context.lookup("sampleQueue");
					} else if ("topic".equals(destinationType)) {
						destination = (Destination) context.lookup("sampleTopic");
					}

					try (MessageConsumer messageConsumer = session.createConsumer(destination)) {
						long start = System.currentTimeMillis();

						int actualCount = 0;
						for (int i = 1; i <= count; i++, actualCount++) {
							Message message = messageConsumer.receive();
							// Start to count with the first message
							if (null != message && i == 1) {
								start = System.currentTimeMillis();
							}
						}
						long finish = System.currentTimeMillis();
						long taken = finish - start;
						System.out.print(this.name + " - Received " + actualCount + " messages in " + taken + " ms. ");
						System.out.println("Ratio (msg/sg): " + (actualCount / (taken / 1000D)));

						writeStatsToCSV(this.fileName, this.name, actualCount, taken, (actualCount / (taken / 1000D)));
					}
				}
			}
		} catch (Exception exp) {
			System.err.println("Consumer Thread " + this.name + ". Caught exception, exiting: " + exp.getMessage());
			exp.printStackTrace(System.err);
		}
	}

	private static void writeStatsToCSV(String fileName, String name, int count, long time, double ratio)
			throws Exception {
		try (PrintWriter pw = new PrintWriter(new FileWriter(fileName, true))) {
			StringBuilder sb = new StringBuilder();
			sb.append(name);
			sb.append(",");
			sb.append(count);
			sb.append(",");
			sb.append(time);
			sb.append(",");
			sb.append(ratio);
			sb.append('\n');
			pw.write(sb.toString());
		}
	}

	private static class MyExceptionListener implements ExceptionListener {
		@Override
		public void onException(JMSException exception) {
			System.out.println("Connection ExceptionListener fired, exiting.");
			exception.printStackTrace(System.out);
			System.exit(1);
		}
	}
}
