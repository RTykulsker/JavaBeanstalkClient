package com.surftools.BeanstalkClientImpl;

/*

 Copyright 2009-2013 Robert Tykulsker 

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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.surftools.BeanstalkClient.BeanstalkException;

/**
 * simple class to serialize and deserialize any Serializable object.
 * 
 * @author bobt
 * 
 */
public class Serializer {
	public static byte[] serializableToByteArray(Serializable serializable) {
		byte[] bytes;
		ByteArrayOutputStream baos = null;
		ObjectOutputStream oos = null;
		try {
			baos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(baos);
			oos.writeObject(serializable);
			oos.flush();
			bytes = baos.toByteArray();
			oos.close();
			baos.close();
		} catch (Exception e) {
			throw new BeanstalkException(e.getMessage());
		}
		return bytes;
	}

	public static Serializable byteArrayToSerializable(byte[] bytes) {
		Serializable serializable = null;
		ByteArrayInputStream bais = null;
		ObjectInputStream ois = null;
		try {
			bais = new ByteArrayInputStream(bytes);
			ois = new ObjectInputStream(bais);
			serializable = (Serializable) ois.readObject();
		} catch (Exception e) {
			throw new BeanstalkException(e.getMessage());
		}
		return serializable;
	}

}
