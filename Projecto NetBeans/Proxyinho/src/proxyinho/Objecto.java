
package proxyinho;

import java.io.Serializable;
import java.util.GregorianCalendar;

/**
 * Class que representa um objecto pedido a um servidor, representado depois na cache
 * 
 * @author José Ribeiro e Luis Vieira
 * 
 */
public class Objecto implements Serializable{

    private GregorianCalendar dataIni;
    private GregorianCalendar dataExpiracao;
    private GregorianCalendar dataUltimaModificacao;
    private String etag;
    private int length;
    private long idadeMax;
    private byte body[];

    /**
     * Constructor
     * 
     * @param etag
     * @param dataExpiracao
     * @param dataUltimaModificacao
     * @param length
     * @param idadeMax
     * @param body
     */
    public Objecto(String etag, GregorianCalendar dataExpiracao, GregorianCalendar dataUltimaModificacao, int length, long idadeMax, byte body[]){
        this.length = length;
        this.idadeMax = idadeMax;
        this.body = body;
        this.dataIni = new GregorianCalendar();
        this.dataExpiracao=dataExpiracao;
        this.dataUltimaModificacao=dataUltimaModificacao;
        this.etag=etag;
    }

    /**
     * 
     * Idade do objecto (diferença entre a data actual e a data do pedido do objecto
     * 
     * @return idade em segundos
     */
    public long getIdade(){
        GregorianCalendar actual = new GregorianCalendar();
        return (actual.getTimeInMillis() - this.getDataIni().getTimeInMillis())*1000;
    }

    /**
     * 
     * Verifica se a data de expiração do objecto é superior a data actual
     * 
     * @return boolean expirado
     */
    public boolean isExpirado(){

        if(this.dataExpiracao==null) return false;

        GregorianCalendar actual = new GregorianCalendar();

        return actual.after(this.getDataExpiracao());

    }
    
    /**
     * 
     * Verifica se o objecto foi modificado desde a sua leitura, isto é
     * se a data de modificação é superior à que temos do mesmo
     * 
     * @param ultimaModificacao
     * @return boolean modificado
     */
    public boolean isModificado(GregorianCalendar ultimaModificacao){
        
        if(this.dataUltimaModificacao==null) return false;
        
        return ultimaModificacao.after(this.getDataUltimaModificacao());

    }
    
    /**
     * 
     * Verifica se a length do objecto é diferente
     * 
     * @param novaLength
     * @return boolean length diferente
     */
    public boolean isLengthDiferente(long novaLength){
        
        if(this.length==0) return false;
        
        return (novaLength!=this.getLength());
        
    }
    
    /**
     * 
     * Verifica se a idade maxima do objecto foi ultrapassada
     * 
     * @return
     */
    public boolean idadeMaxUltrapassada(){
        
        if(this.getIdadeMax()==0) return false;
        
        return this.getIdade() > this.getIdadeMax();
        
    }

    /**
     * 
     * Verifica se a etag do objecto foi alterada
     * 
     * @param etag
     * @return bolean etag diferente
     */
    public boolean isEtagDiferente(String etag){
    
        if(this.etag.equals("")) return false;
        
        return !this.etag.equals(etag);
        
    }

    /**
     * 
     * @return
     */
    public GregorianCalendar getDataIni() {
        return dataIni;
    }

    /**
     * 
     * @return
     */
    public GregorianCalendar getDataExpiracao() {
        return dataExpiracao;
    }

    /**
     * 
     * @return
     */
    public GregorianCalendar getDataUltimaModificacao() {
        return dataUltimaModificacao;
    }

    /**
     * 
     * @return
     */
    public String getEtag() {
        return etag;
    }

    /**
     * 
     * @return
     */
    public int getLength() {
        return length;
    }

    /**
     * 
     * @return
     */
    public long getIdadeMax() {
        return idadeMax;
    }

    /**
     * 
     * @return
     */
    public byte[] getBody() {
        return body;
    }

    /**
     * 
     * @param dataIni
     */
    public void setDataIni(GregorianCalendar dataIni) {
        this.dataIni = dataIni;
    }

    /**
     * 
     * @param dataExpiracao
     */
    public void setDataExpiracao(GregorianCalendar dataExpiracao) {
        this.dataExpiracao = dataExpiracao;
    }

    /**
     * 
     * @param dataUltimaModificacao
     */
    public void setDataUltimaModificacao(GregorianCalendar dataUltimaModificacao) {
        this.dataUltimaModificacao = dataUltimaModificacao;
    }

    /**
     * 
     * @param etag
     */
    public void setEtag(String etag) {
        this.etag = etag;
    }

    /**
     * 
     * @param length
     */
    public void setLength(int length) {
        this.length = length;
    }

    /**
     * 
     * @param idadeMax
     */
    public void setIdadeMax(long idadeMax) {
        this.idadeMax = idadeMax;
    }

    /**
     * 
     * @param body
     */
    public void setBody(byte[] body) {
        this.body = body;
    }

}
