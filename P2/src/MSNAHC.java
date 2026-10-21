import java.util.*;

/** Multistart Next Ascent Hillclimbing (Q2). */
public class MSNAHC {
    /**
     * Repete (solucao aleatoria + NAHC) ate encontrar o otimo global (todas as clausulas)
     * ou, no inicio de um restart, ja terem passado maxEvals avaliacoes.
     */
    static void run(MaxSat p, Random r, long maxEvals) {
        while (p.evals < maxEvals && p.bestFit < p.m) {
            p.newRandom(r);
            if (p.fit < p.m) NAHC.run(p, r);
        }
    }
}
