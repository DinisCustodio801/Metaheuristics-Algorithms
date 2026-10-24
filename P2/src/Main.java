import java.io.*;
import java.util.*;

/**
 * Uso:  java Main <q1|q2|q3|q4|q5> [pasta_dos_cnf] [nº_de_runs]
 *   q1 = NAHC   q2 = MSNAHC   q3 = VNA   q4 = MSVNA   q5 = otimos locais (uf20-01)
 * Defaults: pasta "." e 30 runs.
 *
 * Como compilar e correr:
 *   javac -d out src\*.java
 *   java -cp out Main q1 <pasta_cnf> 30
 *   java -cp out Main q2 <pasta_cnf> 30
 *   java -cp out Main q3 <pasta_cnf> 30
 *   java -cp out Main q4 <pasta_cnf> 30
 *   java -cp out Main q5 <pasta_cnf>
 * 
 *  o 3º argumento e o nº de execucoes, e a q5 ignora-o)
 */
public class Main {
    static final long MAX_EVALS = 10_000_000L;
    static final int K_MAX = 3;
    static final String[] INSTANCES = {
        "uf20-01", "uf20-02", "uf20-03",
        "uf100-01", "uf100-02", "uf100-03",
        "uf250-01", "uf250-02", "uf250-03"
    };

    interface Algo { void run(MaxSat p, Random r); }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) { System.out.println("Uso: java Main <q1|q2|q3|q4|q5> [pasta] [runs]"); return; }
        String q = args[0].toLowerCase();
        String dir = args.length > 1 ? args[1] : ".";
        int runs = args.length > 2 ? Integer.parseInt(args[2]) : 30;

        switch (q) {
            case "q1": experiment("Q1: NAHC (1 bit)", dir, runs,
                    (p, r) -> { p.newRandom(r); NAHC.run(p, r); }); break;
            case "q2": experiment("Q2: MSNAHC (max 10M avaliacoes)", dir, runs,
                    (p, r) -> MSNAHC.run(p, r, MAX_EVALS)); break;
            case "q3": experiment("Q3: VNA (1 a 3 bits)", dir, runs,
                    (p, r) -> { p.newRandom(r); VNA.run(p, r, K_MAX); }); break;
            case "q4": experiment("Q4: MSVNA (1 a 3 bits, max 10M avaliacoes)", dir, runs,
                    (p, r) -> MSVNA.run(p, r, K_MAX, MAX_EVALS)); break;
            case "q5": LocalOptima.run(dir + File.separator + "uf20-01.cnf"); break;
            default: System.out.println("Pergunta desconhecida: " + q);
        }
    }

    /** Corre 'runs' execucoes independentes em cada instancia e imprime a tabela. */
    static void experiment(String title, String dir, int runs, Algo algo) throws IOException {
        System.out.println(title + "  (" + runs + " runs por instancia)");
        System.out.printf("%-10s %5s %9s %6s %11s %14s %14s%n",
            "instance", "m", "best", "#best", "mean", "evals@best", "cpu_ms@best");
        for (String name : INSTANCES) {
            MaxSat p = MaxSat.load(dir + File.separator + name + ".cnf");
            // aquecimento da JVM (compilacao JIT), nao contabilizado nas medicoes
            p.resetTracker();
            p.resetPerm();
            algo.run(p, new Random(987654L));
            int[] bf = new int[runs];
            long[] be = new long[runs], bt = new long[runs];
            for (int run = 0; run < runs; run++) {
                Random r = new Random(12345L + run);     // seed fixa => reprodutivel
                p.resetTracker();
                p.resetPerm();
                algo.run(p, r);
                bf[run] = p.bestFit; be[run] = p.evalsAtBest; bt[run] = p.cpuAtBestNs;
            }
            int best = Arrays.stream(bf).max().getAsInt();
            int nBest = 0; double e = 0, t = 0, mean = 0;
            for (int i = 0; i < runs; i++) {
                mean += bf[i];
                if (bf[i] == best) { nBest++; e += be[i]; t += bt[i]; }
            }
            System.out.printf("%-10s %5d %9d %6d %11.2f %14.1f %14.3f%n",
                name, p.m, best, nBest, mean / runs, e / nBest, t / nBest / 1e6);
        }
        System.out.println("(evals@best e cpu_ms@best: media sobre as runs que atingiram o melhor valor)");
    }
}
