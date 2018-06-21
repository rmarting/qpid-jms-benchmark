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
package org.apache.qpid.jms;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Properties;

import org.apache.qpid.jms.runnable.ConsumerRunnable;

public class Consumer {

	public static void main(String[] args) throws Exception {
		String testPropertiesPath = "test.properties";
		Properties testProperties = new Properties();
		testProperties.load(new FileInputStream(testPropertiesPath));

		String fileName = testProperties.getProperty("fileNameConsumer");
		String username = testProperties.getProperty("username");
		String password = testProperties.getProperty("password");
		int threads = Integer.parseInt(testProperties.getProperty("threads"));
		int iterations = Integer.parseInt(testProperties.getProperty("iterations"));
		String destination = testProperties.getProperty("destination");

		try (PrintWriter pw = new PrintWriter(new FileWriter(fileName, true))) {
			pw.write("thread,messages,time(ms),ratio\n");
		}

		try {
			for (int i = 0; i < threads; i++) {
				Thread thread = new Thread(
						new ConsumerRunnable(fileName, i, iterations, username, password, destination));
				thread.start();
			}
		} catch (Exception exp) {
			System.out.println("Main Exception, exiting: " + exp.getMessage());
			exp.printStackTrace(System.err);
		}
	}

}
