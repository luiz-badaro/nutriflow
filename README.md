# NutriFlow

Sistema web de gestão para nutricionistas, integrando agenda, gestão financeira, marketing e análise inteligente de dados por meio de Inteligência Artificial.

## Objetivo

Centralizar informações da rotina profissional do nutricionista, permitindo acompanhar resultados financeiros, organizar consultas, analisar investimentos em marketing e identificar oportunidades de melhoria.

## Tecnologias

- Front-End: React.js
- Back-End: Java e Spring Boot
- Banco de dados: MySQL (H2 em arquivo no desenvolvimento)
- Persistência: Spring Data JPA / Hibernate
- Gerenciador de dependências: Maven
- Inteligência Artificial: API Gemini

## Estrutura do projeto

- `frontend/`: aplicação React.
- `backend/`: aplicação Java com Spring Boot.
- `database/`: scripts e estrutura do banco de dados.
- `docs/`: documentação, diagramas C4 e protótipos.

## Equipe

Projeto acadêmico desenvolvido por uma equipe de quatro integrantes, com organização baseada em Scrum.

## Como rodar

Pré-requisitos: JDK 21 e Node.js. O Maven vem embutido (`mvnw`).

```bash
# Terminal 1 - backend (http://localhost:8765)
cd backend
./mvnw spring-boot:run        # no Windows: mvnw.cmd spring-boot:run

# Terminal 2 - frontend (http://localhost:5180)
cd frontend
npm install
npm run dev
```

Abra http://localhost:5180.

## Contas

| Perfil | E-mail | Senha |
|---|---|---|
| Nutricionista | `nutri@nutriflow.com` | `nutri123` |
| Pacientes fictícios | por exemplo `ana.souza@exemplo.com` | `paciente123` |

A conta do nutricionista é criada na primeira execução e pode ser trocada pelas variáveis
`NUTRIFLOW_ADMIN_EMAIL` e `NUTRIFLOW_ADMIN_SENHA` (antes de criar o banco). Novos pacientes se
cadastram pela tela "Criar conta".

## Ambiente de demonstração

Com `NUTRIFLOW_DEMO=true` (padrão no desenvolvimento):

- na primeira execução, com o banco vazio, são criados dados fictícios dos três meses anteriores ao mês atual;
- a aba **Configurações** permite definir uma **data simulada**, usada em todo o sistema;
- a recuperação de senha mostra o código na tela, porque o MVP não envia e-mails.

Para recomeçar a demonstração do zero, pare o backend e apague a pasta `backend/data`.

## Rodar com MySQL (entrega)

```bash
cd backend
MYSQL_USER=root MYSQL_PASSWORD=sua_senha ./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
```

O banco `nutriflow` é criado automaticamente. `MYSQL_URL` muda o endereço, se preciso.

## Análise com IA

Defina a chave antes de subir o backend; ela nunca vai para o frontend nem para o código:

```bash
GEMINI_API_KEY=sua_chave ./mvnw spring-boot:run
```

`GEMINI_MODEL` escolhe o modelo (confira o nome do modelo Flash atual na documentação do Gemini).
Sem a chave, o resto do sistema funciona e a tela de IA avisa que ela não está configurada.

## Testes

```bash
cd backend
./mvnw test
```

Cobrem agenda, solicitação/confirmação/negativa, receita única, cálculos (inclui o exemplo das
cinco consultas da especificação), retenção com data simulada, dados enviados à IA e controle de acesso.

## Decisões do MVP

- Receita e conversão entram no mês da **data da consulta**.
- Retenção considera só consultas confirmadas com data já passada; alerta acima de 45 dias.
- Projeção da IA só com pelo menos 3 meses de histórico.
- Todas as análises de IA ficam guardadas com a data de geração.
- Canais fixos: Instagram, Google, Indicação e Outro. Retorno não é canal.
- Bloqueio sobre horário já pendente ou confirmado é recusado.
- Fora do escopo: cancelamento, pagamento online, envio de mensagens, DRE e balanço.

## Status

MVP implementado: back-end, front-end e testes automatizados. Falta validar a integração real com a API Gemini (depende da chave) e a execução com MySQL.
