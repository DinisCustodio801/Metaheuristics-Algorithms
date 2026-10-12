# SatSolver
Exact SAT solver implementation in Python for DIMACS CNF files.

The solver reads any instance in DIMACS CNF format, systematically visits the
entire search space (all 2^n assignments) and reports **all** assignments that
satisfy the formula.

## Requirements
- Python 3 (standard library only, no extra packages)

## Usage
```
python sat_solver.py [--dimacs] [-o output_file] input.cnf
```

| Argument         | Description                                                                 |
|------------------|-----------------------------------------------------------------------------|
| `input.cnf`      | **Required.** Instance in DIMACS CNF format. Without it the program exits with an error. |
| `--dimacs`       | Print the result on screen in DIMACS output format (see below).             |
| `-o output_file` | Write the result to `output_file`. The file is **always** in DIMACS output format, regardless of `--dimacs`. |

Tip: do not use the `.cnf` extension for the output file (it is the extension of
input instances); use `.txt`, `.out` or `.sol`.

### Examples
```
python sat_solver.py hoos.cnf                       # human-readable, on screen
python sat_solver.py --dimacs hoos.cnf              # DIMACS format, on screen
python sat_solver.py -o output_hoos.txt hoos.cnf    # DIMACS format, to a file
```

## Output formats

### Default (on screen)
```
--- A processar hoos.cnf ---
Variaveis: 5 | Clausulas lidas: 6
Total de solucoes encontradas: 1
  11000  (x1=True, x2=True, x3=False, x4=False, x5=False)
Tempo: 0.000 s (32 atribuicoes visitadas)
```

### DIMACS (`--dimacs` on screen, always with `-o`)
Follows section 2.4 of the DIMACS format description (`satformat.pdf`):
```
c Instance: hoos.cnf
c Total de solucoes encontradas: 1
s cnf 1 5 6
v 1
v 2
v -3
v -4
v -5
t cnf 1 5 6 0.000 32
```
- `c` – comment lines
- `s cnf <SOLUTION> <VARIABLES> <CLAUSES>` – solution line (`SOLUTION` is 1 if the formula is satisfiable, 0 otherwise)
- `v <literal>` – variable lines, one per literal; a positive value means the variable is true, a negative value means false. When there are several solutions, they are listed one after the other, each with `<VARIABLES>` lines.
- `t cnf <SOLUTION> <VARIABLES> <CLAUSES> <CPUSECS> <MEASURE1>` – timing line, where `CPUSECS` is the CPU time spent in the search and `MEASURE1` is the number of assignments visited (2^n)

## How it works
1. **Parsing** – comments (`c`) are ignored, the problem line (`p cnf <vars> <clauses>`) is read, and the `%` terminator used by the SATLIB `uf` instances is handled. Clauses may span several lines. A lone `0` is read as an empty (unsatisfiable) clause. Warnings are printed to stderr if the header does not match the content.
2. **Search** – `itertools.product([False, True], repeat=n)` enumerates all 2^n assignments. Each clause is evaluated with short-circuiting (stops at the first true literal), and an assignment is discarded as soon as one clause is false.
3. **Report** – every satisfying assignment is collected and reported.

Complexity is Θ(2^n · m) for n variables and m clauses, so it is only practical for small instances (e.g. `uf20-*`, which take a couple of seconds). It is not meant for instances such as `uf100-430`.

## Files
- `sat_solver.py` – the solver
- `hoos.cnf` – example instance with 5 variables and 6 clauses (Hoos & Stützle, p. 20); its only solution is `x1=x2=True`, `x3=x4=x5=False`
- `Answers.pdf` – written answers to the assignment questions