import com.google.gson.Gson;

import java.io.File;
import java.util.*;

public class Peer {
    String peerIP;
    int peerPort;
    List<String> peerFiles;
    String peerFolder;
    String[] knownIP = new String[2];
    int[] knownPort = new int[2];

    private static final boolean debug = true;

    /**
     * The constructor for Peer X.
     *
     * @param peerIP The Peer X IP.
     * @param peerPort The Peer X Port.
     * @param peerFolder The Peer X folder path.
     * @param neighborAIP The first known neighbor IP.
     * @param neighborAPort The first known neighbor Port.
     * @param neighborBIP The second known neighbor IP.
     * @param neighborBPort The second known neighbor Port.
     */
    public Peer(String peerIP, int peerPort, String peerFolder, String neighborAIP, int neighborAPort,
                String neighborBIP, int neighborBPort) {
        this.peerIP = peerIP;
        this.peerPort = peerPort;
        this.peerFolder = peerFolder;
        this.peerFiles = listFiles();
        this.knownIP[0] = neighborAIP;
        this.knownPort[0] = neighborAPort;
        this.knownIP[1] = neighborBIP;
        this.knownPort[1] = neighborBPort;
    }

    /**
     * Go to the peer folder and see what is inside.
     *
     * @return a String List with the files on the peer folder.
     */
    public List<String> listFiles(){
        File fileDir = new File(peerFolder);
        String[] files = fileDir.list();

        List<String> filesList = new ArrayList<>();
        if (files == null) throw new AssertionError();
        Collections.addAll(filesList, files);

        return filesList;
    }

    /**
     * Print the list of Peer X files.
     *
     * @return a String to print.
     */
    public String printListOfFiles(List<String> l) {
        StringBuilder toPrint = new StringBuilder();
        for (String list : l) {
            toPrint.append(list);
            toPrint.append(" ");
        }

        return toPrint.toString();
    }

    /**
     * Get the new files on the Peer X folder.
     *
     * @param newList is the new check of folder contents.
     *
     * @return only the new files.
     */
    public List<String> hasNewFiles(List<String> newList) {
        List<String> newFiles = new ArrayList<>(newList);
        newFiles.removeAll(this.peerFiles);

        return newFiles;
    }

    /**
     * Get the removed files of the Peer X folder.
     *
     * @param newList is the new check of folder contents.
     *
     * @return only the removed files.
     */
    public List<String> hasRemovedFiles(List<String> newList) {
        List<String> removedFiles = new ArrayList<>(this.peerFiles);
        removedFiles.removeAll(newList);

        return removedFiles;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean run = true;
        Peer p1;
        PeriodicTask t1 = null;

        while(run) {
            /* --------------------------------------------------
            Menu interativo (por console) que permita realizar a escolha somente das
            funções INICIALIZA e SEARCH.
            */
            if (debug) System.out.println("Choose an option: ");
            if (debug) System.out.println("INICIALIZA");
            if (debug) System.out.println("SEARCH");
            String option = scanner.nextLine();

            if (option.equalsIgnoreCase("inicializa")) {
                /* --------------------------------------------------
                b)	Inicialização: captura do teclado o IP e porta do peer X, a pasta onde estão localizados
                    seus arquivos, e o IP e porta de outros dois peers.
                -------------------------------------------------- */
                if (debug) System.out.println("Welcome new peer!");
                if (debug) System.out.print("Peer X IP address: ");
                String peerXIP = scanner.nextLine();
                if (debug) System.out.print("Peer X Port: ");
                int peerXPort = scanner.nextInt();
                scanner.nextLine();
                if (debug) System.out.print("Peer X folder path: ");
                String peerXFolder = scanner.nextLine();
                if (debug) System.out.print("Neighbor A IP address: ");
                String neighborAIP = scanner.nextLine();
                if (debug) System.out.print("Neighbor A Port: ");
                int neighborAPort = scanner.nextInt();
                scanner.nextLine();
                if (debug) System.out.print("Neighbor B IP address: ");
                String neighborBIP = scanner.nextLine();
                if (debug) System.out.print("Neighbor B Port: ");
                int neighborBPort = scanner.nextInt();
                scanner.nextLine();
                if (debug) System.out.println();

                p1 = new Peer(peerXIP, peerXPort, peerXFolder, neighborAIP, neighborAPort, neighborBIP, neighborBPort);

                System.out.print("arquivos da pasta: ");
                System.out.println(p1.printListOfFiles(p1.peerFiles));
                /* --------------------------------------------------
                c)	Monitoramento da pasta: cada 30 segundos o peer verificará se na pasta (capturada na
                    inicialização) houveram modificações, ou seja se foram inseridos ou removidos arquivos.
                    A lista de arquivos deverá estar armazenada em alguma estrutura na memória, por exemplo,
                    uma lista ou um hash.
                -------------------------------------------------- */
                t1 = new PeriodicTask(30, p1);
                t1.start(p1);
            }
            if (option.equalsIgnoreCase("search")) {
                if (debug) System.out.print("Name of the file: ");
                String searchFile = scanner.nextLine();



            }
            if (option.equalsIgnoreCase("exit")) {
                if (debug) System.out.println("Bye!");
                if (t1 != null) {
                    t1.stop();
                }
                run = false;
            }
        }



        /*
        // Generate JSON from object Peer p1:
        Gson gson = new Gson();
        String json = gson.toJson(p1);
        System.out.println(json);
        */


    }
}

/**
 * Class PeriodicTask is used for periodic actions with timer threads.
 */
class PeriodicTask extends TimerTask {
    private final int period;
    Timer timer;
    Peer p;
    private static final boolean debug = false;

    /**
     * The constructor to PeriodicTask.
     *
     * @param period is the period in seconds.
     */
    public PeriodicTask(int period, Peer p) {
        this.period = period;
        this.p = p;
        this.timer = new Timer(true);
    }

    /**
     * Start the periodic task.
     */
    public void start(Peer p) {
        TimerTask timerTask = new PeriodicTask(period, p);
        timer.scheduleAtFixedRate(timerTask, 0, (long)period*1000);
        if(debug) System.out.println("Started the periodic check for new files!");
    }

    /**
     * Stop the periodic task.
     */
    public void stop(){
        timer.cancel();
    }

    @Override
    public void run() {
        if(debug) System.out.println("New check at: " + new Date());

        List<String> newCheck = p.listFiles();
        List<String> newFiles = p.hasNewFiles(newCheck);
        List<String> removedFiles = p.hasRemovedFiles(newCheck);

        if (!newFiles.isEmpty()) {
            p.peerFiles.addAll(newFiles);
            if(debug) System.out.println("New file(s) added!");
            if(debug) System.out.println(p.printListOfFiles(newFiles));
        } else if (!removedFiles.isEmpty()) {
            p.peerFiles.removeAll(removedFiles);
            if(debug) System.out.println("File(s) removed!");
            if(debug) System.out.println(p.printListOfFiles(removedFiles));
        } else {
            if(debug) System.out.println("The list of files is up to date!\n");
        }

        System.out.print("Sou peer " + p.peerIP + ":" + p.peerPort + " com arquivos ");
        System.out.println(p.printListOfFiles(p.peerFiles));
    }
}
