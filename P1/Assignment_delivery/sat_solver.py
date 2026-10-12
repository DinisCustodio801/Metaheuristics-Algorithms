import sys
import time
import itertools

def parse_dimacs_cnf(filename):
    num_vars = 0
    num_clauses = 0
    clauses = []
    current_clause = []
    
    with open(filename, 'r') as f:
        for line in f:
            line = line.strip()
            # Ignora linhas de comentario e linhas vazias
            if not line or line.startswith('c'):
                continue
            
            # Linha de configuracao do problema: p cnf <vars> <clauses>
            elif line.startswith('p'):
                parts = line.split()
                num_vars = int(parts[2])
                num_clauses = int(parts[3])

            elif line.startswith('%'):
                break

            else:
                tokens = line.split()
                for token in tokens:
                    if token == '%':
                        continue
                    
                    val = int(token)
                    if val == 0:
                        # Um '0' sem literais antes e uma clausula vazia (insatisfazivel)
                        clauses.append(current_clause)
                        current_clause = []
                    else:
                        current_clause.append(val)


    # Verifica se os literais usam variaveis dentro do intervalo declarado
    max_var = max((abs(lit) for clause in clauses for lit in clause), default=0)
    if num_vars == 0:
        num_vars = max_var
    elif max_var > num_vars:
        print(f"Aviso: existem literais ate a variavel {max_var}, mas o cabecalho "
              f"declara {num_vars} variaveis; a usar {max_var}.", file=sys.stderr)
        num_vars = max_var

    # Avisa se o numero de clausulas nao corresponde ao cabecalho
    if num_clauses and len(clauses) != num_clauses:
        print(f"Aviso: o cabecalho declara {num_clauses} clausulas, "
              f"mas foram lidas {len(clauses)}.", file=sys.stderr)

    return num_vars, clauses

def eval_clause(clause, assignment):
    for literal in clause:
        var_idx = abs(literal) - 1
        var_val = assignment[var_idx]
        
        # Literal positivo quer True; literal negativo quer False
        literal_val = var_val if literal > 0 else not var_val
        
        if literal_val:
            return True  # Clausula satisfeita
    return False

def solve_exact_sat(num_vars, clauses):
    solutions = []
    
    # Gera todas as 2^n combinacoes de (False, True)
    for assignment in itertools.product([False, True], repeat=num_vars):
        satisfied = True
        for clause in clauses:
            if not eval_clause(clause, assignment):
                satisfied = False
                break
                
        if satisfied:
            solutions.append(assignment)
            
    return solutions

if __name__ == "__main__":
    # Uso: python sat_solver.py [--dimacs] [-o saida.txt] [ficheiro.cnf]
    # -o escreve sempre em formato DIMACS; --dimacs afeta apenas o ecra
    use_dimacs = "--dimacs" in sys.argv
    args = sys.argv[1:]
    out_path = None
    if "-o" in args:
        k = args.index("-o")
        out_path = args[k + 1] if k + 1 < len(args) else None
        if out_path is None:
            sys.exit("Erro: -o requer o nome do ficheiro de output.")
        del args[k:k + 2]
    args = [a for a in args if a != "--dimacs"]
    if not args:
        sys.exit("Erro: indique o ficheiro .cnf de input.\n"
                 "Uso: python sat_solver.py [--dimacs] [-o saida.txt] ficheiro.cnf")
    file_path = args[0]

    try:
        num_vars, clauses = parse_dimacs_cnf(file_path)
    except FileNotFoundError:
        sys.exit(f"Erro: ficheiro {file_path} nao encontrado.")

    # Mede apenas a fase de busca (a solucao do problema)
    start = time.process_time()
    solutions = solve_exact_sat(num_vars, clauses)
    elapsed = time.process_time() - start

    num_assignments = 2 ** num_vars
    solution_value = 1 if solutions else 0

    def build_dimacs():
        # Formato de output do DIMACS (satformat.pdf, secao 2.4)
        out = [f"c Instance: {file_path}",
               f"c Total de solucoes encontradas: {len(solutions)}",
               f"s cnf {solution_value} {num_vars} {len(clauses)}"]
        for sol in solutions:
            # Uma linha 'v' por literal
            for i, v in enumerate(sol):
                out.append(f"v {i + 1 if v else -(i + 1)}")
        out.append(f"t cnf {solution_value} {num_vars} {len(clauses)} "
                   f"{elapsed:.3f} {num_assignments}")
        return out

    def build_normal():
        out = [f"--- A processar {file_path} ---",
               f"Variaveis: {num_vars} | Clausulas lidas: {len(clauses)}",
               f"Total de solucoes encontradas: {len(solutions)}"]
        for sol in solutions:
            bits = ''.join('1' if v else '0' for v in sol)
            values = ', '.join(f'x{i + 1}={v}' for i, v in enumerate(sol))
            out.append(f"  {bits}  ({values})")
        out.append(f"Tempo: {elapsed:.3f} s ({num_assignments} atribuicoes visitadas)")
        return out

    # O ficheiro de output (-o) esta sempre em formato DIMACS.
    # No ecra usa-se DIMACS apenas com --dimacs.
    if out_path:
        with open(out_path, "w") as f:
            f.write("\n".join(build_dimacs()) + "\n")
        print(f"Resultados (formato DIMACS) escritos em {out_path}")
    else:
        lines = build_dimacs() if use_dimacs else build_normal()
        print("\n".join(lines))