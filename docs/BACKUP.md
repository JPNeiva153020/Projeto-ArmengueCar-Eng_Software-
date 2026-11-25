# Backup e recuperação do PostgreSQL

Os exemplos abaixo usam os executáveis oficiais do PostgreSQL e um banco de restauração separado. Ajuste caminhos, host e usuário ao ambiente. Nunca registre senhas no repositório ou em scripts.

1. Defina a senha apenas na sessão do terminal, por exemplo `$env:PGPASSWORD = Read-Host "Senha" -MaskInput`.
2. Crie o backup: `pg_dump -h localhost -U postgres -Fc -f armenguecar.backup armenguecar`.
3. Proteja o arquivo, armazene-o fora do repositório e verifique sua integridade: `pg_restore --list armenguecar.backup`.
4. Crie um banco separado com nome novo: `createdb -h localhost -U postgres armenguecar_restauracao`.
5. Restaure nele: `pg_restore -h localhost -U postgres -d armenguecar_restauracao --exit-on-error armenguecar.backup`.
6. Confira tabelas, contagens, relacionamentos e alguns registros relevantes; teste também o acesso pela aplicação antes de confiar no procedimento. Não sobrescreva o banco em uso.
7. Apague a variável da sessão com `Remove-Item Env:PGPASSWORD`.

Em 25/11/2026 foi realizado um teste local de restauração em banco temporário separado. Foram comparadas as contagens de clientes, veículos, ordens e a versão registrada pelo Flyway (4, 4, 6 e 6, respectivamente); as contagens coincidiram. O banco temporário foi removido. Essa verificação não prova, sozinha, a integridade de todos os dados nem substitui testes periódicos.
