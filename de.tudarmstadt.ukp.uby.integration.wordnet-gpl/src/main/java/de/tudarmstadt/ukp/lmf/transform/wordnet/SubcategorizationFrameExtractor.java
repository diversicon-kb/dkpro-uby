/**
 * Copyright 2015
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.tudarmstadt.ukp.lmf.transform.wordnet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.tudarmstadt.ukp.lmf.model.enums.ECase;
import de.tudarmstadt.ukp.lmf.model.enums.EComplementizer;
import de.tudarmstadt.ukp.lmf.model.enums.EDeterminer;
import de.tudarmstadt.ukp.lmf.model.enums.EGrammaticalFunction;
import de.tudarmstadt.ukp.lmf.model.enums.EGrammaticalNumber;
import de.tudarmstadt.ukp.lmf.model.enums.ELabelTypeSemantics;
import de.tudarmstadt.ukp.lmf.model.enums.ESyntacticCategory;
import de.tudarmstadt.ukp.lmf.model.enums.ESyntacticProperty;
import de.tudarmstadt.ukp.lmf.model.enums.ETense;
import de.tudarmstadt.ukp.lmf.model.enums.EVerbForm;
import de.tudarmstadt.ukp.lmf.model.meta.SemanticLabel;
import de.tudarmstadt.ukp.lmf.model.semantics.SemanticArgument;
import de.tudarmstadt.ukp.lmf.model.semantics.SemanticPredicate;
import de.tudarmstadt.ukp.lmf.model.semantics.SynSemArgMap;
import de.tudarmstadt.ukp.lmf.model.semantics.SynSemCorrespondence;
import de.tudarmstadt.ukp.lmf.model.syntax.LexemeProperty;
import de.tudarmstadt.ukp.lmf.model.syntax.SubcategorizationFrame;
import de.tudarmstadt.ukp.lmf.model.syntax.SyntacticArgument;
import de.tudarmstadt.ukp.lmf.transform.wordnet.util.WNConvUtil;

/**
 * This class extracts subcategorization frames of
 * <a href="URL#http://wordnet.princeton.edu/">WordNet 3.0</a>
 * by parising subcategorization-mapping file.
 * @author Zijad Maksuti
 * @author Judith Eckle-Kohler
 * @see SubcategorizationFrame
 *
 */
public class SubcategorizationFrameExtractor {
	private InputStream subcatStream; // stream of the file containing subcategorization mappings
	private int subcatFrameNumber = 0;
	private int syntacticArgumentNumber = 0; // Running number for creating ID's of SyntacticArguments
	private final Map<String, SubcategorizationFrame> codeFrameMappings  = new TreeMap<String, SubcategorizationFrame>();
	private final Map<String, SemanticPredicate> codePredMappings  = new TreeMap<String, SemanticPredicate>();

	private final Map<String, String> codeSynSemArgMapping  = new TreeMap<String, String>();
	private final Map<String, String> synSemArgSynArgMapping  = new TreeMap<String, String>();
	private final Map<String, SubcategorizationFrame> synArgSubcatFrameMapping  = new TreeMap<String, SubcategorizationFrame>();

	// list of all processed SemanticPredicates
	private final List<SemanticPredicate> semanticPredicates = new LinkedList<SemanticPredicate>();
	private int semanticPredicateNumber = 0;
	private int semanticArgumentNumber = 0;
	private final List<SynSemCorrespondence> synSemCorrespondences = new LinkedList<SynSemCorrespondence>();
	private int synSemCorrespondenceNumber = 0; // Running number for creating IDs

	private final Map<String, SynSemCorrespondence> synsemargsSynSemCorrMap  = new TreeMap<String, SynSemCorrespondence>();

	private final Log logger = LogFactory.getLog(getClass());

	private final TreeSet<SubcategorizationFrame> allSubcategorizationFrames = new TreeSet<SubcategorizationFrame>();
	
	private String prefix;

	/**
	 * Constructs a {@link SubcategorizationFrameExtractor}
	 * @param subcatStream stream of the File containing the SubcategorizationFrame-mappings
	 * @return SubcategorizationFrameExtractor-instance used for parsing subcatStream
	 * @see SubcategorizationFrame
	 */
	public SubcategorizationFrameExtractor(InputStream subcatStream, String prefix){
	    this.prefix = prefix;
		if(codeFrameMappings.isEmpty()){
			this.subcatStream = subcatStream;
			parseSubcatMappings();
			addAdjectiveSubcats(); // add subcategorization frames that apply for adjectives
			Collections.sort(semanticPredicates);
			allSubcategorizationFrames.addAll(codeFrameMappings.values());
		}
	}

