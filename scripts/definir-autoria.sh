#!/usr/bin/env bash
# Define o seu nome/e-mail como autor(a) de todos os commits do repositório.
# Uso (Git Bash no Windows, ou terminal no Linux/macOS), na pasta do projeto:
#   bash scripts/definir-autoria.sh "Seu Nome" "seu.email@exemplo.com"
set -euo pipefail

if [ $# -ne 2 ]; then
  echo 'Uso: bash scripts/definir-autoria.sh "Seu Nome" "seu.email@exemplo.com"'
  exit 1
fi
NOME="$1"
EMAIL="$2"

git config user.name "$NOME"
git config user.email "$EMAIL"

export FILTER_BRANCH_SQUELCH_WARNING=1
git filter-branch -f --env-filter "
export GIT_AUTHOR_NAME='$NOME'
export GIT_AUTHOR_EMAIL='$EMAIL'
export GIT_COMMITTER_NAME='$NOME'
export GIT_COMMITTER_EMAIL='$EMAIL'
" --tag-name-filter cat -- --all

# remove a cópia de segurança criada pelo filter-branch
git for-each-ref --format='%(refname)' refs/original/ | xargs -r -n 1 git update-ref -d
git checkout -q main
echo
echo "Pronto! Autoria definida para: $NOME <$EMAIL>"
git log --format='%h %an <%ae> %s' -3
