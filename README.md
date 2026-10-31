# 📦 Conferente Pro

### Gestão operacional, controle de ocorrências e acompanhamento de produtos.

O **Conferente Pro** é um aplicativo Android desenvolvido para auxiliar nas rotinas de conferência, organização de demandas e acompanhamento de ocorrências no ambiente farmacêutico.

O projeto tem como objetivo centralizar informações importantes da operação, facilitar o acompanhamento de pendências e proporcionar maior controle sobre prazos, responsabilidades e processos internos.

---

## 🎯 Objetivo do projeto

Simplificar o trabalho diário do conferente por meio de uma ferramenta prática, organizada e acessível, reduzindo a dependência de anotações dispersas e melhorando a rastreabilidade das atividades.

O aplicativo está sendo desenvolvido com foco em uso individual, armazenamento local e facilidade de utilização.

## ✨ Funcionalidades

### Disponíveis na versão atual

- Cadastro e gerenciamento de demandas.
- Organização de pendências.
- Registro de lembretes.
- Acompanhamento de atividades.
- Interface Android de utilização simples.

### Em desenvolvimento e planejamento

- Dashboard com indicadores operacionais.
- Controle mensal de produtos vencidos destinados ao CD.
- Alertas automáticos para prazos.
- Registro de produtos, quantidades e ocorrências.
- Histórico cronológico de alterações.
- Controle de trocas com fornecedores.
- Relatórios operacionais.
- Exportação de informações.
- Pesquisa e filtros avançados.
- Melhorias no sistema de notificações.

> As funcionalidades planejadas serão disponibilizadas gradualmente nas próximas versões.

---

## 📅 Controle de vencidos destinados ao CD

Uma das funcionalidades centrais planejadas para o aplicativo é o acompanhamento mensal dos produtos vencidos que precisam ser encaminhados ao Centro de Distribuição (CD).

**Regra operacional:**

- Produtos vencidos durante determinado mês devem ser encaminhados ao CD entre os dias 1º e 15 do mês seguinte.
- O aplicativo deverá apresentar um alerta recorrente para esse período.
- O controle mensal não dependerá do cadastro prévio de produtos.
- O encerramento do período deverá ser realizado manualmente.
- Os períodos anteriores deverão permanecer disponíveis para consulta.

O objetivo é evitar esquecimentos e proporcionar um acompanhamento mais organizado dessa rotina.

---

## 🎨 Identidade visual

O projeto utiliza uma proposta visual corporativa, com foco em legibilidade, organização e facilidade de navegação.

**Diretrizes de interface:**

- Azul-marinho como cor principal.
- Branco e tons neutros para organização visual.
- Laranja para alertas e ações importantes.
- Tema claro e tema escuro.
- Navegação inferior.
- Painel inicial com indicadores e atalhos.

---

## 📱 Plataforma

| Informação | Detalhe |
|---|---|
| Plataforma | Android |
| Aplicativo | Conferente Pro |
| Identificador | `com.conferentepro.app` |
| Idioma | Português (Brasil) |
| Utilização | Individual |
| Armazenamento | Local |
| Distribuição | APK Android |

---

## ⚙️ Compilação e distribuição

O projeto utiliza o GitHub Actions para automatizar a geração do aplicativo Android.

O fluxo de compilação permite gerar o APK sem precisar executar manualmente o processo de compilação no computador.

### Gerar uma nova versão

1. Acesse a aba **Actions** do repositório.
2. Selecione o workflow de geração do APK.
3. Clique em **Run workflow**.
4. Aguarde a conclusão da compilação.
5. Acesse os artefatos gerados.
6. Baixe o APK correspondente à versão desejada.

### Assinatura digital

A assinatura do aplicativo é realizada por meio de uma chave privada armazenada nos GitHub Actions Secrets.

**Importante:** a chave de assinatura, as senhas e os demais segredos não devem ser publicados no repositório.

A preservação da mesma identidade de assinatura e do identificador do aplicativo é necessária para permitir atualizações sobre instalações existentes.

---

## 🔐 Privacidade e segurança

O Conferente Pro foi concebido para utilização individual, priorizando o armazenamento local das informações.

Diretrizes do projeto:

- Não publicar informações internas da operação.
- Não armazenar senhas ou chaves de assinatura no código-fonte.
- Preservar os registros do usuário durante atualizações compatíveis.
- Implementar mecanismos de backup e recuperação conforme a evolução do projeto.

O aplicativo não possui integração oficial com o sistema Big Linx. Qualquer integração futura dependerá da disponibilidade de uma API oficial e das respectivas autorizações.

---

## 🗺️ Roadmap

| Etapa |
