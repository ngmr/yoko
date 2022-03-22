/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package test.rmi;

import java.io.Serializable;
import java.rmi.Remote;
import java.util.List;

public class SampleSerializable implements Serializable {
	private int i = 0;
	private List aVector;
	private org.omg.CORBA.Object corbaObj;
	private Remote remote;
	private Object remoteObj;
	private test.rmi.SampleCorba sampleCorba;
	private SampleRemote sampleRemote;
	private Serializable serializable;
	private Object serializableObj1, serializableObj2;

	public void setInt(int i) { this.i = i; }
	public int getInt() { return i; }

	public void setSampleCorba(SampleCorba sampleCorba) {
		this.sampleCorba = sampleCorba;
	}
	public SampleCorba getSampleCorba() {
		return sampleCorba;
	}

	public void setCorbaObj(org.omg.CORBA.Object obj) {
		this.corbaObj = obj;
	}
	public void setRemote(Remote remote) {
		this.remote = remote;
	}
	public void setRemoteObject(Remote remote) {
		this.remoteObj = remote;
	}
	public void setSerializableObject(Serializable ser) {
		this.serializableObj1 = this.serializableObj2 = ser;
	}

	public org.omg.CORBA.Object getCorbaObj() {
		return corbaObj;
	}

	public Remote getRemote() { return remote; }

	public Object getRemoteObject() {
		return remoteObj;
	}

	public Object getSerializableObject() {
		if(serializableObj1 == serializableObj2) {
			return serializableObj1;
		}
		else {
			throw new Error("Expected serializable objects to be == identical");
		}
	}

	public void setList(List l) {
		aVector = l;
	}

	public List getList() {
		return aVector;
	}

	public void setSerializable(Serializable s) {
		this.serializable = s;
	}

	public Serializable getSerializable() {
		return serializable;
	}

	public void setSampleRemote(SampleRemote sampleRemote) {
		this.sampleRemote = sampleRemote;
	}
	public SampleRemote getSampleRemote() {
		return sampleRemote;
	}
}
