package com.surftools.BeanstalkClient;

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

public interface Client {
	
	public final String DEFAULT_HOST = "localhost";
	
	public final int DEFAULT_PORT = 11300;
	
	// ****************************************************************
	// Producer methods
	// ****************************************************************
	public long put (long priority, int delaySeconds, int timeToRun, byte[] data);	
	
	public void useTube (String tubeName);
	
	// ****************************************************************
	// Consumer methods
	//	job-related
	// ****************************************************************
	public Job reserve(Integer timeoutSeconds);
	
	public boolean delete(long jobId);
	
	public boolean release(long jobId, long priority, int delaySeconds );
	
	public boolean bury(long jobId, long priority);
	
	public boolean touch(long jobId);
	
	// ****************************************************************
	// Consumer methods
	//	tube-related
	// ****************************************************************
	public int watch(String tubeName);
	
	/**
	 * 
	 * @param tubeName
	 * @return -1, for NOT_IGNORED response in an attempt for a single tube
	 */
	public int ignore(String tubeName);
	
	// ****************************************************************
	// Consumer methods
	//	peek-related
	// ****************************************************************
	public Job peek(long jobId);
	
	public Job peekReady();
	
	public Job peekDelayed();
	
	public Job peekBuried();
	
	public int kick(int count);
	
	// ****************************************************************
	// Consumer methods
	//	stats-related
	// ****************************************************************
	public Map<String,String> statsJob(long jobId);
	
	public Map<String,String> statsTube(String tubeName);
	
	public Map<String,String> stats();
	
	public List<String> listTubes();
	
	public String listTubeUsed();
	
	public List<String> listTubesWatched();
	
	// ****************************************************************
	// Client methods
	// ****************************************************************
	/**
	 * return the version of this Beanstalkd Client
	 */
	public String getClientVersion();
	
	/**
	 * return the version of the beanstalkd daemon
	 * @return
	 */
	public String getServerVersion();
	
	/**
	 * close underlying connection to beanstalkd
	 */
	public void close();
	
	/**
	 * one unique connection per thread or a single shared connection?
	 * @return
	 */
	public boolean isUniqueConnectionPerThread();
	public void setUniqueConnectionPerThread(boolean uniqueConnectionPerThread);

}
