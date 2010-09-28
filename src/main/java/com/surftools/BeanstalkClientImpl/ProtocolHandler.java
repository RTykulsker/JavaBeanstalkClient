package com.surftools.BeanstalkClientImpl;

/*

Copyright 2009-2010 Robert Tykulsker 

This file is part of JavaBeanstalkCLient.

JavaBeanstalkCLient is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version, or alternatively, the BSD license supplied
with this project in the file "BSD-LICENSE".

JavaBeanstalkCLient is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with JavaBeanstalkCLient.  If not, see <http://www.gnu.org/licenses/>.

*/

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.surftools.BeanstalkClient.BeanstalkException;

public class ProtocolHandler {
	
	private Socket socket;
	
	ProtocolHandler(String host, int port) {		
		try {
			socket = new Socket(host, port);
		} catch (Exception e) {
			throw new BeanstalkException(e.getMessage());
		}
	}
	

	
	Response processRequest(Request request) {
		validateRequest(request);
		
		Response response = null;
		OutputStream os = null;
		InputStream is = null;
		PrintWriter out = null;

		try {			
			os = socket.getOutputStream();
			out = new PrintWriter(os);

			out.print(request.getCommand() + "\r\n");
				if (request.getData() != null) {
				out.flush();
				os.write(request.getData());
				out.print("\r\n");
			}
			out.flush();
			os.flush();
							
			is = socket.getInputStream();
			String line = new String(readInputStream(is));
	
			String[] tokens = line.split(" ");
			if (tokens == null || tokens.length == 0) {
				throw new BeanstalkException("no response");
			}
			
			response = new Response();
			response.setResponseLine(line);
			String status = tokens[0];
			response.setStatus(status);
			if (tokens.length > 1) {
				response.setReponse(tokens[1]);
			}
			setState(request, response, status);
			
			switch (request.getExpectedResponse()) {
			case Map:
				if (response.isMatchError()) {
					break;
				}
				response.setData(parseForMap(is));
				break;
			case List:
				response.setData(parseForList(is));
				break;
			case ByteArray:
				if (response.isMatchError()) {
					break;
				}
				byte[] data = readInputStream(is);
				response.setData(data);
				break;
			default:
				break;
			}
			
		} catch (Exception e) {
			throw new BeanstalkException(e.getMessage());
		}
		return response;
	}
	
	
	private byte[] readInputStream(InputStream is) {
		if (is == null) {
			return null;
		}
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		boolean lastByteWasReturnByte = false;
		try {
			while (true) {
				byte b = (byte) is.read();
				
				/**
				 * prevent OutOfMemory exceptions, per leopoldkot			
				 */
				if  (b == -1) { 
					throw new BeanstalkException("The end of InputStream is reached");
				}
			
				baos.write(b);
				if (b == '\n' && lastByteWasReturnByte) {
					break;
				}
				lastByteWasReturnByte = (b == '\r');
			}
		} catch(Exception e) {
			throw new BeanstalkException(e.getMessage());
		}
		
		byte[] data = new byte[baos.toByteArray().length - 2];
		System.arraycopy(baos.toByteArray(), 0, data, 0, data.length);
		return data;
	}
	
	
	private void validateRequest(Request request) {
		if (request == null) {
			throw new BeanstalkException("null request");
		}
		
		String command = request.getCommand();
		if (command == null || command.length() == 0) {
			throw new BeanstalkException("null or empty command");
		}
		
		String[] validStates = request.getValidStates();
		if (validStates == null || validStates.length == 0) {
			throw new BeanstalkException("null or empty validStates");
		}
	}
	
	
	private void setState(Request request, Response response, String status) {
		for (String s : request.getValidStates()) {
			if (status.equals(s)) {
				response.setMatchOk(true);
				break;
			}
		}
			
		if (!response.isMatchOk() && request.getErrorStates() != null) {
			for (String s : request.getErrorStates()) {
				if (status.equals(s)) {
					response.setMatchError(true);
					break;
				}
			}
		}
	
		if (!response.isMatchOk() && !response.isMatchError()) {
			throw new BeanstalkException(status);
		}
	}
	
	
	private Map<String,String> parseForMap(InputStream is) {
		Map<String, String> map = new LinkedHashMap<String, String>();
		String line = null;
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(is));
			while ((line = in.readLine()) != null) {
				if (line.length() == 0) {
					break;
				}
				String[] values = line.split(":");
				if (values.length != 2) {
					continue;
				}
				map.put(values[0], values[1]);
			}
		} catch (Exception e) {
			throw new BeanstalkException(e.getMessage());
		}
		return map;
	}	
	
	
	private List<String> parseForList(InputStream is) {
		List<String> list = new ArrayList<String>();
		String line = null;
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(is));
			while ((line = in.readLine()) != null) {
				if (line.length() == 0) {
					break;
				}
				if (line.equals("---")) {
					continue;
				}
				list.add(line.substring(2));
			}
		} catch (Exception e) {
			throw new BeanstalkException(e.getMessage());
		}
		return list;
	}

}
