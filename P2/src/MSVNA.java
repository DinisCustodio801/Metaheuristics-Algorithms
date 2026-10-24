import java.util.*;

/** Multistart Variable Neighbourhood Ascent (Q4). */
public class MSVNA {
    /**
     * Repete (solucao aleatoria + VNA) ate encontrar o otimo global (todas as clausulas)
     * ou, no inicio de um restart, ja terem passado maxEvals avaliacoes.
     */
    static void run(MaxSat p, Random r, int kMax, long maxEvals) {
        while (p.evals < maxEvals && p.bestFit < p.m) {
            p.newRandom(r);
            if (p.fit < p.m) VNA.run(p, r, kMax);
        }
    }
}
