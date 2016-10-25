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
package de.tudarmstadt.ukp.lmf.transform.wordnet.util;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.Exc;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.POS;
import net.sf.extjwnl.data.Pointer;
import net.sf.extjwnl.data.PointerType;
import net.sf.extjwnl.data.Word;
import net.sf.extjwnl.dictionary.Dictionary;
import de.tudarmstadt.ukp.lmf.model.core.LexicalEntry;
import de.tudarmstadt.ukp.lmf.model.core.Lexicon;
import de.tudarmstadt.ukp.lmf.model.core.Sense;
import de.tudarmstadt.ukp.lmf.model.core.TextRepresentation;
import de.tudarmstadt.ukp.lmf.model.enums.EPartOfSpeech;
import de.tudarmstadt.ukp.lmf.model.meta.MetaData;
import de.tudarmstadt.ukp.lmf.model.semantics.SemanticArgument;
import de.tudarmstadt.ukp.lmf.model.semantics.SemanticPredicate;
import de.tudarmstadt.ukp.lmf.model.semantics.SenseExample;
import de.tudarmstadt.ukp.lmf.model.semantics.SynSemCorrespondence;
import de.tudarmstadt.ukp.lmf.model.semantics.Synset;
import de.tudarmstadt.ukp.lmf.model.syntax.SubcategorizationFrame;
import de.tudarmstadt.ukp.lmf.model.syntax.SyntacticArgument;
import de.tudarmstadt.ukp.lmf.model.syntax.SyntacticBehaviour;

/**
 * This class offers methods offers some helper methods used when converting
 * WordNet UBY-LMF.
 * @author Zijad Maksuti
 *
 */
public class WNConvUtil {

    private final static Log logger = LogFactory.getLog(WNConvUtil.class);
    
    /**
     * div new
     */
    public static final String DEFAULT_PREFIX = "WN";

    /**
     * div new
     */
    public static final Map<Class, String> NAMESPACES = new HashMap<>();
    
    static {
        NAMESPACES.put(Lexicon.class, "lex");
        NAMESPACES.put(Sense.class, "s");
        NAMESPACES.put(SenseExample.class, "se");
        NAMESPACES.put(Synset.class, "ss");        
        NAMESPACES.put(LexicalEntry.class, "le");
        NAMESPACES.put(MetaData.class, "md");
        NAMESPACES.put(SynSemCorrespondence.class, "ssc");
        NAMESPACES.put(SubcategorizationFrame.class, "scf");
        NAMESPACES.put(SyntacticArgument.class, "sya");
        NAMESPACES.put(SemanticArgument.class, "sea");
        NAMESPACES.put(SyntacticBehaviour.class, "sb");
        NAMESPACES.put(SemanticPredicate.class, "sp");
    }
    
    
	/**
	 * This method consumes a {@link POS}
	 * and returns corresponding {@link EPartOfSpeech}
	 * @param pos part of speech encoded in extJWNL-API
	 * @return associated part of speech defined in UBY-LMF
	 * @since 0.2.0
	 */
	public static EPartOfSpeech getPOS(POS pos) {
		switch (pos) {
			case NOUN:
				return EPartOfSpeech.noun;
			case VERB:
				return EPartOfSpeech.verb;
			case ADJECTIVE:
				return EPartOfSpeech.adjective;
			case ADVERB:
				return EPartOfSpeech.adverb;
		}
		return null;
	}

	public static List<TextRepresentation> makeTextRepresentationList(
			final String text, final String language) {
		List<TextRepresentation> result = new LinkedList<TextRepresentation>();
		TextRepresentation textRepresentation = new TextRepresentation();
		textRepresentation.setLanguageIdentifier(language);
		textRepresentation.setWrittenText(text);
		result.add(textRepresentation);
		return result;
	}

	
    /**
     * div new
     * 
     * Makes a generic id (like i.e. 'wn_lex_2') partitioned by {@code clazz}
     *  using {@link #NAMESPACES} mappings. No sanitization is done on value!
     */
    public static String makeId(String prefix, Class clazz, String value) {
        return prefix + "_" + NAMESPACES.get(clazz) + "_" + value;
    }

