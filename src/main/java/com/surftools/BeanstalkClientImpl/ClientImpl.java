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

import java.util.List;
import java.util.Map;

import com.surftools.BeanstalkClient.BeanstalkException;
import com.surftools.BeanstalkClient.Client;
import com.surftools.BeanstalkClient.Job;

public class ClientImpl implements Client {
	
	private static final String VERSION = "1.2.2";
	private static final long MAX_PRIORITY = 4294967296L;
	
	private String host;
	private int port;
	
	private ThreadLocal<ProtocolHandler> protocolHandler = new ThreadLocal<ProtocolHandler> () {
		@Override 
		protected ProtocolHandler initialValue () {
			return new ProtocolHandler (host,port);
		}
	};
	
	public ClientImpl() {
		this(DEFAULT_HOST, DEFAULT_PORT);
	}
	
	public ClientImpl(String host, int port) {
		this.host = host;
		this.port = port;
	}
	
	public ClientImpl(boolean useBlockIO) {
		this(DEFAULT_HOST, DEFAULT_PORT);
		protocolHandler.get().setUseBlockIO(useBlockIO);
	}
	
	public ClientImpl(String host, int port, boolean useBlockIO) {
		this.host = host;
		this.port = port;
		protocolHandler.get().setUseBlockIO(useBlockIO);
	}
	
	// ****************************************************************
	// Producer methods
	// ****************************************************************
	
	public long put(long priority, int delaySeconds, int timeToRun, byte[] data) {
		if (data == null) {
			throw new BeanstalkException("null data");			
		}
		if (priority > MAX_PRIORITY) {
			throw new BeanstalkException("invalid priority");
		}
		long jobId = -1;
		Request request = new Request(
			"put " + priority + " " + delaySeconds + " " + timeToRun + " " + data.length, 
			new String[] {"INSERTED","BURIED"},
			new String[] {"JOB_TOO_BIG"},
			data,
			ExpectedResponse.None);
        Response response = protocolHandler.get().processRequest(request);
        if (response != null && response.getStatus().equals("JOB_TOO_BIG")) {
        	BeanstalkException be = new BeanstalkException(response.getStatus());
        	throw be;
        }        	
		if (response != null && response.isMatchOk()) {
			jobId = Long.parseLong(response.getReponse());
		}
		return jobId;
	}
	
	public void useTube(String tubeName) {
		Request request = new Request(
			"use " + tubeName, 
			"USING", 
			null,
			null,
			ExpectedResponse.None);
		protocolHandler.get().processRequest(request);
	}
	
	// ****************************************************************
	// Consumer methods
	//	job-related
	// ****************************************************************	
	public Job reserve(Integer timeoutSeconds) {
		Job job = null;
		String command = (timeoutSeconds == null)
			? "reserve"
			: "reserve-with-timeout " + timeoutSeconds.toString();
		Request request = new Request(
				command, 
				new String[] {"RESERVED"},
				new String[] {"DEADLINE_SOON", "TIMED_OUT", },
				null,
				ExpectedResponse.ByteArray,
				2);
	        Response response = protocolHandler.get().processRequest(request);
	        if (response != null && response.getStatus().equals("DEADLINE_SOON")) {
	        	BeanstalkException be = new BeanstalkException(response.getStatus());
	        	throw be;
	        }
			if (response != null && response.isMatchOk()) {
				long jobId = Long.parseLong(response.getReponse());
				job = new JobImpl(jobId);
				job.setData((byte[]) response.getData());
			}
		return job;
	}

	public boolean delete(long jobId) {
		Request request = new Request(
			"delete " + jobId, 
			"DELETED", 
			"NOT_FOUND",
			null,
			ExpectedResponse.None);
        Response response = protocolHandler.get().processRequest(request);
		return response != null && response.isMatchOk();
	}
	
	public boolean release(long jobId, long priority, int delaySeconds) {
		Request request = new Request(
			"release " + jobId + " " + priority + " " + delaySeconds, 
			new String[] {"RELEASED"}, 
			new String[] {"NOT_FOUND", "BURIED"},
			null,
			ExpectedResponse.None);
		Response response = protocolHandler.get().processRequest(request);
		return response != null && response.isMatchOk();
	}

	public boolean bury(long jobId, long priority ) {
		Request request = new Request(
			"bury " + jobId + " " + priority, 
			"BURIED",
			"NOT_FOUND",
			null,
			ExpectedResponse.None);
		Response response = protocolHandler.get().processRequest(request);
		return response != null && response.isMatchOk();
	}
	
	public boolean touch(long jobId) {
		Request request = new Request(
			"touch " + jobId, 
			"TOUCHED",
			"NOT_FOUND",
			null,
			ExpectedResponse.None);
		Response response = protocolHandler.get().processRequest(request);
		return response != null && response.isMatchOk();
	}
	
	// ****************************************************************
	// Consumer methods
	//	tube-related
	// ****************************************************************
	public int watch(String tubeName) {
		Request request = new Request(
				"watch " + tubeName, 
				"WATCHING", 
				null,
				null,
				ExpectedResponse.None);
	        Response response = protocolHandler.get().processRequest(request);
			return Integer.parseInt(response.getReponse());
	}

