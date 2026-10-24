/**
 * Q5: conta otimos locais de uf20-01 sob vizinhancas de k bits, k = 1..4.
 * Enumera as 2^n atribuicoes com um "odometro" binario sobre o array boolean[] x
 * (incrementa como um contador binario, de tudo false ate tudo true; cada troca de bit
 * e um flip in-place que mantem o fitness incremental de MaxSat atualizado).
 * Este ficheiro NAO usa operadores bit a bit: os vizinhos a distancia de Hamming
 * exatamente k sao gerados por combinacoes de k indices distintos, avaliados com
 * flip/unflip in-place.
 *   exata : vizinhos a distancia de Hamming exatamente k
 *   ate   : vizinhos a distancia <= k (uniao de 1..k)
 */
public class LocalOptima {
    static void run(String path) throws Exception {
        MaxSat f = MaxSat.load(path);
        int n = f.n, K = 4;
        long size = 1;
        for (int v = 0; v < n; v++) size *= 2;          // 2^n (sem operadores bit a bit)

        // atribuicao inicial: tudo false; estado incremental calculado por varrimento total
        for (int v = 0; v < n; v++) f.x[v] = false;
        f.fit = f.alwaysSat;                               // tautologias sempre satisfeitas
        for (int c = 0; c < f.clauses.length; c++) {
            int k = 0;
            for (int lit : f.clauses[c]) if (lit < 0) k++;   // x tudo false => so literais negados sao true
            f.cnt[c] = k;                                    // cnt = count: nº de literais verdadeiros na clausula c
            if (k > 0) f.fit++;                              // fit = fitness: +1 clausula satisfeita
        }

        long[] exact = new long[K + 1], upTo = new long[K + 1];

        // visita cada atribuicao exatamente uma vez (ordem do odometro)
        for (long step = 0; step < size; step++) {
            if (step > 0) odometerIncrement(f);
            int base = f.fit;
            boolean localUpTo = true;
            for (int k = 1; k <= K; k++) {
                if (!hasImproving(f, k, 0, 1, base)) exact[k]++;
                else localUpTo = false;
                if (localUpTo) upTo[k]++;
            }
        }

        System.out.println("Instancia: " + path + "  (n=" + n + ", m=" + f.m + ", 2^n=" + size + ")");
        System.out.printf("%2s %8s %22s %22s%n", "k", "|N_k|", "otimos locais (exata)", "otimos locais (<=k)");
        for (int k = 1; k <= K; k++)
            System.out.printf("%2d %8d %22d %22d%n", k, binomial(n, k), exact[k], upTo[k]);
    }

    /** Incremento de contador binario sobre boolean[] x: troca o bit 0; se ficou true o
        "vai-um" e absorvido, senao transporta para o bit seguinte. MaxSat.flip mantem
        cnt[] e fit consistentes. */
    static void odometerIncrement(MaxSat f) {
        for (int v = 0; v < f.n; v++) {
            f.flip(v);
            if (f.x[v]) return;                          // false -> true: transporte termina
        }
    }

    /** Gerador de combinacoes: existe vizinho a distancia de Hamming exatamente k com
        fitness estritamente melhor? Faz flip dos k indices escolhidos ao descer na
        recursao e unflip ao subir (estado sempre restaurado). */
    static boolean hasImproving(MaxSat f, int k, int start, int depth, int base) {
        for (int i = start; i <= f.n - (k - depth + 1); i++) {
            f.flip(i);
            boolean better;
            if (depth == k) better = f.fit > base;
            else better = hasImproving(f, k, i + 1, depth + 1, base);
            f.flip(i);
            if (better) return true;
        }
        return false;
    }

    /** C(n, k) sem overflow de fatorial (k pequeno). */
    static long binomial(int n, int k) {
        long r = 1;
        for (int i = 1; i <= k; i++) r = r * (n - k + i) / i;
        return r;
    }
}
