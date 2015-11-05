package com.test.lru.memory.disk.cache.dist;

import com.lru.memory.disk.cache.AbstractCacheService;
import com.lru.memory.disk.cache.Utl;
import com.lru.memory.disk.cache.DistributedRequestResponse;
import com.lru.memory.disk.cache.DistributedConfigServer;
import com.lru.memory.disk.cache.DistributedManager;
import com.lru.memory.disk.cache.Distributor;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.Socket;

/**
 *
 * @author sathayeg
 */
public class TDistributed {

    public static void main(String[] args) {
        try{
            TDistributed td = new TDistributed();
            //td.createMgr();
            td.distribute();
        }catch(Exception e){
            p("err: " + e + " : cause: " + e.getCause());
            System.exit(0);
        }
    }

    void createMgr() throws Exception {
        Cache cache = new Cache("mycache", 10);
        DistributedManager dm = new DistributedManager(9090, "127.0.0.1:9090", cache);
        p("dm cache map size: " + dm.getCacheMap().size());
        p("dm cluster servers size: " + dm.getClusterServers().size());
        p("dm server port: " + dm.getServerPort());
    }
    
    void distribute() throws Exception {
        p("start distribute");
        Cache cache = new Cache("mycache", 10);
        
        String clusterConfig = "127.0.0.1:18082, 127.0.0.1:18081";
        
        Distributor.distribute(18081, clusterConfig, cache);
        Distributor.distribute(18082, clusterConfig, cache);
        
        DistributedManager distMgr = Distributor.getDistMgr(18082);
        tSocketClient(distMgr, "key1", cache);
        tSocketClient(distMgr, "key1", cache);
        
        tSocketClient(distMgr, "key2", cache);
        tSocketClient(distMgr, "key3", cache);
        tSocketClient(distMgr, "key4", cache);
        tSocketClient(distMgr, "key5", cache);
        
        System.exit(0);
    }
    
    static void tSocketClient(DistributedManager distMgr, String key, Cache cache) throws Exception {  
        
        InputStream is = null;
        OutputStream os = null;
        
        ObjectOutputStream oos = null;
        ObjectInputStream ois = null;

        Socket clientSock = null;

        AutoCloseable closeables[] = {is, os, oos, ois, clientSock};
        try {
            DistributedConfigServer clusterServer = distMgr.getClusterServerForCacheKey(key);
            p("client: key: " + key + ", cluster server: " + clusterServer.toString());
            if(clusterServer.isSelf()){
                p("key: " + key + ", get from jvm");
            }else{
                p("key: " + key + ", get from remote");
            }
            
            p("create client sock: " + clusterServer.getHost() + ", " + clusterServer.getPort());
            clientSock = new Socket(clusterServer.getHost(), clusterServer.getPort());
            DistributedRequestResponse<Serializable> cssr = 
                    new DistributedRequestResponse<>(clusterServer.getHost(), clusterServer.getPort(),
                            key, cache.getCacheName());
            
            os = clientSock.getOutputStream();
            oos = new ObjectOutputStream(os);
            oos.writeObject(cssr);
            oos.flush(); 
            
            is = clientSock.getInputStream();
            ois = new ObjectInputStream(is);
            DistributedRequestResponse<Serializable> resp = (DistributedRequestResponse<Serializable>) ois.readObject();
            p(resp.toString());
            p("-----");p(" ");
        } catch (Exception e) {
            p("error: sendDataToServer: " + e);
        } finally {
            Utl.closeAll(closeables);
        }
    }
    
    public class Cache extends AbstractCacheService<String> {

        public Cache(String cacheName, int cacheSize) throws Exception {
            super(cacheName, cacheSize);
        }

        @Override
        public boolean isCacheItemValid(String o) {
            return (null != o);
        }

        @Override
        public String loadData(String key) throws Exception {
            return ("data for key: " + key);
        }
        
    }

    static void p(Object o) {
        System.out.println(o);
    }
}