	public int ignore(String tubeName) {
		Request request = new Request(
				"ignore " + tubeName, 
				new String[] {"WATCHING", "NOT_IGNORED"}, 
				null,
				null,
				ExpectedResponse.None);
	        Response response = protocolHandler.get().processRequest(request);
			return Integer.parseInt(response.getReponse());
	}
	
	// ****************************************************************
	// Consumer methods
	//	peek-related
	// ****************************************************************
	public Job peek(long jobId) {
		Job job = null;
		Request request = new Request(
			"peek " + jobId, 
			"FOUND",
			"NOT_FOUND",
			null,
			ExpectedResponse.ByteArray,
			2);
		Response response = protocolHandler.get().processRequest(request);
		if (response != null && response.isMatchOk()) {
			jobId = Long.parseLong(response.getReponse());
			job = new JobImpl(jobId);
			job.setData((byte[]) response.getData());
		}
		return job;
	}

	public Job peekBuried() {
		Job job = null;
		Request request = new Request(
			"peek-buried", 
			"FOUND",
			"NOT_FOUND",
			null,
			ExpectedResponse.ByteArray,
			2);
		Response response = protocolHandler.get().processRequest(request);
		if (response != null && response.isMatchOk()) {
			long jobId = Long.parseLong(response.getReponse());
			job = new JobImpl(jobId);
			job.setData((byte[]) response.getData());
		}
		return job;
	}

	public Job peekDelayed() {
		Job job = null;
		Request request = new Request(
			"peek-delayed", 
			"FOUND",
			"NOT_FOUND",
			null,
			ExpectedResponse.ByteArray,
			2);
		Response response = protocolHandler.get().processRequest(request);
		if (response != null && response.isMatchOk()) {
			long jobId = Long.parseLong(response.getReponse());
			job = new JobImpl(jobId);
			job.setData((byte[]) response.getData());
		}
		return job;
	}

	public Job peekReady() {
		Job job = null;
		Request request = new Request(
			"peek-ready", 
			"FOUND",
			"NOT_FOUND",
			null,
			ExpectedResponse.ByteArray,
			2);
		Response response = protocolHandler.get().processRequest(request);
		if (response != null && response.isMatchOk()) {
			long jobId = Long.parseLong(response.getReponse());
			job = new JobImpl(jobId);
			job.setData((byte[]) response.getData());
		}
		return job;
	}
	
	public int kick(int count) {
		Request request = new Request(
			"kick " + count, 
			"KICKED",
			null,
			null,
			ExpectedResponse.None);
		Response response = protocolHandler.get().processRequest(request);
		if (response != null && response.isMatchOk()) {
			count = Integer.parseInt(response.getReponse());
		}
		return count;
	}
	
	// ****************************************************************
	// Consumer methods
	//	stats-related
	// ****************************************************************
	@SuppressWarnings("unchecked")
	public Map<String, String> statsJob(long jobId) {
		Request request = new Request(
				"stats-job " + jobId , 
				"OK", 
				"NOT_FOUND",
				null,
				ExpectedResponse.Map);
	        Response response = protocolHandler.get().processRequest(request);
	        Map<String, String> map = null;
			if (response != null && response.isMatchOk()) {			
				map = (Map<String, String>) response.getData();
			}
			return map;
	}

	@SuppressWarnings("unchecked")
	public Map<String, String> statsTube(String tubeName) {
		if (tubeName == null) {
			return null;
		}
		
		Request request = new Request(
				"stats-tube " + tubeName, 
				"OK", 
				"NOT_FOUND",
				null,
				ExpectedResponse.Map);
	        Response response = protocolHandler.get().processRequest(request);
	        Map<String, String> map = null;
			if (response != null && response.isMatchOk()) {			
				map = (Map<String, String>) response.getData();
			}
			return map;
	}

	@SuppressWarnings("unchecked")
	public Map<String, String> stats() {
		Request request = new Request(
			"stats", 
			"OK", 
			null,
			null,
			ExpectedResponse.Map);
        Response response = protocolHandler.get().processRequest(request);
        Map<String, String> map = null;
		if (response != null && response.isMatchOk()) {			
			map = (Map<String, String>) response.getData();
		}
		return map;
	}

	@SuppressWarnings("unchecked")
	public List<String> listTubes() {
		Request request = new Request(
			"list-tubes", 
			"OK", 
			null,
			null,
			ExpectedResponse.List);
        Response response = protocolHandler.get().processRequest(request);
        List<String> list = null;
		if (response != null && response.isMatchOk()) {			
			list = (List<String>) response.getData();
		}
		return list;
	}

	public String listTubeUsed() {
		String tubeName = null;
		Request request = new Request(
			"list-tube-used",
			"USING", 
			null,
			null,
			ExpectedResponse.None);
        Response response = protocolHandler.get().processRequest(request);
		if (response != null && response.isMatchOk()) {			
			tubeName = response.getReponse();
		}
		return tubeName;
	}

	@SuppressWarnings("unchecked")
	public List<String> listTubesWatched() {
		Request request = new Request(
			"list-tubes-watched", 
			"OK", 
			null,
			null,
			ExpectedResponse.List);
        Response response = protocolHandler.get().processRequest(request);
        List<String> list = null;
		if (response != null && response.isMatchOk()) {			
			list = (List<String>) response.getData();
		}
		return list;
	}

	public String getClientVersion() {
		return VERSION;
	}

	public void close() {
		protocolHandler.get().close();
	}
}
