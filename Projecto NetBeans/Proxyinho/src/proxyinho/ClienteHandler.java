
package proxyinho;

import java.io.*;
import java.net.Socket;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * 
 * Class que contem a thread com o handler de cada cliente
 * 
 * @author José Ribeiro e Luis Vieira
 *  
 */
public class ClienteHandler extends Thread{

    private Cache cache;
    private Socket cliente;

    private static final String novaLinha = "\r\n";

    /* esta concorrência pode ter alguns problemas se forem muitos pedidos ao mesmo tempo fazendo com que
     cada thread o heap space possa dar exceptions*/
    private static final int bufferSize = 2097152; // quase 1 megabyte
    private static final int bodySize = 20971520;  // 20mb
    
    /**
     * 
     * Constructor que instancia o handler com as referencias da cache e do
     * socket do cliente
     * 
     * @param cache
     * @param cliente
     */
    public ClienteHandler(Cache cache, Socket cliente){
        this.cache=cache;
        this.cliente=cliente;
    }
    
    @Override
    public void run(){
        
        BufferedReader in;
        DataOutputStream out;
        String pedido, metodo, uri, versao, header;
        PrintWriter logs,historico;
        String[] params;
        boolean pedidoSemCache;
        
        DataInputStream sitein;
        BufferedWriter siteout;
        
        boolean temStatus;
        GregorianCalendar dataExpira;
        GregorianCalendar dataUltimaModificacao;
        String etag;
        boolean respostaSemCache;
        long maxAge;
        
        Socket remoto;
        
        try{

            logs = new PrintWriter(new FileWriter("logs.txt", true));
            historico = new PrintWriter(new FileWriter("historico.txt",true));

            in = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
            out = new DataOutputStream(cliente.getOutputStream());

            pedido = in.readLine();

            params = pedido.split(" ");
            metodo = params[0];
            uri = params[1];
            versao = params[2];
            String tmp;
            header="";
            pedidoSemCache=false;

            while ( (tmp=in.readLine()).length() != 0) {

                /* Verifica se o pedido foi efectuado de modo a não consultar a cache */
                if(tmp.toUpperCase().equals("Cache-Control: no-cache".toUpperCase()) ||
                    tmp.toUpperCase().equals("Pragma: no-cache".toUpperCase())){
                        pedidoSemCache = true;
                }

                /* Constroi o header com os dados recebidos*/
                header += tmp + novaLinha;
            }

            /* Apenas estamos a tratar pedidos com GET */
            if(metodo.equals("GET")){

                /* Acede ao servidor remoto e abre uma ligação com o mesmo na porta 80
                 * utilizada para pedidos HTTP e cria os Data Streams para a comunicação com o mesmo
                 */
                 
                remoto = new Socket(parseURIparaDominio(uri),80);

                sitein = new DataInputStream(remoto.getInputStream());
                siteout = new BufferedWriter(new OutputStreamWriter(remoto.getOutputStream()));

                /* Cria o request para o servidor remoto e envia */
                String req = criaRequest(metodo, parseURIparaCaminho(uri), versao, header);

                siteout.write(req);
                siteout.flush();
                
                temStatus=false;
                dataExpira=null;
                dataUltimaModificacao=null;
                etag="";
                respostaSemCache=false;
                maxAge=0;

                String status="";
                String headerResponse="";
                int length=(-1);
                int iaux=0;

                /* Trata os dados recebidos pelo servidor remoto */
                String line = sitein.readLine();
                while (line.length() != 0) {
                    
                    //System.out.println(line);
                    
                    /* Verifica se o status já foi recebido */
                    if (!temStatus) {
                        status = line;
                        temStatus = true;
                    } else {
                        /* Cria o header response */
                        headerResponse += line + novaLinha;
                    }

                    /* Se encontrar no header o content-length guarda-o */
                    if (line.startsWith("Content-Length") ||
                        line.startsWith("Content-length")) {
                        String[] tmp3 = line.split(" ");
                        length = Integer.parseInt(tmp3[1]);
                    }

                    /* Caso a resposta diga que o objecto retornado é para ser
                     * apresentado sem utilizar a nossa cache
                     */
                    if(line.toUpperCase().contains("no-cache".toUpperCase())){
                            respostaSemCache = true;
                    }

                    /* Verifica qual a data de expiração do objecto */
                    if (line.toUpperCase().startsWith("EXPIRES")) {
                        iaux = line.indexOf("GMT")+3;
                        if(iaux-3 >=0){
                            String expira = line.substring(9,iaux);
                            //System.out.println(expira);
                            dataExpira = strToGC(expira);
                        }
                    }

                    /* Verifica qual a data de ultima modificação do objecto */
                    if (line.toUpperCase().startsWith("LAST-MODIFIED")) {
                        iaux = line.indexOf("GMT")+3;
                        if(iaux-3 >=0){
                            String expira = line.substring(15,iaux);
                            //System.out.println(expira);
                            dataUltimaModificacao = strToGC(expira);
                        }
                    }

                    /* Verifica qual a etag do objecto */
                    if (line.toUpperCase().startsWith("ETAG")) {
                        etag = line.substring(7,line.length()-1);
                    }

                    /* Verifica qual a idade maxima do objecto na cache */
                    if(line.toUpperCase().contains("MAX_AGE")){

                        String saux = line.substring(line.toUpperCase().indexOf("MAX_AGE")+8);

                        iaux = saux.indexOf(",");

                        if(iaux>=0){
                            maxAge=Long.parseLong(saux.substring(0,iaux));
                        }else{
                            maxAge=Long.parseLong(saux);
                        }

                    }

                    line = sitein.readLine();

                }

                /* Cria o header response a enviar para o cliente */
                String enviarHeader = criaHeaderResponse(status, headerResponse);

                /* Verifica se a uri do objecto acedido já existe em cache */
                boolean existeEmCache = cache.existeEmCache(uri);

                Objecto ele=null;

                /* Caso exista em cache, vai buscar a sua referencia */
                if(existeEmCache){
                    ele = cache.getObjecto(uri);
                }

                /* Condições necessárias para que o objecto guardado em cache seja apresentado:
                 * 
                 *  - O pedido não foi efectuado pedindo para não utilizar a cache
                 *  - A resposta não foi efectuada pedindo para não utilizar a cache
                 *  - O objecto existe em cache
                 *  
                 *  Com base nos dados recebidos pelo header do servidor remoto, que contem
                 *  informações sobre o objecto existente, comparamos essas informações com
                 *  o nosso objecto em cache, o qual tem de cumprir que:
                 * 
                 *  - O Objecto não foi modificado desde que foi adicionado à cache
                 *  - A Etag não é diferente
                 *  - A idade maxima que o objecto pode permanecer na cache não foi ultrapassada
                 *  - O Objecto não está expirado com base na data actual
                 *  - A Length do objecto não é diferente (não fiável, mas caso falhe, garante que algo é diferente)
                 */
                if(
                        !pedidoSemCache && 
                        !respostaSemCache && 
                        existeEmCache && 
                        !ele.isModificado(dataUltimaModificacao) && 
                        !ele.isEtagDiferente(etag) && 
                        !ele.idadeMaxUltrapassada() && 
                        !ele.isExpirado() && 
                        !ele.isLengthDiferente(length)

                    ){

                    System.out.println("Foi a cache para o URI: "+uri);

                    /* Envia o header */
                    out.writeBytes(enviarHeader);

                    /* Envia o elemento guardado em cache */
                    out.write(ele.getBody(), 0, ele.getLength());

                    out.flush();

                    out.close();

                }else{
                    
                    /* Se um dos parametros em cima indicados falhar, lê o body do servidor remoto */

                    boolean loop = true;
                    int conta = 0;

                    /* Se não tiver a length, lemos tudo o que vier */
                    if(length==(-1)){
                        loop = true;
                    }

                    int total = 0;
                    byte buf[] = new byte[bufferSize];
                    byte body[] = new byte[bodySize];

                    while (total < length || loop) {

                        if((conta=sitein.read(buf,0,bufferSize))==(-1)){
                            break;
                        }

                        for (int i = 0; i < conta && (i + conta) < bodySize; i++) {
                        /* Lê o body byte a byte */
                            body[total+i] = buf[i];
                        }

                        total += conta;

                    }
                    
                    System.out.println("Nao foi a cache para o URI: " + uri);

                    /* Envia o header */
                    out.writeBytes(enviarHeader);
                    
                    /* Envia o body lido pelo servidor remoto */
                    out.write(body, 0, total);

                    out.flush();

                    out.close();

                    /* Adiciona à cache o objecto lido */

                    byte baux[] = new byte[total];

                    System.arraycopy(body, 0, baux, 0, total);

                    ele = new Objecto(etag, dataExpira, dataUltimaModificacao, total, maxAge, baux);
                    cache.addObjecto(uri, ele); // Se existe, substitui
                    cache.saveCache();

                }

                sitein.close();
                siteout.close();

                remoto.close();

                historico.append((new Date()).toString() + ": " + uri);

                historico.close();

            }else{
                //System.out.println("ERRO");
                logs.append((new Date()).toString() + ": "
                        +"Não suportamos GET: O Proxyinho bloqueou um acesso com o método " + metodo + " proveniente de " + uri+"\r\n");
                logs.flush();
            }

            /* Termina a ligação com o cliente */
            
            in.close();            
            out.close();            

            cliente.shutdownInput();
            cliente.shutdownOutput();
            cliente.close();

            logs.close();
        
        }catch(Exception ex){

            /* Guarda os erros no ficheiro logs.txt */
            
            try {
                logs = new PrintWriter(new FileWriter("logs.txt", true));
                logs.append((new Date()).toString() + ": " + ex.getMessage());
                logs.flush();
                logs.close();
            } catch (IOException ex1) {}
        
        }

    }
    
