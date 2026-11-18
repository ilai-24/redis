import java.util.concurrent.ConcurrentHashMap;

//responsible for managing data in storage
public class StorageEngine {
    private final ConcurrentHashMap<String,String>store=new ConcurrentHashMap<>();
    public void set(String key, String value){
        store.put(key, value);
    }
    public String get(String key){
        return store.get(key);
    }
    public boolean del(String key){
        return store.remove(key)!=null;
    }
}
