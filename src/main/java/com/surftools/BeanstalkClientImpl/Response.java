package com.surftools.BeanstalkClientImpl;

/*

 Copyright 2009-2020 Robert Tykulsker 

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

public class Response {
	private String status;
	private String reponse;
	private String responseLine;
	private boolean matchOk;
	private boolean matchError;
	private Object data;

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getReponse() {
		return reponse;
	}

	public void setReponse(String reponse) {
		this.reponse = reponse;
	}

	public String getResponseLine() {
		return responseLine;
	}

	public void setResponseLine(String responseLine) {
		this.responseLine = responseLine;
	}

	public boolean isMatchOk() {
		return matchOk;
	}

	public void setMatchOk(boolean matchOk) {
		this.matchOk = matchOk;
	}

	public boolean isMatchError() {
		return matchError;
	}

	public void setMatchError(boolean matchError) {
		this.matchError = matchError;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

}
