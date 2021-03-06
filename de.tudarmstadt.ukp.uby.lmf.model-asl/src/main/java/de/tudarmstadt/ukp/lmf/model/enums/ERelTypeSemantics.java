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
package de.tudarmstadt.ukp.lmf.model.enums;

import de.tudarmstadt.ukp.lmf.model.semantics.SenseRelation;
import de.tudarmstadt.ukp.lmf.model.semantics.SynsetRelation;

/**
 * Enumeration of possible relation types specified by an instance of
 * {@link SenseRelation} or {@link SynsetRelation} class.
 */
public enum ERelTypeSemantics {

	taxonomic, // e.g. hyponym
	partWhole, // e.g. metonym
	association, // e.g. see also
	label,	// e.g. region, topic, but also capital, currency
	predicative, // e.g. is played by, flows through
	complementary, // e.g. antonym
	frameRelation, // e.g. subframe, relation between semantic predicates
	sameFrame, // e.g. excludes, relation between semantic predicates
	coreSet, // e.g. core_set_4 relation between semantic arguments

	@Deprecated translation,

	@Deprecated labelOmegaWiki,

	@Deprecated predicativeOmegaWiki

}
