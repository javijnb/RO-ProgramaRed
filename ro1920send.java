import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Calendar;
import java.io.File;
import java.io.FileInputStream;
import java.net.DatagramPacket;


//SINTAXIS:  ro1920send   [input_file   dest_IP   dest_port   emulator_IP   emulator_port}


//ERRORES:    
// 1) No hay respuesta del servidor IP_Servidor:puerto tras timeout segundos
// 2) Fichero inexistente
// 3) Invocacion del programa incorrecta
// 4) Cualquier excepcion contemplada en el bloque try catch


public class ro1920send{

public static void main(String[] args) {
 

    //COMPROBAMOS QUE SE HA INVOCADO BIEN AL CLIENTE
    if(args.length!=5){
        System.out.println("No ha introducido un formato válido de parámetros en el comando");
        System.out.println("Cerrando cliente");
        System.exit(0);
    }


    //LEEMOS LOS PARAMETROS DE ENTRADA
    String input_file = args[0];
    String ipDestino  = args[1];
    short puertoDestino = Short.parseShort(args[2]);
    String ipEmulador = args[3];
    int puertoEmulador= Integer.parseInt(args[4]);


    // VARIABLES
    final int TIMEOUT = 200;
    final int PAYLOAD_TOTAL = 1472; // 4 + 2 + 1 + 1 + 2 + 1463
    final int PAYLOAD_UTIL = 1462;
    String FICHERO_A_ENVIAR = input_file;
    File FicheroCliente = new File(FICHERO_A_ENVIAR);

        if((FicheroCliente.exists())==false){
            System.out.println("No existe fichero con nombre y extension: "+input_file);
            System.exit(0);
        }

    int paquetesTotales = (int) Math.ceil(FicheroCliente.length()/(double)PAYLOAD_UTIL);


    // BUFFERS - 1465bytes de payload de envio
    byte[] bufferLectura = new byte[PAYLOAD_UTIL];
    byte[] bufferEnvio   = new byte[PAYLOAD_TOTAL];
    byte[] bufferRecepcion = new byte[7];
    byte   bufferACK = 0;

    //[IP_DESTINO] + [PUERTO DESTINO] + [MORE_FRAGMENTS] + [ID_PAQUETE] [TAMANO] + [PAYLOAD] = 4 + 2 + 1 + 1 + 2 + 1462 = 1472bytes
    
    // INFO EN BYTES - 10 bytes en total de cabeceras extra
    byte[] headerIp      = new byte[4];
    byte[] headerPort    = new byte[2];
    byte   moreFragments = 1;
    byte   secuencia     = 0;
    byte[] tamano        = new byte[2];


    long inicio = Calendar.getInstance().getTimeInMillis();

    // -------------------------------------------------------------------------------------------------------------------------
    // FUNCIONALIDADES DE LA APLICACION EMISORA
    try {
        System.out.println("Iniciando la aplicacion emisora...");
        System.out.println("Instante de inicio:  "+inicio);
        System.out.println("Timeout establecido: "+TIMEOUT+" ms");


        //CREAMOS EL SOCKET + INFORMACION DEL ENLACE
        DatagramSocket socketUDP = new DatagramSocket(12345);
        socketUDP.setSoTimeout(TIMEOUT);
        System.out.println("\n\n\n\nINFORMACION DE SOCKET:");
        System.out.println("Socket creado en puerto "+socketUDP.getLocalPort());
        System.out.println("Shufflerouter:        "+ipEmulador+":"+puertoEmulador);
        System.out.println("Destino (receptor):   "+ipDestino +":"+puertoDestino);


        System.out.println("\n\n\nINFORMACION DE ENVIO:");
        System.out.println("Nombre del fichero a ser transmitido: "+FICHERO_A_ENVIAR);
        System.out.println("Tamano de fichero: "+FicheroCliente.length()+" bytes");
        System.out.println("Numero de paquetes a ser enviados: "+paquetesTotales+" ----> [0 - "+(paquetesTotales-1)+"]");

 
        // ----------------------------------------------------------------------------------------
        // PREPARAMOS EL ENVIO

        System.out.println("\n\n\nComenzamos el envio...");
        InetAddress dirIpEmulador = InetAddress.getByName(ipEmulador);
 
        // PREPARAMOS LA INFORMACION DE NUESTRAS CABECERAS FIJAS: DIR IP + PUERTO
        headerIp = (InetAddress.getByName(ipDestino)).getAddress();
        headerPort = short2ByteArray(puertoDestino);

        FileInputStream inputFichero = new FileInputStream(FicheroCliente);
        short a = 0;
        int ultimoPaqueteEnviado = 0;





        // MIENTRAS HAYA FICHERO POR LEER, IMPLEMENTAMOS PARADA Y ESPERA, esto es enviar y tratar los ACK recibidos
        while((a = (short)inputFichero.read(bufferLectura)) != -1){

            // Mientras el ACK recibido no es correcto (ACK=0) o simplemente no llega (expira el timeout)
            // se reenvia el paquete previo
            while(bufferACK==0){





            //    E N V I O

            //ESTAMOS REALIZANDO LA TRANSMISION DEL ULTIMO PAQUETE:
            if(ultimoPaqueteEnviado==(paquetesTotales-1)){
                moreFragments = 0;
                //System.out.println("\nHemos desactivado el byte MoreFragments");
            }

            ByteBuffer BbufferEnvio = ByteBuffer.wrap(bufferEnvio);
                BbufferEnvio.put(headerIp);
                BbufferEnvio.put(headerPort);
                BbufferEnvio.put(moreFragments);
                BbufferEnvio.put(secuencia);
                    tamano = short2ByteArray(a);
                BbufferEnvio.put(tamano);
                BbufferEnvio.put(bufferLectura);
            
            DatagramPacket paqCliente = new DatagramPacket(bufferEnvio, BbufferEnvio.capacity(), dirIpEmulador, puertoEmulador);
            System.out.println("\nEnviamos paquete con identificador: "+ultimoPaqueteEnviado+"(con secuencia "+secuencia+")");
            socketUDP.send(paqCliente);

            



            // R E C E P C I O N      D E      A C K 
            //cada vez que recibamos un ack valido cambiamos el numero de secuencia (0 -> 1  || 1 -> 0)

            try{

                DatagramPacket ACK = new DatagramPacket(bufferRecepcion, bufferRecepcion.length);
                socketUDP.receive(ACK);
                byte[] bufferaux = ACK.getData();
                bufferACK = bufferaux[6];    
                System.out.println("ACK recibido: "+bufferACK);


                //NO HEMOS RECIBIDO UN ACK
                if (bufferACK != 1){
                     System.out.println("No hemos recibido un ACK valido, reenviando paquete previo (con secuencia "+secuencia+")");
    

                //HEMOS RECIBIDO UN ACK     
                }else{ 

                    //Al recibir ACK actualizamos secuencia
                    if(secuencia==1){
                        secuencia=0;
                    }else if(secuencia==0){
                        secuencia=1;
                    }

                    //HEMOS RECIBIDO EL ULTIMO ACK
                    if(ultimoPaqueteEnviado==(paquetesTotales-1)){
                        System.out.println("Este era el ultimo ACK, finalizando la transmision...\n");
                        break;
                    }
    
                }

            } catch(SocketTimeoutException ex){
                System.out.println("Ha expirado el tiempo de espera por el siguiente ACK. Reenviando paquete anterior ("+ultimoPaqueteEnviado+") y secuencia "+secuencia);
           
            } catch(Exception ex){
                System.out.println("Excepcion no contemplada en la recepcion del ACK");
            }
        
            }//FIN DEL WHILE DE ESPERA POR EL ACK CORRECTO





            //Si estamos en el ultimo ACK recibido no hace falta sacar el mensaje de preparando el siguiente paquete
            if(ultimoPaqueteEnviado!=(paquetesTotales-1)){

                System.out.println("Preparando siguiente paquete...\n");
                bufferACK=0; //reiniciamos el valor del ACK recibido (0 por defecto)
                ultimoPaqueteEnviado++;

            }

            


        }// FIN DEL WHILE DE LECTURA DEL FICHERO





        // ---------------------------------------------------------------------
        // FIN DE LA TRANSMISION
        //System.out.println("Hemos salido del while de lectura del fichero");
        socketUDP.close();
        System.out.println("Fin de la transmision del fichero.");
    




    //EXCEPCIONES
    } catch(UnknownHostException ex){
        System.out.println("Error conviertiendo una direccion IP a InetAddress");
        ex.printStackTrace();

    } catch(NumberFormatException ex){
        ex.printStackTrace();
        
    // TIMEOUT EXCEDIDO
    } catch (SocketTimeoutException ex) {
        System.out.println("No se ha obtenido respuesta de la aplicacion emisora en "+TIMEOUT/1000+ " segundo(s).");
        System.out.println("Cerrando conexion de forma abrupta");
        System.exit(-1);

    } catch (Exception ex){
        System.out.println("Excepcion no contemplada");
        ex.printStackTrace();
    }
    
    //System.out.print("Hemos salido del try catch general. ");
    long final2 = Calendar.getInstance().getTimeInMillis();
    System.out.println("Tiempo resultante: "+((final2-inicio)/1000)+" segundos");
    System.out.println("Finalizando ejecucion de la aplicacion emisora");
   
}


//METODOS DE CONVERSION DE TIPO
public static byte[] int2ByteArray(int numero){
    ByteBuffer bufferaux = ByteBuffer.allocate(4);
    bufferaux.putInt(numero);
    return bufferaux.array();
}

public static int byteArray2Int(byte[] array){
    ByteBuffer bufferaux = ByteBuffer.wrap(array);
    return bufferaux.getInt();
}

public static byte[] short2ByteArray(short numero){
    ByteBuffer bufferaux = ByteBuffer.allocate(2);
    bufferaux.putShort(numero);
    return bufferaux.array();
}

}