	/**
	 * This method consumes a frame, coded in WordNet's files and returns it's
	 * associated SubcategorizationFrame,<br> generated by this extractor
	 * @param frame a String encoding a frame
	 * @return {@link SubcategorizationFrame}-instance, associated with the frame, or null
	 * if this extractor does not contain a mapping for frame
	 */
	public SubcategorizationFrame getSubcategorizationFrame(String frame){
		return codeFrameMappings.get(frame);
	}

	/**
	 * This method consumes a frame, coded in WordNet's files and returns it's
	 * associated SemanticPredicate,<br> generated by this extractor
	 * @param frame a String encoding a frame
	 * @return {@link SemanticPredicate}-instance, associated with the frame, or null
	 * if this extractor does not contain a mapping for frame
	 */
	public SemanticPredicate getSemanticPredicate(String frame){

		return codePredMappings.get(frame);
	}


	/**
	 * Returns a sorted list of all SubcategorizationFrames processed by this extractor
	 * @return all {@link SubcategorizationFrame}-instances, processed by this extractor
	 */
	public List<SubcategorizationFrame> getSubcategorizationFrames(){
		ArrayList<SubcategorizationFrame> result = new ArrayList<SubcategorizationFrame>(allSubcategorizationFrames);
		return result;
	}

	/**
	 * Returns a list of all SemanticPredicates processed by this extractor
	 * @return all {@link SemanticPredicate}-instances, processed by this extractor
	 */
	public List<SemanticPredicate> getSemanticPredicates(){
		LinkedList<SemanticPredicate> result = new LinkedList<SemanticPredicate>();
		result.addAll(codePredMappings.values());
		return result;
	}

	/**
	 * Returns a list of all SynSemCorrespondences processed by this extractor
	 * @return all {@link SynSemCorrespondence}-instances, processed by this extractor
	 */
	public List<SynSemCorrespondence> getSynSemCorrespondences() {
		LinkedList<SynSemCorrespondence> result = new LinkedList<SynSemCorrespondence>();
		result.addAll(synsemargsSynSemCorrMap.values());
		return result;
	}