    /**
     * div new
     * 
     * Sanitizes ids so they can be valid xml ids for DTDs
     * (technically, they need to be NCNames).
     * 
     * In particular, non letters are converted to their unicode equivalent name in capital letters, 
     *   so for example lemma {@code euro} becomes {@code _EURO_}. Common 
     *   non-letters like apostrophe get converted to "-".
     * 
     */
    public static String xmlId(String wnId){
        if (wnId == null){
            throw new IllegalArgumentException("Tried to parse a null wordnet id!");
        }
        if (wnId.trim().isEmpty()){
            throw new IllegalArgumentException("Tried to parse a blank wordnet id!");
        }
        
        String wnIdNoSpaces = wnId.trim()
                .replaceAll("(?U)\\s", "-")
                .replaceAll("'", "-");
        
        String fid;
        
        String first = wnIdNoSpaces.substring(0,1); 
        
        if ( first.equals(".")
                || first.equals("-")
                || Character.isDigit(first.charAt(0))){
            fid = "_" + wnIdNoSpaces;
        } else {
            fid = wnIdNoSpaces;
        }
        
        
        Pattern p = Pattern.compile("[^\\p{L}|\\d|\\.|\\_|-]", Pattern.UNICODE_CHARACTER_CLASS);
        Matcher m = p.matcher(fid);
        StringBuilder sb = new StringBuilder();        
        int lastEnd = 0;
        
        while (m.find()){                      
            sb.append(fid.substring(lastEnd, m.start()));
            if (m.group().length() > 1){
                logger.warn("Found group in id of length > 1: " + m.group());
            }
            int codePoint = Character.codePointAt(fid, m.start());            
            String charName = Character.getName(codePoint);
            if (charName == null){
                sb.append("-");
            } else {               
                sb.append("_"+charName.replaceAll(p.toString(), "-") + "_");    
            }
            
            lastEnd = m.end();
        }
        if (lastEnd < fid.length()){
            sb.append(fid.substring(lastEnd, fid.length()));
        }
                
        return sb.toString();
    }
        

    // div new
    private WNConvUtil(){}

    /**
     * div new
     * 
     * Returns a valid xml id (i.e. something like wn_le_n-vehicle)
     * 
     * For sanitization rules, see {@link #xmlId}
     */
    public static String makeLexicalEntryId(String prefix, POS pos, String lemma) {        
        return makeId(prefix, LexicalEntry.class, xmlId(pos.getKey() + "_" + lemma));
    }    
    
    /**
     * div new
     * 
     * Returns something like wn_ss_n321534
     */
    public static String makeSynsetId(String prefix, net.sf.extjwnl.data.Synset wnSynset) {
        return WNConvUtil.makeId(prefix,
                Synset.class, 
                wnSynset.getPOS().getKey() + Long.toString(wnSynset.getOffset()));
    }

