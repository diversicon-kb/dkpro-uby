package de.tudarmstadt.ukp.alignment.framework.uima;

import java.io.IOException;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.uimafit.component.CasCollectionReader_ImplBase;
import org.uimafit.descriptor.ConfigurationParameter;


public class StringReader extends CasCollectionReader_ImplBase {


	public static final String PARAM_CONTENT = "String for processing";
	@ConfigurationParameter(name = PARAM_CONTENT, mandatory=true)
	private String mContent;

	private boolean done=false;
	@Override
	public void getNext(CAS aCAS) throws IOException, CollectionException {
		JCas jcas = null;
		try {
			jcas = aCAS.getJCas();
		} catch (CASException e) {
			e.printStackTrace();
		}
		jcas.setDocumentText(mContent);
		done=true;
	}

	@Override
	public boolean hasNext() throws IOException, CollectionException {
		return !done;
	}

	@Override
	public Progress[] getProgress() {
		return new Progress[] { new ProgressImpl(1, 1, mContent) };
	}

}