import java.util.*;

/** Next Ascent Hillclimbing, vizinhanca de 1 bit, ordem aleatoria (Q1). */
public class NAHC {
    /** Sobe a partir da solucao atual de p ate um otimo local (ou ao otimo global). */
    static void run(MaxSat p, Random r) {
        boolean improved;
        do {
            improved = false;
            p.shufflePerm(r);                     // nova ordem aleatoria da vizinhanca
            for (int i = 0; i < p.n; i++) {
                int v = p.perm[i];
                p.evals++;                        // 1 avaliacao por vizinho visitado
                if (p.delta(v) > 0) {             // vizinho melhor? (sem fazer flip)
                    p.flip(v);                    // aceita o primeiro que melhora
                    p.record();
                    improved = true;
                    break;
                }
            }
        } while (improved && p.fit < p.m);
    }
}
