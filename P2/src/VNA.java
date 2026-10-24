import java.util.*;

/** Variable Neighbourhood Ascent com vizinhancas de 1 a kMax bits (Q3). */
public class VNA {
    /**
     * Sobe a partir da solucao atual de p. Usa N1, depois N2, ... ate Nk (distancia de
     * Hamming exatamente k). Quando melhora com Nk, volta a N1. Termina quando nenhuma
     * das vizinhancas melhora.
     */
    static void run(MaxSat p, Random r, int kMax) {
        int k = 1;
        while (k <= kMax && p.fit < p.m) {
            p.shufflePerm(r);
            boolean improved = (k == 1) ? step1(p) : explore(p, k, 0, 1, p.fit);
            if (improved) { p.record(); k = 1; }
            else k++;
        }
    }

    /** Vizinhanca de 1 bit: usa delta (sem fazer flip nos vizinhos rejeitados). */
    private static boolean step1(MaxSat p) {
        for (int i = 0; i < p.n; i++) {
            int v = p.perm[i];
            p.evals++;
            if (p.delta(v) > 0) { p.flip(v); return true; }
        }
        return false;
    }

    /**
     * Vizinhanca de k bits: escolhe k posicoes i1 < i2 < ... < ik da permutacao.
     * Faz flip ao descer na recursao e desfaz ao subir; so as folhas contam como avaliacao.
     * Se encontra melhoria, devolve true e deixa os flips aplicados.
     */
    private static boolean explore(MaxSat p, int k, int start, int depth, int base) {
        for (int i = start; i <= p.n - (k - depth + 1); i++) {
            int v = p.perm[i];
            p.flip(v);
            if (depth == k) {
                p.evals++;
                if (p.fit > base) return true;
            } else if (explore(p, k, i + 1, depth + 1, base)) return true;
            p.flip(v);
        }
        return false;
    }
}
