/*******************************************************************************
 * Copyright 2016
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.lmf.model.interfaces;

/**
 * An interface for UBY-LMF classes that have a unique identifier.
 *
 * @author Zijad Maksuti
 *
 */
public interface IHasID {


	/**
	 * Returns the unique identifier of the UBY-LMF object.
	 * @return the unique identifier of the UBY-LMF object or null,
	 * if the object does not have the identifier set <p>
	 * <i>
	 * Note that all UBY-LMF classes implementing {@link IHasID} interface should
	 * have this field set. Absence of the value of identifier field may indicate to an
	 * incomplete conversion process of the original resource.
	 * </i>
	 */
	String getId();

	/**
	 * Sets the unique identifier of this UBY-LMF object.
	 * @param id the unique identifier to set
	 */
	void setId(String id);
}
