/**
 * Q5: conta otimos locais de uf20-01 sob vizinhancas de k bits, k = 1..4.
 * Enumera as 2^20 solucoes (a solucao e um int; o bit v e o valor da variavel v).
 * Otimo local = nenhum vizinho estritamente melhor.
 *   exata : vizinhos a distancia de Hamming exatamente k
 *   ate   : vizinhos a distancia <= k (uniao de 1..k)
 */
public class LocalOptima {
    static void run(String path) throws Exception {
        MaxSat f = MaxSat.load(path);
        int n = f.n, size = 1 << n, K = 4;

        byte[] fit = new byte[size];             // fitness de todas as solucoes (<= 91 cabe num byte)
        for (int mask = 0; mask < size; mask++) {
            int c = 0;
            for (int[] cl : f.clauses)
                for (int lit : cl) {
                    boolean bit = ((mask >> (Math.abs(lit) - 1)) & 1) != 0;
                    if (bit == (lit > 0)) { c++; break; }
                }
            fit[mask] = (byte) c;
        }

        // masks[k] = todas as mascaras com exatamente k bits a 1 ("trocar k variaveis")
        int[][] masks = new int[K + 1][];
        for (int k = 1; k <= K; k++) {
            int cnt = 0;
            for (int d = 0; d < size; d++) if (Integer.bitCount(d) == k) cnt++;
            masks[k] = new int[cnt];
            int i = 0;
            for (int d = 0; d < size; d++) if (Integer.bitCount(d) == k) masks[k][i++] = d;
        }

        long[] exact = new long[K + 1], upTo = new long[K + 1];
        for (int mask = 0; mask < size; mask++) {
            boolean allLocal = true;
            for (int k = 1; k <= K; k++) {
                boolean local = true;
                for (int d : masks[k])
                    if (fit[mask ^ d] > fit[mask]) { local = false; break; }
                if (local) exact[k]++; else allLocal = false;
                if (allLocal) upTo[k]++;
            }
        }

        System.out.println("Instancia: " + path + "  (n=" + n + ", m=" + f.m + ", 2^n=" + size + ")");
        System.out.printf("%2s %8s %22s %22s%n", "k", "|N_k|", "otimos locais (exata)", "otimos locais (<=k)");
        for (int k = 1; k <= K; k++)
            System.out.printf("%2d %8d %22d %22d%n", k, masks[k].length, exact[k], upTo[k]);
    }
}
