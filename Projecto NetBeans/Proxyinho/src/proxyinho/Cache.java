
package proxyinho;

import java.io.*;
import java.util.HashMap;

/**
 * Class que representa a cache do proxy, onde guarda todos os objectos nela
 * existentes e ainda tem metodos que permitem o seu controlo
 * 
 * @author José Ribeiro e Luis Vieira
 * 
 */
public class Cache implements Serializable{

    private HashMap<String, Objecto> objectos;
    
    private final String nomeFicheiroDados = "cache.dat";

    /**
     * Constructor da Cache
     */
    public Cache(){
        
        try {

            loadCache();

        } catch (Exception ex) {

            this.objectos = new HashMap<>();
        }
    }
    
    /**
     * 
     * Adiciona um objecto à cache 
     * Se o uri (chave) já existir substitui pelo objecto fornecido
     * 
     * @param uri
     * @param ele
     */
    public void addObjecto(String uri, Objecto ele){
        this.objectos.put(uri, ele);
    }
    
    /**
     * 
     * Remove um objecto da cache, atraves da sua uri (chave)
     * 
     * @param uri
     */
    public void removeObjecto(String uri){
        this.objectos.remove(uri);
    }
    
    /**
     * 
     * Verifica se uma uri tem o seu objecto em cache
     * 
     * @param uri
     * @return
     */
    public boolean existeEmCache(String uri){
        return this.objectos.containsKey(uri);
    }
    
    /**
     * 
     * Retorna um objecto pela sua uri
     * 
     * @param uri
     * @return
     */
    public Objecto getObjecto(String uri){
        return this.objectos.get(uri);
    }
    
    /**
     * 
     * Guarda o estado da cache actual num ficheiro de dados de modo a não
     * se perder com o fecho do programa
     * 
     */
    public void saveCache() throws IOException{

            FileOutputStream fos = new FileOutputStream(nomeFicheiroDados);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(this.objectos);
            oos.close();
            fos.close();

    }

    /**
     * 
     * Carrega o estado da cache actual através do ficheiro de dados
     * 
     */
    private void loadCache() throws FileNotFoundException, IOException, ClassNotFoundException{
        
            FileInputStream fis = new FileInputStream(nomeFicheiroDados);
            ObjectInputStream ois = new ObjectInputStream(fis);
            this.objectos = (HashMap<String, Objecto>) ois.readObject();
            ois.close();
            fis.close();
    
    }
    
}
