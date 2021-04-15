# RO-ProgramaRed
Programa de red - Redes de ordenadores 2020 - 2021
Implementación de un algoritmo de retransmisión (ARQ) en Java empleando sockets UDP
Dos programas, ro1920send y ro1920recv se comunican a través de un ejecutable `shufflerouter` que simula un nodo de la red que altera el orden de los paquetes que le llegan y descarta algunos de ellos con cierta probabilidad para probar la robustez del algoritmo implementado.
## Invocación
ro1920recv   output_file   listen_port
ro1920send   input_file   dest_IP   dest_port   emulator_IP   emulator_port
