# Conferente Pro — pacote inicial para GitHub

Este pacote contém o projeto Android inicial e um fluxo do GitHub Actions para tentar compilar o APK de teste.

## Como enviar ao GitHub

1. Extraia este ZIP no computador.
2. No GitHub, crie um repositório chamado `ConferentePro`.
3. Envie **o conteúdo desta pasta**, não o arquivo ZIP fechado. Preserve a pasta oculta `.github/workflows`.
4. Faça o commit na branch `main`.
5. Abra a aba **Actions**, selecione **Gerar APK do Conferente Pro** e execute `Run workflow` se a compilação não iniciar automaticamente.
6. Quando terminar com sucesso, abra a execução e baixe o artefato `ConferentePro-APK`. Dentro dele estará `app-debug.apk`.

## O que esta versão já contém

- Projeto Android em Kotlin e Jetpack Compose.
- Banco local Room/SQLite para demandas e histórico.
- Cadastro inicial de demandas e listagem.
- Categorias, prioridades, status e tipos básicos de responsabilidade.
- Verificação periódica de demandas vencidas.
- Workflow para gerar um APK debug.

## Limitações importantes

Este é um **MVP inicial**, não a versão completa da especificação. Ainda não estão implementados todos os módulos solicitados (cadastro completo de produtos/ocorrências, fluxos completos de CD e fornecedores, calendário, relatórios, exportação PDF/Excel, anexos, leitura de código de barras e backup/restore). O formulário atual de demanda também é básico.

O APK `debug` é para teste e instalação manual. A compilação ainda precisa ser executada no GitHub Actions; este pacote não contém um APK já compilado.

## Requisitos técnicos

- Android mínimo: API 26 (Android 8.0).
- Java 17.
- Gradle 8.7.
- O app guarda os dados localmente no aparelho. Não há integração oficial com Big Linx nem sincronização em nuvem.
