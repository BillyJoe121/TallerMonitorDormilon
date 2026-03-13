import java.util.concurrent.Semaphore;
import java.util.Random;

public class MonitorDormilon {

    // Parámetros de la simulación
    private static final int NUM_SILLAS_CORREDOR = 3;
    private static final int NUM_ESTUDIANTES = 5; // Cantidad de estudiantes en la simulación

    // Contador de estudiantes esperando en las sillas del corredor
    private static int estudiantesEsperando = 0;

    // Semáforo para proteger la lectura/escritura de la variable
    // estudiantesEsperando
    private static final Semaphore mutexSillas = new Semaphore(1);

    // Semáforo con el que los estudiantes despiertan/avisan al monitor que llegaron
    private static final Semaphore semMonitor = new Semaphore(0);

    // Semáforo con el que el monitor avisa que está listo para atender.
    // Se inicializa con 'true' para que sea justo (fair) y se respete el FIFO
    // (orden de llegada).
    private static final Semaphore semEstudiante = new Semaphore(0, true);

    // Semáforo para que el estudiante espere hasta que el monitor termine de
    // ayudarle
    private static final Semaphore semAyudaTerminada = new Semaphore(0);

    public static void main(String[] args) {
        System.out.println("Iniciando la simulación del Monitor Dormilón...");
        System.out.println("Monitor, 3 sillas en el corredor y " + NUM_ESTUDIANTES + " estudiantes en total.\n");

        // Crear e iniciar el hilo del monitor
        Thread monitor = new Thread(new TareaMonitor());
        monitor.start();

        // Crear e iniciar los hilos de los estudiantes
        for (int i = 1; i <= NUM_ESTUDIANTES; i++) {
            Thread estudiante = new Thread(new TareaEstudiante(i));
            estudiante.start();
        }
    }

    // Clase que representa el comportamiento del Monitor
    static class TareaMonitor implements Runnable {
        private Random random = new Random();

        @Override
        public void run() {
            while (true) {
                try {
                    // Si no hay permisos pendientes en semMonitor, significa que no hay nadie
                    // esperando
                    boolean durmiendo = false;
                    long inicioDormir = 0;
                    if (semMonitor.availablePermits() == 0) {
                        System.out.println(
                                "[Monitor] No hay estudiantes esperando. El monitor se va a dormir una siesta... zZz");
                        durmiendo = true;
                        inicioDormir = System.currentTimeMillis();
                    }

                    // El monitor espera a ser despertado o a que haya estudiantes en la fila
                    semMonitor.acquire();
                    if (durmiendo) {
                        long tiempoDormido = System.currentTimeMillis() - inicioDormir;
                        System.out.println(
                                "[Monitor] Despertando. Tiempo que estuvo durmiendo: " + tiempoDormido + " ms");
                    }

                    // Accedemos de forma segura a las sillas
                    mutexSillas.acquire();
                    estudiantesEsperando--; // Un estudiante se levanta de la silla para entrar a la oficina

                    // El monitor le avisa al estudiante (en orden de llegada) que ya puede pasar
                    semEstudiante.release();
                    mutexSillas.release();

                    // Simula el tiempo que toma explicarle la duda al estudiante
                    System.out.println("[Monitor] Atendiendo a un estudiante en la oficina.");
                    int tiempoExplicando = (random.nextInt(3) + 1) * 1000;
                    Thread.sleep(tiempoExplicando); // Entre 1 y 3 segundos
                    System.out.println("[Monitor] Terminó de explicar la duda al estudiante. (Tiempo explicando: "
                            + tiempoExplicando + " ms)");

                    // Le avisa al estudiante específico que ya terminaron
                    semAyudaTerminada.release();

                } catch (InterruptedException e) {
                    System.err.println("El monitor fue interrumpido.");
                    e.printStackTrace();
                }
            }
        }
    }

    // Clase que representa el comportamiento de un Estudiante
    static class TareaEstudiante implements Runnable {
        private int id;
        private Random random = new Random();

        public TareaEstudiante(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    // El estudiante pasa tiempo programando en la sala
                    int tiempoProgramando = (random.nextInt(5) + 2) * 1000;
                    System.out.println("[Estudiante " + id + "] Programando en su tarea de algoritmos...");
                    Thread.sleep(tiempoProgramando); // Entre 2 y 6 segundos
                    System.out.println("[Estudiante " + id + "] Terminó de programar. (Tiempo programando: "
                            + tiempoProgramando + " ms)");

                    System.out.println("[Estudiante " + id + "] Tiene una duda y va a la oficina del monitor.");

                    // Intenta revisar las sillas disponibles en el corredor
                    mutexSillas.acquire();

                    if (estudiantesEsperando < NUM_SILLAS_CORREDOR) {
                        // Hay al menos una silla disponible
                        estudiantesEsperando++;
                        System.out.println("[Estudiante " + id
                                + "] Toma un asiento en el corredor. Estudiantes esperando: " + estudiantesEsperando);

                        // Le avisa al monitor que requiere ayuda (esto lo despierta si estaba
                        // durmiendo)
                        semMonitor.release();
                        mutexSillas.release(); // Libera las sillas para que otros puedan revisar

                        // Espera a que el monitor esté listo para atenderlo
                        long inicioEspera = System.currentTimeMillis();
                        semEstudiante.acquire();
                        long tiempoEspera = System.currentTimeMillis() - inicioEspera;
                        System.out.println("[Estudiante " + id
                                + "] Entra a la oficina y está recibiendo ayuda del monitor. (Tiempo esperando en silla: "
                                + tiempoEspera + " ms)");

                        // Se queda escuchando hasta que el monitor termine la explicación
                        long inicioAyuda = System.currentTimeMillis();
                        semAyudaTerminada.acquire();
                        long tiempoAyuda = System.currentTimeMillis() - inicioAyuda;
                        System.out.println("[Estudiante " + id
                                + "] Duda resuelta. Vuelve a la sala a programar. (Tiempo recibiendo ayuda: "
                                + tiempoAyuda + " ms)");

                    } else {
                        // Las 3 sillas están ocupadas
                        System.out.println("[Estudiante " + id
                                + "] Ve que las sillas están llenas. Regresa a programar y volverá más tarde.");
                        mutexSillas.release(); // Libera las sillas sin sentarse
                    }

                } catch (InterruptedException e) {
                    System.err.println("El estudiante " + id + " fue interrumpido.");
                    e.printStackTrace();
                }
            }
        }
    }
}
