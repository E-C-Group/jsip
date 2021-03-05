package co.ecg.jain_sip.core.ri;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface MultiValueMap<K,V> extends Map<K,List<V>>, Serializable {
    public Object removeKV( K key, V item );
}
