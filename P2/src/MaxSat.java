import java.io.*;
import java.util.*;

/**
 * Problema MAXSAT + solucao atual.
 *   Instancia: clausulas lidas do ficheiro DIMACS (variaveis 0-based internamente).
 *   O load normaliza cada clausula: literais repetidos da mesma variavel sao fundidos
 *   num so, e tautologias (a variavel com os dois sinais na mesma clausula) sao
 *   removidas e contadas em alwaysSat (satisfeitas por qualquer atribuicao).
 *   Assim, nenhuma clausula contem a mesma variavel mais do que uma vez, que e o que
 *   a avaliacao incremental assume.
 *   Solucao atual: UM unico array x, modificado no sitio (nunca se copia).
 *   Avaliacao incremental: cnt[c] = nº de literais verdadeiros da clausula c;
 *   fit = nº de clausulas satisfeitas (cnt[c] >= 1) + alwaysSat.
 */
public class MaxSat {
    public final int n, m;            // n = nº de variaveis; m = nº TOTAL de clausulas (inclui tautologias)
    public final int alwaysSat;       // nº de clausulas tautologicas (sempre satisfeitas, ja contadas em fit)
    public final int[][] clauses;     // literais DIMACS (com sinal) de cada clausula NAO tautologica
    public final int[][] occClause;   // occ = "occurrences": occClause[v][k] = para cada variável v em que clausula c ela ocorre (k = 0..#ocorrencias-1)
    public final int[][] occSign;     // occSign[v][k] = sinal dessa ocorrencia (+1 se positiva, -1 se negada)

    // ---- solucao atual ----
    public final boolean[] x;         // x[v] = valor atual da variavel v
    public final int[] cnt;           // cnt = "count": cnt[c] = nº de literais VERDADEIROS na clausula c
    public final int[] perm;          // perm = "permutation": ordem (baralhavel) de visita dos vizinhos
    public int fit;                   // fit = "fitness": nº de clausulas satisfeitas (cnt[c] >= 1)

    // ---- estatisticas da execucao ----
    public long evals;                // evals = "evaluations": 1 por vizinho visitado
    public int bestFit;               // melhor fitness vista ate agora nesta execucao
    public long evalsAtBest;          // nº de avaliacoes quando o melhor foi atingido
    public long cpuAtBestNs;          // tempo decorrido (ns) quando o melhor foi atingido
    private long cpu0;                // instante de inicio da execucao (System.nanoTime)

    private MaxSat(int n, int[][] clauses, int alwaysSat) {
        this.n = n;
        this.alwaysSat = alwaysSat;
        this.m = clauses.length + alwaysSat;      // m = total de clausulas (inclui tautologias)
        this.clauses = clauses;
        int[] count = new int[n];
        for (int[] c : clauses) for (int lit : c) count[Math.abs(lit) - 1]++;
        occClause = new int[n][];
        occSign = new int[n][];
        for (int v = 0; v < n; v++) { occClause[v] = new int[count[v]]; occSign[v] = new int[count[v]]; }
        int[] pos = new int[n];
        for (int ci = 0; ci < clauses.length; ci++)
            for (int lit : clauses[ci]) {
                int v = Math.abs(lit) - 1;
                occClause[v][pos[v]] = ci;
                occSign[v][pos[v]] = lit > 0 ? 1 : -1;
                pos[v]++;
            }
        x = new boolean[n];
        cnt = new int[clauses.length];
        perm = new int[n];
        for (int i = 0; i < n; i++) perm[i] = i;
    }

    /** Le um ficheiro DIMACS (ignora 'c', le 'p', para em '%').
        Normaliza cada clausula: literais repetidos da mesma variavel sao fundidos num so;
        se uma variavel aparece com os dois sinais, a clausula e uma tautologia e passa
        para a contagem alwaysSat (nao entra em clauses). */
    public static MaxSat load(String path) throws IOException {
        List<int[]> cl = new ArrayList<>();
        List<Integer> cur = new ArrayList<>();
        int n = 0, taut = 0, maxV = 0;
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
                            // seen[vv]: 0 = ausente, +1 = so positiva, -1 = so negada
                            int[] seen = new int[maxV + 1];
                            boolean isTaut = false;
                            List<Integer> norm = new ArrayList<>();
                            for (int lit : cur) {
                                int vv = Math.abs(lit);
                                int sg = lit > 0 ? 1 : -1;
                                if (seen[vv] == 0) { seen[vv] = sg; norm.add(lit); }        // 1ª ocorrencia
                                else if (seen[vv] != sg) isTaut = true;                     // x e ¬x: tautologia
                                // mesmo sinal repetido: duplicado, ignora-se
                            }
                            if (isTaut) taut++;
                            else {
                                int[] a = new int[norm.size()];
                                for (int i = 0; i < a.length; i++) a[i] = norm.get(i);
                                cl.add(a);
                            }
                            cur.clear();
                        }
                    } else {
                        cur.add(v);
                        maxV = Math.max(maxV, Math.abs(v));
                    }
                }
            }
        }
        return new MaxSat(n, cl.toArray(new int[0][]), taut);
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
        fit = alwaysSat;                      // as tautologias estao sempre satisfeitas
        for (int c = 0; c < clauses.length; c++) {
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
