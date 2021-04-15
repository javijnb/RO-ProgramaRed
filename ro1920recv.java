import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.util.*;


// SINTAXIS:  ro1920recv   output_file   listen_port

// ERRORES:   
// 1) Comprobar que se invoque bien 
// 2) Introducir un fichero valido (nombre.extension) y existente

public class ro1920recv {
 
    public static void main(String[] args) {
        

        // PARAMETROS DE ENTRADA
        String archivoSalida = args[0];
        int puertoEscucha = Integer.parseInt(args[1]);


        //COMPROBAMOS QUE SE INVOCÓ BIEN EL SERVIDOR
        if(args.length!=2){
            System.out.println("No ha introducido un formato válido de parámetros en el comando");
            System.out.println("Finalizando ejecucion de forma abrupta de la aplicacion receptora...");
            System.exit(-1);
        }
        
        if(!archivoSalida.contains(".")){
            System.out.println("No ha introducido un nombre de fichero y su extension. ");
            System.out.println("Finalizando ejecucion de forma abrupta de la aplicacion receptora...");
            System.exit(-1);
        }



        // VARIABLES
        final int PAYLOAD_TOTAL = 1472; // = 4 + 2 + 1 + 1 + 2 + 1463
        final int PAYLOAD_UTIL = 1462;
 
        final int TIMEOUT = 200;


        // BUFFERS
        byte[] bufferRecepcion = new byte[PAYLOAD_TOTAL];
        byte[] bufferEscritura = new byte[PAYLOAD_UTIL];
        byte[] bufferRespuesta = new byte[7];
        byte bufferACK = 0;
        

        // INFO ENCAPSULADA
        byte[] headerIp = new byte[4];
        byte[] headerPort = new byte[2];
        byte   moreFragments;
        byte   secuencia;
        byte[] tamano = new byte[2];

        short headerPort_short;

        InetAddress ipShufflerouter, ipReceptor;
        int puertoShufflerouter;


        // ------------------------------------------------------------------------------------------------------------
        // FUNCIONALIDADES DE LA APLICACION RECEPTORA:
        try {

            System.out.println("Iniciando la aplicacion receptora...");
            System.out.println("Instante de inicio:  "+Calendar.getInstance().getTimeInMillis());

            File fileSalida = new File(archivoSalida);
            FileOutputStream fos = new FileOutputStream(fileSalida, false);

            //Creacion del socket
            DatagramSocket socketUDP = new DatagramSocket(puertoEscucha);
            socketUDP.setSoTimeout(TIMEOUT);
            System.out.println("\n\nINFORMACION DE SOCKET:");
            System.out.println("Socket creado en puerto: "+puertoEscucha);
            System.out.println("Nombre del fichero a recibir: "+archivoSalida);
            System.out.println("Timeout establecido: "+TIMEOUT+" ms");



            //Siempre recibiendo peticiones
            int currentPacket = 0;
            int nextPacket = 1;
            byte secuencia_anterior=1;

            while (true) {
                    
                // -------------------------------------------------------------
                // RECEPCION DE LA INFORMACION DEL FICHERO
                // [IP_DESTINO] + [PUERTO DESTINO] + [MORE_FRAGMENTS] + [ID_PAQUETE] +[TAMANO] + [PAYLOAD] = 4 + 2 + 1 + 1 + 2 + 1462 = 1472bytes

                try{

                    DatagramPacket packetFromSender = new DatagramPacket(bufferRecepcion, bufferRecepcion.length);
                    socketUDP.receive(packetFromSender);
                    System.out.println("\nInformacion de la aplicacion emisora recibida");

                    ipShufflerouter = packetFromSender.getAddress();
                    puertoShufflerouter = packetFromSender.getPort();
                    byte[] bufferPaquete = packetFromSender.getData();

                        headerIp = Arrays.copyOfRange(bufferPaquete, 0, 4);
                        headerPort = Arrays.copyOfRange(bufferPaquete, 4, 6);
                        moreFragments = bufferPaquete[6];
                        secuencia = bufferPaquete[7];
                        tamano = Arrays.copyOfRange(bufferPaquete, 8, 10);
                            short offset = byteArray2Short(tamano);
                        bufferEscritura = Arrays.copyOfRange(bufferPaquete, 10, (10+offset));

                    ipReceptor = InetAddress.getByAddress(headerIp);
                    headerPort_short = byteArray2Short(headerPort);

                    /*
                    System.out.println("Mas fragmentos:       "+moreFragments);
                    System.out.println("Tamano:               "+offset);
                    System.out.println("ID secuencia:         "+secuencia);
                    */


                    //Si recibimos el siguiente numero de secuencia, escribimos
                    //Solo se actualiza el numero de secuencia si es distinto al anterior (es el esperado)

                    if(secuencia!=secuencia_anterior){
                        fos.write(bufferEscritura);
                    }

                    secuencia_anterior=secuencia;


                    // -------------------------------------------------------------
                    // ENVIO DE ACKs
                    // [IP DESTINO] + [PUERTO DESTINO] + [ACK] = 4 + 2 + 1 = 7 bytes

                    bufferACK = 1;

                    ByteBuffer BbufferEnvio = ByteBuffer.wrap(bufferRespuesta);
                        BbufferEnvio.put(headerIp);
                        BbufferEnvio.put(headerPort);
                        BbufferEnvio.put(bufferACK);

                    DatagramPacket ACK = new DatagramPacket(bufferRespuesta, BbufferEnvio.capacity(), ipShufflerouter, puertoShufflerouter);
                    socketUDP.send(ACK);
                    System.out.print("ACK enviado ("+bufferACK+"). ");


                    if(moreFragments==0){
                        System.out.println("El ultimo paquete recibido era el definitivo, cerrando aplicacion receptora...");
                        fos.close();
                        socketUDP.close();
                        System.exit(0);
                    } else{
                        System.out.println("Pidiendo proximo paquete ("+nextPacket+")");
                    }


                    //Limpiamos buffer de recepcion
                    Arrays.fill(bufferRespuesta, (byte)0);
                    
                    if(/*paquete anterior != paquete actual*/ true){
                        nextPacket++;
                        currentPacket++;
                    }




                    } catch(SocketTimeoutException ex){
                        System.out.println("\nNo se ha recibido el siguiente paquete ("+currentPacket+") de la aplicacion emisora en "+TIMEOUT/1000+" segundo(s)");
                        System.out.println("Solicitando paquete...");
                    }
                    
                
            
            
            }//FIN DEL WHILE





        }catch(SocketTimeoutException ex){
            System.out.println("\nNo ha habido respuesta de la aplicacion emisora en "+TIMEOUT/1000+" segundo(s)");
            System.out.println("Finalizando ejecucion de forma abrupta de la aplicacion receptora...");
            System.exit(-1);
        
        }catch(SocketException ex){


        }catch(Exception e){
            e.printStackTrace();
        }

        System.out.println("He salido el try catch general, finalizando la ejecucion...");
        System.exit(0);
    }




    //METODOS DE CONVERSION DE TIPOS

    public static byte[] int2ByteArray(int numero){
        ByteBuffer bufferaux = ByteBuffer.allocate(4);
        bufferaux.putInt(numero);
        return bufferaux.array();
    }

    public static int byteArray2Int(byte[] array){
        ByteBuffer bufferaux = ByteBuffer.wrap(array);
        return bufferaux.getInt();
    }

    public static short byteArray2Short(byte[] array){
        ByteBuffer bufferaux = ByteBuffer.wrap(array);
        return bufferaux.getShort();
    }

    public static long byteArray2Long(byte[] array){
        ByteBuffer bufferaux = ByteBuffer.wrap(array);
        return bufferaux.getLong();
    }

    public static byte[] long2ByteArray(long numero){
        ByteBuffer bufferaux = ByteBuffer.allocate(8);
        bufferaux.putLong(numero);
        return bufferaux.array();
    }
}