	/**
	 * This method parses the file containing the SubcategorizationFrame-mappings
	 */
	private void parseSubcatMappings() {
		logger.info("Parsing SubcatMappings...");
		try{
			BufferedReader input = new BufferedReader(new InputStreamReader(this.subcatStream));
			String line;
			while ((line = input.readLine()) != null) {
				if(!line.startsWith("#")) {
					parseLine(line); // skipping comments
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Error reading from subcat mappings resource stream", e);
		}
		logger.info("done");

		// first, create SubcategorizationFrames
		Iterator<String> codeIterator = codeSynSemArgMapping.keySet().iterator();
		while (codeIterator.hasNext()) {
			String code = codeIterator.next();
			String synSemArgs = codeSynSemArgMapping.get(code);
			String synArgs = synSemArgSynArgMapping.get(synSemArgs);

			if (!synArgSubcatFrameMapping.containsKey(synArgs)) {
				SubcategorizationFrame subcategorizationFrame = new SubcategorizationFrame();
				String id = prefix + "_sf_".concat(Integer.toString(subcatFrameNumber));
				subcategorizationFrame.setId(id);
				subcatFrameNumber++;
				subcategorizationFrame = parseArguments(synSemArgs,subcategorizationFrame);

				synArgSubcatFrameMapping.put(synArgs,subcategorizationFrame);
				codeFrameMappings.put(code, subcategorizationFrame);

				if (synSemArgs.contains("semanticRole")) {
					SemanticPredicate semanticPredicate = new SemanticPredicate();
					semanticPredicate = parseSemanticArguments(synSemArgs,subcategorizationFrame);
					codePredMappings.put(code, semanticPredicate);
					semanticPredicates.add(semanticPredicate);
				}
			}
			else {
				SubcategorizationFrame subcategorizationFrame = synArgSubcatFrameMapping.get(synArgs);
				codeFrameMappings.put(code, subcategorizationFrame);

				if (synSemArgs.contains("semanticRole")) {
					SemanticPredicate semanticPredicate = new SemanticPredicate();
					semanticPredicate = parseSemanticArguments(synSemArgs,subcategorizationFrame);
					codePredMappings.put(code, semanticPredicate);
					semanticPredicates.add(semanticPredicate);
				}
			}
		}

	}

	/**
	 * This method parses a line of the file containing the subcategorization-mappings<br>
	 * Line of subcategorization-mappings file has the form: {@literal <CODE>%<Arg>:..:<Arg>}
	 * @param line a line of the file containing subcategorization-mappings
	 */
	private void parseLine(String line) {

		String [] parts = line.split("%");
		codeSynSemArgMapping.put(parts[0], parts[1]);

		if (parts[1].contains("semanticRole")) {
			String synArgs = parts[1].replaceFirst(",semanticRole=[a-z]+", "");
			synSemArgSynArgMapping.put(parts[1], synArgs);

		} else {
			synSemArgSynArgMapping.put(parts[1], parts[1]);
		}
	}

	/**
	 * This method parses syntactic arguments encoded in a line of subcategorization mapping file
	 * @param synSemArgs part of the line encoding the arguments
	 * @param subcatFrame subcategorization frame to which syntactic arguments should be appended
	 * @return subcategorization frame with appended syntactic arguments
	 * @see SubcategorizationFrame
	 * @see SyntacticArgument
	 */
	private SubcategorizationFrame parseArguments(String synSemArgs, SubcategorizationFrame subcatFrame) {
		SubcategorizationFrame scFrame = subcatFrame;
		List<SyntacticArgument> synArgs = new LinkedList<SyntacticArgument>();
		String[] args = synSemArgs.split(":");
		for(String arg : args) {
			if (!arg.contains("syntacticProperty")) {
				SyntacticArgument syntacticArgument = new SyntacticArgument();
				syntacticArgument.setId(WNConvUtil.makeId(prefix, SyntacticArgument.class, Integer.toString(syntacticArgumentNumber)));
				syntacticArgumentNumber++;
				String[] atts = arg.split(",");
				for(String att : atts){
					String [] splits = att.split("=");
					String attName = splits[0];
					if (attName.equals("grammaticalFunction")){
						String gf=splits[1];
						if(gf.endsWith("Comp")) {
							gf = gf.concat("lement");
						}
						syntacticArgument.setGrammaticalFunction(EGrammaticalFunction.valueOf(gf));
					}
					if(attName.equals("syntacticCategory")) {
						syntacticArgument.setSyntacticCategory(ESyntacticCategory.valueOf(splits[1]));
					}
					else
						if(attName.equals("optional")) {
							syntacticArgument.setOptional(splits[1].equals("yes"));
						}
						else
							if(attName.equals("case")) {
								syntacticArgument.setCase(ECase.valueOf(splits[1]));
							}
							else
								if(attName.equals("determiner")) {
									syntacticArgument.setDeterminer(EDeterminer.valueOf(splits[1]));
								}
								else
									if(attName.equals("preposition")) {
										syntacticArgument.setPreposition(splits[1]);
									}
									else
										if(attName.equals("prepositionType")) {
											syntacticArgument.setPrepositionType(splits[1]);
										}
										else
											if(attName.equals("number")) {
												syntacticArgument.setNumber(EGrammaticalNumber.valueOf(splits[1]));
											}
											else
												if(attName.equals("lex")) {
													syntacticArgument.setLexeme(splits[1]);
												}
												else
													if(attName.equals("verbForm")) {
														syntacticArgument.setVerbForm(EVerbForm.valueOf(splits[1]));
													}
													else
														if(attName.equals("tense")) {
															syntacticArgument.setTense(ETense.valueOf(splits[1]));
														}
														else
															if(attName.equals("complementizer")) {
																syntacticArgument.setComplementizer(EComplementizer.valueOf(splits[1]));
															}
				}
				synArgs.add(syntacticArgument);
			} else {
				String [] splits = arg.split("=");
				String sp = splits[1];
				if (sp.equals("raising")) {
					sp = sp.replaceAll("raising", "subjectRaising");
				}
				LexemeProperty lexemeProperty = new LexemeProperty();
				lexemeProperty.setSyntacticProperty(ESyntacticProperty.valueOf(sp));
				scFrame.setLexemeProperty(lexemeProperty);
			}
		}
		scFrame.setSyntacticArguments(synArgs);
		return scFrame;
	}


	/**
	 * This method consumes the part of the line of subcategorization mapping file encoding semantic arguments. <br>
	 * It parses the arguments and returns an instance of {@link SemanticPredicate} class containing the arguments
	 * @param synSemArgs part of the line encoding semantic arguments.
	 * @param subcategorizationFrame instance of {@link SubcategorizationFrame} class used for creating
	 * instances of {@link SynSemArgMap} class
	 * @return semantic predicate containing parsed semantic arguments
	 */
	private SemanticPredicate parseSemanticArguments(String synSemArgs,SubcategorizationFrame subcategorizationFrame) {
		// list of mappings between syntactic and semantic arguments is to be created
		SemanticPredicate semanticPredicate = new SemanticPredicate();
		semanticPredicate.setId(prefix + "_sp_".concat(Integer.toString(semanticPredicateNumber)));
		semanticPredicateNumber++;
		List<SemanticArgument> semanticArguments = new LinkedList<SemanticArgument>();
		List<SynSemArgMap> synSemArgMaps = new LinkedList<SynSemArgMap>();
		SynSemArgMap synSemArgMap = new SynSemArgMap();

		String[] args = synSemArgs.split(":");
		int index = 0;
		// iterate over syntactic Arguments
		for (SyntacticArgument synArg: subcategorizationFrame.getSyntacticArguments()) {
			String synsemArg = args[index];
			if (synsemArg.contains("syntacticProperty")) {
				index++;
				synsemArg = args[index];
			}
			// look at synsemArg: is semantic role defined? if yes: create corresponding semanticArg
			String[] atts = synsemArg.split(",");
			for(String att : atts){
				String [] splits = att.split("=");
				String attName = splits[0];
				if(attName.equals("semanticRole")){
					SemanticArgument semanticArgument = new SemanticArgument();
					semanticArgument.setId(WNConvUtil.makeId(prefix, SemanticArgument.class, Integer.toString(semanticArgumentNumber)));
					semanticArgumentNumber++;
					semanticArgument.setSemanticRole(splits[1]);
					semanticArguments.add(semanticArgument);
					// Generate SynSemArgMapping
					synSemArgMap.setSyntacticArgument(synArg);
					synSemArgMap.setSemanticArgument(semanticArgument);
					synSemArgMaps.add(synSemArgMap);

					// creating SemanticLabel for "somebody" and "something"
					if(splits[1].equals("somebody") || splits[1].equals("something")){
						// Create a semantic label
						SemanticLabel semanticLabel = new SemanticLabel();
						semanticLabel.setLabel(splits[1]);
						semanticLabel.setType(ELabelTypeSemantics.selectionalPreference);
						List<SemanticLabel> semanticLabels = new LinkedList<SemanticLabel>();
						semanticLabels.add(semanticLabel);
						semanticArgument.setSemanticLabels(semanticLabels);
					}
				}
			}
			index++;
		}
		semanticPredicate.setSemanticArguments(semanticArguments);

		SynSemCorrespondence synSemCorrespondence = new SynSemCorrespondence();
		synSemCorrespondence.setId(
		        WNConvUtil.makeId(prefix, SynSemCorrespondence.class , Integer.toString(synSemCorrespondenceNumber)));
		synSemCorrespondenceNumber++;
		synSemCorrespondence.setSynSemArgMaps(synSemArgMaps);
		synSemCorrespondences.add(synSemCorrespondence);
		synsemargsSynSemCorrMap.put(synSemArgs,synSemCorrespondence);
		return semanticPredicate;
	}

	private void setId(SubcategorizationFrame sf){
	    sf.setId(prefix + "_sf_".concat(Integer.toString(subcatFrameNumber++)));
	}
	/**
	 * This method adds mappings to the codes of SubcategorizationFrames
	 * that apply only for adjectives
	 * @see SubcategorizationFrame
	 */
	private void addAdjectiveSubcats() {
		// (p) predicativeAdjective
		SubcategorizationFrame predAdj = new SubcategorizationFrame();
		setId(predAdj);
		LexemeProperty lpPredAdj = new LexemeProperty();
		lpPredAdj.setSyntacticProperty(ESyntacticProperty.predicativeAdjective);
		predAdj.setLexemeProperty(lpPredAdj);
		codeFrameMappings.put("p", predAdj);

		// (a) preattributiveAdjective
		SubcategorizationFrame preattAdj = new SubcategorizationFrame();
		setId(preattAdj);
		LexemeProperty lpPreattAdj = new LexemeProperty();
		lpPreattAdj.setSyntacticProperty(ESyntacticProperty.nonPredicativeAdjective);
		preattAdj.setLexemeProperty(lpPreattAdj);
		codeFrameMappings.put("a", preattAdj);

		// (ip) postattributiveAdjective
		SubcategorizationFrame postattAdj = new SubcategorizationFrame();
		setId(postattAdj);
		LexemeProperty lpPostattAdj = new LexemeProperty();
		lpPostattAdj.setSyntacticProperty(ESyntacticProperty.postpositiveAdjective);
		postattAdj.setLexemeProperty(lpPostattAdj);
		codeFrameMappings.put("ip", postattAdj);
		}
}
