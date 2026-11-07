# Política de Privacidade do DaltonPick

**Data de vigência:** [DATA DO LANÇAMENTO]
**Desenvolvedor:** WCH Softworks
**Contato:** [E-MAIL DE CONTATO]
**Código-fonte:** https://github.com/WCH-Softworks/dalton-pick


## A versão curta

O DaltonPick não coleta, armazena, compartilha nem vende dados pessoais. Ele não tem a permissão de internet, então não consegue enviar nada para fora do seu celular. Não tem anúncios, ferramentas de análise nem contas.

## O que o DaltonPick faz com a sua tela

O DaltonPick mostra uma lupa sobre outros apps e diz o nome da cor que está sob a mira. Para isso, ele precisa enxergar os pixels da sua tela.

- Quando você puxa a lupa, o app faz uma captura de tela usando o serviço de acessibilidade do Android. Enquanto a lupa fica aberta, ele faz uma nova captura quando a tela rola ou muda, para a lupa continuar mostrando o que realmente está ali.
- A captura fica **somente na memória** do seu celular. Ela é usada para ampliar a área sob a lupa e para ler um pequeno grupo de pixels e descobrir o nome da cor.
- A captura **nunca é gravada no armazenamento, nunca é registrada em log, nunca é compartilhada e nunca é enviada**. Ela é descartada quando você fecha a lupa.
- O DaltonPick não lê o texto da sua tela, suas mensagens, suas senhas nem o que você digita. O serviço de acessibilidade é configurado para não conseguir ler o conteúdo das janelas.
- Alguns apps (de banco, alguns de vídeo) bloqueiam capturas de tela. Neles a captura vem em branco e o DaltonPick avisa que não consegue ver o conteúdo.

## O serviço de acessibilidade

O DaltonPick usa a API AccessibilityService do Android como ferramenta de acessibilidade para pessoas com deficiência na visão de cores. O serviço é usado apenas para fazer as capturas descritas acima. Para saber quando fazer uma nova, ele é avisado *de que* uma tela rolou ou mudou, e de quanto rolou; ele não recebe o que está na tela. Ele não executa ações por você, não toca em nada e não registra o que você faz em outros apps. Você mesmo ativa nas configurações do Android (Configurações → Acessibilidade) e pode desativar quando quiser. Com ele desativado, a lupa deixa de funcionar.

## Modo gravação de tela (alternativo)

Se você não quiser ativar o serviço de acessibilidade, o DaltonPick pode usar a gravação de tela do Android. O Android pede sua permissão a cada vez e mostra o próprio indicador de gravação enquanto ela acontece. Os quadros gravados são usados na memória, exatamente como as capturas acima, e **nunca são gravados no armazenamento nem enviados para lugar nenhum**. Você pode encerrar a gravação quando quiser pelo botão Parar da notificação, pelo app DaltonPick ou pelos controles de gravação do próprio Android.

## Modo câmera

Se você escolher o modo câmera, o DaltonPick pede a permissão da câmera nesse momento. Os quadros da câmera são processados na memória para dar nome às cores e **nunca são gravados nem enviados para lugar nenhum**. Você pode usar o resto do app sem nunca dar acesso à câmera.

## Outras permissões

- **Exibir sobre outros apps:** para desenhar a aba da borda e a lupa por cima de outros apps.
- **Notificações:** para mostrar uma pequena notificação fixa que permite exibir ou esconder a aba.
- **Serviço em primeiro plano (gravação de tela):** o Android exige isso enquanto o modo gravação de tela está em uso. Não é usado com o serviço de acessibilidade.

Nenhuma dessas permissões dá ao DaltonPick qualquer forma de coletar dados sobre você.

## O que fica guardado no seu celular

Suas preferências (tamanho da lupa, zoom, borda, posição da aba e similares, além de pequenos contadores, como se você já concluiu a introdução) ficam guardadas localmente no seu celular. Elas não têm informações pessoais. Se o backup do Android estiver ativado, o celular pode incluir essas preferências no backup normal para a sua conta Google. O DaltonPick em si não as envia para lugar nenhum.

## Terceiros

- O DaltonPick **não tem SDKs de terceiros**: nem anúncios, nem análise de uso, nem relatório de falhas, nem cobrança.
- A tela "Sobre" tem um link "Apoiar no Ko-fi". Ao tocar nele, a página do Ko-fi abre no seu navegador. A partir daí você está no site do Ko-fi, que tem a própria política de privacidade. O DaltonPick não envia nada ao Ko-fi. Apoiar o projeto não libera nada no app.

## Crianças

O DaltonPick não coleta dados pessoais de ninguém, inclusive de crianças. Ele não é voltado especificamente ao público infantil.

## Seus direitos

Como o DaltonPick não coleta nem mantém dados pessoais, não há nada para acessar, corrigir, exportar ou excluir. Leis de proteção de dados, como a LGPD (Brasil), o GDPR (UE) e leis parecidas na América Latina, dão direitos sobre dados pessoais; se tiver dúvidas sobre como elas se aplicam, escreva para o contato acima.

## Mudanças nesta política

Se uma versão futura do DaltonPick mudar a forma como trata os dados, esta página será atualizada antes do lançamento dessa versão e a data de vigência mudará. As versões anteriores desta política estão no histórico do repositório público do projeto.

## Contato

[E-MAIL DE CONTATO]
