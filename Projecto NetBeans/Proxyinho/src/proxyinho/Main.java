
package proxyinho;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Class que inicia o servidor proxy
 * 
 * @author José Ribeiro e Luis Vieira
 *
 */
public class Main {

    private static final int port=6069;
    
    /**
     * Método main, que inicia o servidor e trata dos pedidos dos clientes
     * atribuindo uma thread para o seu pedido
     * 
     */
    public static void main(String[] args) throws IOException{

        Cache cache = new Cache();
        ServerSocket servidor=null;
        Socket cliente;

        try{
            servidor = new ServerSocket(port);
        }catch(IOException iox){
            System.out.println("Nao foi possivel iniciar o servidor: "+iox.getMessage());
            System.exit(0);
        }
        
        while(true){

            cliente = servidor.accept();
    
            (new ClienteHandler(cache, cliente)).start();
            
        
        }
        
    }
    
}

