Assignment 2 — Algoritmos HillClimbing para MAXSAT
===================================================

COMO COMPILAR E CORRER (Java 21 ou superior)

1. Compilar (na pasta raiz do projeto, onde está a pasta src):
   javac -d out src\*.java

2. Correr cada questão (a pasta com os ficheiros .cnf é o 2º argumento):
   java -cp out Main q1 <pasta_cnf> 30
   java -cp out Main q2 <pasta_cnf> 30
   java -cp out Main q3 <pasta_cnf> 30
   java -cp out Main q4 <pasta_cnf> 30
   java -cp out Main q5 <pasta_cnf>

O 3º argumento é o número de execuções independentes (30 por omissão; a q5 ignora-o).
Os ficheiros .cnf (formato DIMACS) são fornecidos pelo docente na tutoria.
