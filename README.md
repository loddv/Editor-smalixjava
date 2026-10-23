# Editor-smalixjava

Aplicativo Android para inspecionar e editar código Smali, com Java planejado como visualização de apoio.

## Escopo e decisões

- Smali é a fonte editável; Java será uma visualização decompilada, inicialmente somente leitura.
- O editor de código usa [Rosemoe sora-editor](https://github.com/Rosemoe/sora-editor).
- O projeto poderá ser aberto importando um APK ou selecionando uma pasta `/smali` já extraída.
- A edição Java → Smali, reconstrução, assinatura e instalação de APK não fazem parte da primeira entrega.

## MVP iniciado

- Aplicativo Android nativo em Kotlin.
- Seleção de uma pasta com o seletor de documentos do Android e navegação por subpastas.
- Listagem e edição de arquivos `.smali`, com gravação no documento original.
- Seleção de APK disponível; decodificação e montagem ainda serão implementadas.

## Próximas etapas

1. Extrair um APK para a estrutura de projeto e identificar corretamente arquivos multi-DEX.
2. Adicionar realce e validação de sintaxe Smali.
3. Exibir a classe Java correspondente como preview sob demanda.
4. Montar DEX, reconstruir e assinar o APK exportado.
5. Adicionar backups, histórico e opções de instalação/exportação.

## Build

Abra o projeto no Android Studio ou execute `./gradlew assembleDebug` com Android SDK instalado.