    /**
     * div new 
     * 
     * <p>div copied and adapted from {@link Word#getSenseKey()} so it returns valid XML ids.
     *      
     * For example, for the SenseKey
     * 
     *   <pre>entity%1:03:00::</pre>   
     *   returns 
     *   <pre>prefix_entity_1.03.00..  </pre>        
     * <p>
     * 
     * For lemma sanitization, see {@link #xmlId(String)}
     * 
     * @param i if < 1 does nothing, otherwise number 'i' is appended to lemma
     * 
     * @return sense key
     * @throws JWNLException JWNLException
     */
    public static String makeSenseId(String prefix, Word lexeme, int i) {
        
        int ss_type = lexeme.getPOS().getId();
        if (POS.ADJECTIVE == lexeme.getSynset().getPOS() && lexeme.getSynset().isAdjectiveCluster()) {
            ss_type = POS.ADJECTIVE_SATELLITE_ID;
        }

        String dot = ".";
                
        
        // a bit convoluted but should make sense
        StringBuilder senseKey = 
                new StringBuilder(prefix + "_" 
                            + xmlId(NAMESPACES.get(Sense.class) 
                                    + "_" + lexeme.getLemma()
                                    + (i < 1 ? "" : "-" + i)));
        senseKey.append("_").append(ss_type).append(dot);
        if (lexeme.getSynset().getLexFileNum() < 10) {
            senseKey.append("0");
        }
        senseKey.append(lexeme.getSynset().getLexFileNum()).append(dot);
        if (lexeme.getLexId() < 10) {
            senseKey.append("0");
        }
        senseKey.append(lexeme.getLexId()).append(dot);

        if (POS.ADJECTIVE_SATELLITE_ID == ss_type) {
            List<Pointer> p = lexeme.getSynset().getPointers(PointerType.SIMILAR_TO);
            if (0 < p.size()) {
                Pointer headWord = p.get(0);
                List<Word> words;
                try {
                    words = headWord.getTargetSynset().getWords();
                } catch (JWNLException e) {                
                    throw new RuntimeException(e);
                }
                if (0 < words.size()) {
                    Word word = words.get(0);
                    senseKey.append(xmlId(word.getLemma())).append(dot);
                    if (word.getLexId() < 10) {
                        senseKey.append("0");
                    }
                    senseKey.append(word.getLexId());
                }
            }
        } else {
            senseKey.append(dot);
        }

        return senseKey.toString();
    }
    
    
    /**
     * Wordnet has dictionary like this:  mice -> mouse , some-other-baseform, ...
     * 
     * We want an inverted dictionary:  mouse -> mice, ...
     * 
     * div new
     */
    public static Map<POS, HashMap<String, List<String>>> buildExceptionMap(Dictionary dictionary) {

        Map<POS, HashMap<String, List<String>>> ret = new HashMap<>();
        
        HashSet<String> baseformsWithMultipleExceptions = new HashSet<>();
        
        
        ret.put(POS.NOUN, new HashMap());
        ret.put(POS.ADJECTIVE, new HashMap());
        ret.put(POS.ADVERB, new HashMap());
        ret.put(POS.VERB, new HashMap());
        
        try {
            List<String> skipped = new ArrayList<>();            
            
            for (POS pos : POS.getAllPOS()) {
                Iterator<Exc> it = dictionary.getExceptionIterator(pos);
                while (it.hasNext()) {
                    Exc exc = it.next();

                    // Note: the 'lemma' here is actually the exception ...
                    String weirdForm = exc.getLemma();

                    // ... and the exceptions are actually the baseForm O_o
                    for (String baseForm : exc.getExceptions()) {
                        
                        // because exceptions sometimes have dash "-" like "goose-step"
                        // and sometimes have space like "join battle"
                        
                        IndexWord normalizedBaseFormWord = dictionary.lookupIndexWord(pos, baseForm);
                                      

                        if (normalizedBaseFormWord == null){
                            logger.debug("Couldn't look up index word for baseForm " + pos.getKey() + " " +  baseForm + "  , skipping it!");
                            skipped.add(pos.getKey() + " " + baseForm);
                        } else {
                            String normalizedBaseForm = normalizedBaseFormWord.getLemma();
                            List<String> weirdForms = ret.get(pos)
                                    .get(normalizedBaseForm);
                            if (weirdForms == null) {
                              weirdForms = new ArrayList<>();
                            } else {                            
                             baseformsWithMultipleExceptions.add(pos.getKey() + " " + normalizedBaseForm);                                                                                                                 
                            }
                            
                            if (!weirdForms.contains(weirdForm)){
                              weirdForms.add(weirdForm);    
                            }

                            ret.get(pos).put(normalizedBaseForm, weirdForms);
                            
                        }
                        
                        
                        

                    }
                }
                                
            }
            if (skipped.size() > 0){
                logger.info("Skipped " + skipped.size() + " WordForm exceptions because their baseform was not defined in Wordnet. This is a Wordnet issue, for details see https://github.com/DavidLeoni/dkpro-uby/issues/4 ");
                logger.debug("Complete list is :  \n" + skipped.toString());
            }
            
            
            if (!baseformsWithMultipleExceptions.isEmpty()){                
                
                for (String pbf : baseformsWithMultipleExceptions){
                    POS bfPos = POS.getPOSForKey(pbf.substring(0,1));
                    String bf = pbf.substring(2);
                    logger.debug("  " + pbf + " : " + ret.get(bfPos).get(bf));                    
                }
                
                logger.debug("\nFound " + baseformsWithMultipleExceptions.size() + " baseforms which have multiple exceptions (see above log)");
                
            }
            
            return ret;
        } catch (JWNLException e) {
            throw new RuntimeException(e);
        }
    }    
    
}
