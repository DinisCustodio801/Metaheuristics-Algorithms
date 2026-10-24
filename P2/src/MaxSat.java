import java.io.*;
import java.util.*;

/**
 * Problema MAXSAT + solucao atual.
 * - Instancia: clausulas lidas do ficheiro DIMACS (variaveis 0-based internamente).
 * - Solucao atual: UM unico array x, modificado no sitio (nunca se copia).
 * - Avaliacao incremental: cnt[c] = nº de literais verdadeiros da clausula c;
 *   fit = nº de clausulas satisfeitas (cnt[c] >= 1).
 *   Assume que nenhuma clausula contem a mesma variavel mais do que uma vez
 *   (verifica-se nas instancias uf*).
 */
public class MaxSat {
    public final int n, m;
    public final int[][] clauses;    // literais DIMACS (com sinal) de cada clausula
    public final int[][] occClause;  // para cada variavel: clausulas onde aparece
    public final int[][] occSign;    // para cada variavel: +1 se positiva, -1 se negada

    // ---- solucao atual ----
    public final boolean[] x;
    public final int[] cnt;
    public final int[] perm;         // permutacao auxiliar (ordem aleatoria da vizinhanca)
    public int fit;

    // ---- estatisticas da execucao ----
    public long evals;               // nº de avaliacoes (1 por vizinho visitado)
    public int bestFit;
    public long evalsAtBest;
    public long cpuAtBestNs;
    private long cpu0;

    private MaxSat(int n, int[][] clauses) {
        this.n = n;
        this.m = clauses.length;
        this.clauses = clauses;
        int[] count = new int[n];
        for (int[] c : clauses) for (int lit : c) count[Math.abs(lit) - 1]++;
        occClause = new int[n][];
        occSign = new int[n][];
        for (int v = 0; v < n; v++) { occClause[v] = new int[count[v]]; occSign[v] = new int[count[v]]; }
        int[] pos = new int[n];
        for (int ci = 0; ci < m; ci++)
            for (int lit : clauses[ci]) {
                int v = Math.abs(lit) - 1;
                occClause[v][pos[v]] = ci;
                occSign[v][pos[v]] = lit > 0 ? 1 : -1;
                pos[v]++;
            }
        x = new boolean[n];
        cnt = new int[m];
        perm = new int[n];
        for (int i = 0; i < n; i++) perm[i] = i;
    }

    /** Le um ficheiro DIMACS (ignora 'c', le 'p', para em '%'). */
    public static MaxSat load(String path) throws IOException {
        List<int[]> cl = new ArrayList<>();
        List<Integer> cur = new ArrayList<>();
        int n = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.charAt(0) == 'c') continue;
                if (line.charAt(0) == 'p') { n = Integer.parseInt(line.split("\\s+")[2]); continue; }
                if (line.charAt(0) == '%') break;
                for (String t : line.split("\\s+")) {
                    int v = Integer.parseInt(t);
                    if (v == 0) {
                        if (!cur.isEmpty()) {
                            int[] a = new int[cur.size()];
                            for (int i = 0; i < a.length; i++) a[i] = cur.get(i);
                            cl.add(a);
                            cur.clear();
                        }
                    } else cur.add(v);
                }
            }
        }
        return new MaxSat(n, cl.toArray(new int[0][]));
    }

    // ================= estatisticas =================

    public void resetTracker() {
        evals = 0; bestFit = -1; evalsAtBest = 0; cpuAtBestNs = 0;
        cpu0 = System.nanoTime();     // tempo decorrido (relogio monotónico, resolucao ns)
    }

    /** Guarda avaliacoes e tempo decorrido se a solucao atual for a melhor ate agora. */
    public void record() {
        if (fit > bestFit) {
            bestFit = fit;
            evalsAtBest = evals;
            cpuAtBestNs = System.nanoTime() - cpu0;
        }
    }

    // ================= solucao atual =================

    /** Nova solucao aleatoria u.a.r. Conta como 1 avaliacao. */
    public void newRandom(Random r) {
        for (int i = 0; i < n; i++) x[i] = r.nextBoolean();
        fit = 0;
        for (int c = 0; c < m; c++) {
            int k = 0;
            for (int lit : clauses[c]) if ((lit > 0) == x[Math.abs(lit) - 1]) k++;
            cnt[c] = k;
            if (k > 0) fit++;
        }
        evals++;
        record();
    }

    /** Variacao do fitness se a variavel v fosse trocada. NAO altera nada. */
    public int delta(int v) {
        int d = 0;
        int[] oc = occClause[v], sg = occSign[v];
        boolean xv = x[v];
        for (int k = 0; k < oc.length; k++) {
            boolean litTrue = (sg[k] > 0) == xv;
            int c = cnt[oc[k]];
            if (litTrue) { if (c == 1) d--; }   // era o unico literal verdadeiro
            else         { if (c == 0) d++; }   // clausula insatisfeita passa a satisfeita
        }
        return d;
    }

    /** Troca a variavel v e atualiza contadores e fitness. Chamar de novo desfaz. */
    public void flip(int v) {
        int[] oc = occClause[v], sg = occSign[v];
        boolean xv = x[v];
        for (int k = 0; k < oc.length; k++) {
            boolean litTrue = (sg[k] > 0) == xv;
            int c = oc[k];
            if (litTrue) { if (--cnt[c] == 0) fit--; }
            else         { if (cnt[c]++ == 0) fit++; }
        }
        x[v] = !xv;
    }

    /** Baralha perm (Fisher-Yates): define a ordem aleatoria de visita dos vizinhos. */
    public void shufflePerm(Random r) {
        for (int i = n - 1; i > 0; i--) {
            int j = r.nextInt(i + 1);
            int t = perm[i]; perm[i] = perm[j]; perm[j] = t;
        }
    }

    /** Repoe a ordem natural da vizinhanca (chamada no inicio de cada execucao). */
    public void resetPerm() {
        for (int i = 0; i < n; i++) perm[i] = i;
    }
}
