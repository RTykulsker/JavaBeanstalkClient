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

import com.surftools.BeanstalkClient.Job;

public class JobImpl implements Job {
	private byte[] data;
	private long jobId;
	
	
	public JobImpl (long jobId) {
		this.jobId = jobId;
	}

	public byte[] getData() {
		return data;
	}

	public long getJobId() {
		return jobId;
	}


	public void setData(byte[] data) {
		this.data = data;
	}
	
}
