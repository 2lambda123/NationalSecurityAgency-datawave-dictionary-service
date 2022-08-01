package datawave.webservice.dictionary.edge;

import datawave.webservice.result.BaseResponse;
import io.protostuff.Message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import java.util.List;

@XmlAccessorType(XmlAccessType.NONE)
@XmlSeeAlso(DefaultEdgeDictionary.class)
public abstract class EdgeDictionaryBase<T,F extends MetadataBase<F>> extends BaseResponse implements Message<T> {
    
    private static final long serialVersionUID = 1L;
    
    public abstract List<? extends MetadataBase<F>> getMetadataList();
    
    public abstract void setTotalResults(long totalResults);
    
    public abstract long getTotalResults();
    
}
