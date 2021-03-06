/**
 * Copyright 2008 - 2015 The Loon Game Engine Authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * @project loon
 * @author cping
 * @email：javachenpeng@yahoo.com
 * @version 0.5
 */
package loon.action.map;

public class Attribute {

	private String name;

	private Object attribute;

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Object getAttribute() {
		return this.attribute;
	}

	public int getAttributeInt() {
		return ((Integer) this.attribute).intValue();
	}

	public void setAttribute(Object attribute) {
		this.attribute = attribute;
	}

	public Attribute cpy(Attribute other){
		this.name = other.name;
		this.attribute = other.attribute;
		return this;
	}
}