    /**
     * 
     * Função que retorna o dominio de um dado uri passado como argumento
     * 
     * @param uri
     * @return dominio
     */
    private String parseURIparaDominio(String uri){
    
        if(uri.startsWith("http://")){
            uri = uri.substring(7);
        }

        if(uri.contains("/")){
            uri = uri.substring(0, uri.indexOf("/"));
        }

        return uri;
        
    }
    
    /**
     * 
     * Função que retorna o path de um objecto dentro do dominio
     * 
     * @param uri
     * @return caminho
     */
    private String parseURIparaCaminho(String uri){
    
        String dom = parseURIparaDominio(uri);
        
        int pos = uri.indexOf(dom) + dom.length();

        return uri.substring(pos);
        
    }
    
    /**
     * 
     * Cria o request a ser enviado para o servidor remoto
     * 
     * @param metodo 
     * @param uri
     * @param versao 
     * @param headers 
     * @return request
     */
    private String criaRequest(String metodo, String uri, String versao, String headers){
    
	String req = metodo + " " + uri + " " + versao + novaLinha;
	req += headers;
	/* Não permite ligações persistentes */
	req += "Connection: close" + novaLinha;
	req += novaLinha;
	
	return req;
    
    }
    
    /**
     * 
     * Função que cria o header response a ser enviado
     * 
     * @param status 
     * @param headers 
     * @return header
     */    
    private String criaHeaderResponse(String status, String headers){

        String res = status + novaLinha;
	res += headers;
	res += novaLinha;
	
	return res;
    
    }

    /**
     * 
     * Função que converte uma data no formato: EEE, dd MMM yyyy HH:mm:ss z utilizado pelos headers do HTTP
     * num objecto GregorianCalendar
     * 
     * @param data 
     * @return calendario
     */
    private GregorianCalendar strToGC(String data) throws ParseException{

        DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.UK);
        Date parsed = df.parse(data);

        GregorianCalendar g = new GregorianCalendar();
        g.setTime(parsed);

        return g;
        
    }

}
