#!/usr/bin/env bash
set -euo pipefail

if [[ "$(uname -s)" != Linux || "$(uname -m)" != x86_64 ]]; then
    echo "Gere este pacote em um Linux x64." >&2
    exit 1
fi

raiz="$(cd -- "$(dirname -- "$0")" && pwd)"
if [[ -n "${JAVA_HOME:-}" ]]; then
    jdk_bin="$JAVA_HOME/bin"
else
    jpackage_path="$(command -v jpackage || true)"
    if [[ -z "$jpackage_path" ]]; then
        echo "Instale um JDK 17 ou superior com jpackage ou configure JAVA_HOME." >&2
        exit 1
    fi
    jdk_bin="$(dirname -- "$(readlink -f -- "$jpackage_path")")"
fi
for ferramenta in javac jar jpackage; do
    if [[ ! -x "$jdk_bin/$ferramenta" ]]; then
        echo "Ferramenta nao encontrada: $jdk_bin/$ferramenta" >&2
        exit 1
    fi
done

destino="$raiz/dist"
imagem="$destino/linux/Eudoria"
arquivo="$destino/Eudoria-Linux-x64.tar.gz"
if [[ -e "$imagem" || -e "$arquivo" ]]; then
    echo "O aplicativo ou o arquivo Linux ja existe em dist. Mova ou remova os resultados anteriores." >&2
    exit 1
fi

mkdir -p "$raiz/build" "$destino/linux"
temporario="$(mktemp -d "$raiz/build/eudoria-linux-XXXXXXXX")"
trap 'find "$temporario" -depth -delete' EXIT
mkdir -p "$temporario/classes" "$temporario/entrada"
mapfile -d '' fontes < <(find "$raiz/desafio1" -name '*.java' -type f -print0)
if (( ${#fontes[@]} == 0 )); then
    echo "Nenhum arquivo Java encontrado." >&2
    exit 1
fi

"$jdk_bin/javac" --release 11 -encoding UTF-8 -d "$temporario/classes" "${fontes[@]}"
"$jdk_bin/jar" --create --file "$temporario/entrada/eudoria.jar" --main-class desafio1.Main -C "$temporario/classes" .
"$jdk_bin/jpackage" --type app-image --name Eudoria --input "$temporario/entrada" --dest "$destino/linux" \
    --main-jar eudoria.jar --main-class desafio1.Main --add-modules java.desktop,java.logging
for nome in LICENSE NOTICE; do
    if [[ -f "$jdk_bin/../$nome" ]]; then
        cp -- "$jdk_bin/../$nome" "$imagem/$nome-JAVA.txt"
    fi
done
tar -czf "$arquivo" -C "$destino/linux" Eudoria
echo "Pacote criado: $arquivo"
