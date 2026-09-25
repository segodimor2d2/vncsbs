
---
$$$$

git clone git@github.com:segodimor2d2/vncsbs.git

eu quero fazer um app em android usando jetpack compose, com uma arquitetura MVVM,
estou pensando em fazer um app que vai ser um cliente VNC e que vai acessar e controlar remotamente outro computador,
a parte mais importante deste projeto vai ser que a tela vai fazer split na horizontal duplicando a vista do computador remoto,
para usar a tela como side by side com googles,



---
$$$$


Sim. E eu estruturaria esse projeto desde o começo pensando **primeiro no modo VR/SBS**, em vez de fazer um cliente VNC tradicional e depois tentar adaptar a tela.

A ideia seria:

```text
                 ANDROID
┌──────────────────────────────────────┐
│                                      │
│       ┌──────────────┬──────────────┐│
│       │              │              ││
│       │    REMOTO    │    REMOTO    ││
│       │      L       │      R       ││
│       │              │              ││
│       │              │              ││
│       └──────────────┴──────────────┘│
│                                      │
└──────────────────────────────────────┘
```

As duas metades mostram **a mesma imagem do computador remoto**, lado a lado, para serem vistas através dos dois olhos dos goggles.

### Arquitetura que eu usaria

```text
UI — Jetpack Compose
        │
        ▼
Player/ViewModel
        │
        ├── conexão VNC
        │
        ├── framebuffer remoto
        │
        └── comandos de mouse/teclado
                │
                ▼
          VNC Client Core
                │
                ▼
          TCP / VNC Server
```

Mais concretamente:

```text
app/
└── src/main/
    ├── java/com/rec/vrvnc/
    │   ├── MainActivity.kt
    │   │
    │   ├── ui/
    │   │   ├── VncScreen.kt
    │   │   ├── SbsRemoteView.kt
    │   │   ├── ConnectionScreen.kt
    │   │   └── SettingsScreen.kt
    │   │
    │   ├── viewmodel/
    │   │   └── VncViewModel.kt
    │   │
    │   ├── model/
    │   │   ├── VncConnection.kt
    │   │   ├── RemoteFrame.kt
    │   │   └── VncUiState.kt
    │   │
    │   └── vnc/
    │       ├── VncClient.kt
    │       ├── VncProtocol.kt
    │       └── VncInput.kt
    │
    └── cpp/
        └── ...              ← se precisarmos de C/C++
```

## Mas existe uma decisão importante

Eu **não começaria implementando o protocolo VNC inteiro em Kotlin**.

Para esse projeto, temos duas partes diferentes:

### 1. Controle da conexão

Precisamos implementar:

* conexão TCP;
* handshake RFB;
* autenticação;
* negociação de pixel format;
* envio de comandos;
* atualização da tela;
* mouse;
* teclado;
* clipboard, eventualmente.

### 2. Renderização

Essa é a parte crítica para seu projeto:

```text
VNC framebuffer
       │
       ▼
┌───────────────┐
│ imagem remota │
└───────────────┘
       │
       ├──────────────► olho esquerdo
       │
       └──────────────► olho direito
```

No primeiro protótipo, não precisamos nem de perspectiva 3D.

Podemos simplesmente fazer:

```text
┌───────────────────────────────────────┐
│                                       │
│       ┌───────────┬───────────┐       │
│       │           │           │       │
│       │  REMOTE   │  REMOTE   │       │
│       │           │           │       │
│       │           │           │       │
│       └───────────┴───────────┘       │
│                                       │
└───────────────────────────────────────┘
```

Ou seja, **renderizar o mesmo framebuffer duas vezes**.

---

# Eu faria em etapas bem pequenas

Como esse projeto tem uma parte de rede e outra de renderização, vale evitar começar com tudo ao mesmo tempo.

### Etapa 1 — projeto vazio

Criamos:

```text
Jetpack Compose
MVVM
minSdk
AndroidManifest
MainActivity
```

E uma tela simples:

```text
┌─────────────────────────────┐
│          VR VNC             │
│                             │
│      [ Connect ]            │
│                             │
└─────────────────────────────┘
```

---

### Etapa 2 — tela SBS sem VNC

Antes de qualquer rede, fazemos o componente que realmente diferencia o aplicativo:

```kotlin
@Composable
fun SbsRemoteView(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxSize()
    ) {
        RemoteView(
            modifier = Modifier.weight(1f)
        )

        RemoteView(
            modifier = Modifier.weight(1f)
        )
    }
}
```

Nesse momento colocamos uma imagem/test pattern fictícia.

Por exemplo:

```text
┌──────────────────┬──────────────────┐
│                  │                  │
│       L          │        R         │
│                  │                  │
└──────────────────┴──────────────────┘
```

Assim já podemos testar fisicamente nos goggles.

---

# Etapa 3 — criar o framebuffer

Depois introduzimos:

```kotlin
data class RemoteFrame(
    val width: Int,
    val height: Int,
    val pixels: ByteArray
)
```

Mas aqui eu provavelmente faria uma escolha diferente dependendo do desempenho.

Para um framebuffer VNC grande, **não quero ficar fazendo**:

```text
ByteArray
    ↓
Bitmap
    ↓
Compose Image
    ↓
GPU
```

a cada atualização.

Isso pode virar um gargalo rapidamente.

Para o seu caso, eu prefiro pensar desde cedo em:

```text
VNC
 │
 ▼
Framebuffer
 │
 ▼
GPU texture
 │
 ├──────────► esquerda
 │
 └──────────► direita
```

Podemos usar `AndroidView`/Surface, `Canvas`, ou uma solução baseada em OpenGL dependendo do desempenho que encontrarmos.

---

# Etapa 4 — VNC

Depois ligamos:

```text
VncViewModel
      │
      ▼
VncClient
      │
      ▼
Socket
      │
      ▼
Computador remoto
```

O ViewModel poderia ter algo parecido com:

```kotlin
data class VncUiState(
    val connected: Boolean = false,
    val connecting: Boolean = false,
    val host: String = "",
    val port: Int = 5900,
    val remoteWidth: Int = 0,
    val remoteHeight: Int = 0
)
```

E:

```kotlin
class VncViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(VncUiState())
    val uiState = _uiState.asStateFlow()

    fun connect(host: String, port: Int) {
        // conexão VNC
    }

    fun disconnect() {
        // desconectar
    }
}
```

---

# Etapa 5 — entrada

Depois fazemos o controle do computador remoto.

Por exemplo:

```text
touch Android
      │
      ▼
posição X/Y
      │
      ▼
coordenada VNC
      │
      ▼
PointerEvent
      │
      ▼
computador remoto
```

E podemos mapear:

```text
toque        → mouse move
tap          → botão esquerdo
long press   → botão direito
drag         → mouse drag
dois dedos   → scroll
teclado      → keyboard events
```

---

# E existe uma questão importante para os goggles

Eu não deixaria o SBS simplesmente como:

```text
50% | 50%
```

sem considerar a geometria do display remoto.

Por exemplo, se o computador remoto for:

```text
1920 × 1080
```

cada olho receberia:

```text
960 × 1080
```

mas isso **não preserva a proporção original**.

O ideal é controlar separadamente:

```text
SBS mode

┌─────────────────┬─────────────────┐
│                 │                 │
│     REMOTE      │      REMOTE     │
│                 │                 │
│                 │                 │
└─────────────────┴─────────────────┘
       ↑                  ↑
    eye L              eye R
```

e permitir configurações como:

```text
SBS
 ├── separação
 ├── escala
 ├── zoom
 ├── posição X
 ├── posição Y
 └── aspect ratio
```

Isso vai ser muito útil para ajustar diferentes goggles.

---

## Eu também separaria "VNC" de "visualização"

Essa decisão vai facilitar muito o projeto.

```text
                 VNC
                  │
                  ▼
          RemoteFramebuffer
                  │
                  ▼
              Renderer
             /        \
            /          \
       Normal          SBS
       View            View
```

Assim o VNC não sabe absolutamente nada sobre goggles.

Ele simplesmente entrega:

```text
RemoteFrame
```

E a UI decide:

```text
Normal:

┌──────────────────────┐
│                      │
│       REMOTE         │
│                      │
└──────────────────────┘


SBS:

┌───────────┬───────────┐
│  REMOTE   │  REMOTE   │
│     L     │     R     │
└───────────┴───────────┘
```

Isso também deixa aberta a possibilidade futura de:

```text
SBS
VR
Normal
Zoom
Fit
Crop
```

sem mexer no protocolo VNC.

---

### Um detalhe que eu considero especialmente importante

Como você já está trabalhando com **Compose + MVVM + C++/NDK/libmpv** no outro projeto Android, eu manteria este novo aplicativo inicialmente **100% Kotlin/Compose**, se conseguirmos desempenho suficiente.

Só colocaria C++ quando houver uma necessidade concreta, por exemplo:

```text
VNC decoder
        ↓
C++ / SIMD
        ↓
GPU
```

Não vale começar adicionando JNI e NDK antes de sabermos que precisamos deles.

**Primeiro objetivo:** abrir o aplicativo e mostrar corretamente a mesma área remota duplicada em SBS. Depois colocamos a conexão VNC por trás dela. Isso reduz bastante a quantidade de coisas que podem dar errado simultaneamente.



---
$$$$


Perfeito. Agora vamos fazer um passo pequeno: **tirar o `RemoteFrame` de dentro do `SbsRemoteView`**.

Assim o `SbsRemoteView` não cria mais o frame. Ele apenas **recebe o frame que veio de fora**. Isso prepara exatamente o ponto onde, depois, o VNC vai alimentar a imagem.

### Etapa 2.3 — `RemoteFrame` passa a ser entrada da tela

Substitua o `SbsRemoteView.kt` por:

```kotlin id="58321" title="SbsRemoteView.kt"
package com.rec.vncsbs.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class RemoteFrame(
    val testValue: Float = 0.5f
)

@Composable
fun SbsRemoteView(
    frame: RemoteFrame,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxSize()
    ) {
        RemoteView(
            frame = frame,
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(4.dp)
        )

        RemoteView(
            frame = frame,
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(4.dp)
        )
    }
}

@Composable
private fun RemoteView(
    frame: RemoteFrame,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
    ) {
        drawRect(
            color = Color.DarkGray
        )

        drawCircle(
            color = Color.White,
            radius = size.minDimension * 0.18f,
            center = Offset(
                x = size.width / 2f,
                y = size.height * frame.testValue
            )
        )

        drawLine(
            color = Color.White,
            start = Offset(
                x = 0f,
                y = size.height / 2f
            ),
            end = Offset(
                x = size.width,
                y = size.height / 2f
            ),
            strokeWidth = 4f
        )

        drawLine(
            color = Color.White,
            start = Offset(
                x = size.width / 2f,
                y = 0f
            ),
            end = Offset(
                x = size.width / 2f,
                y = size.height
            ),
            strokeWidth = 4f
        )
    }
}
```

Agora precisamos alterar o `VncScreen.kt`, porque ele ainda chama:

```kotlin
SbsRemoteView(
    modifier = Modifier.fillMaxSize()
)
```

Mude para:

```kotlin id="74106" title="VncScreen.kt"
package com.rec.vncsbs.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rec.vncsbs.viewmodel.VncViewModel

@Composable
fun VncScreen(
    viewModel: VncViewModel
) {
    val frame = RemoteFrame()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        SbsRemoteView(
            frame = frame,
            modifier = Modifier.fillMaxSize()
        )
    }
}
```

### Teste

Primeiro:

```bash
./gradlew assembleDebug
```

Se passar:

```bash
./gradlew installDebug
```

Visualmente deve continuar **exatamente igual**.

A diferença arquitetural é importante:

```text
VncScreen
    │
    └── RemoteFrame
            │
            ▼
      SbsRemoteView
          ├── olho esquerdo
          └── olho direito
```

Depois desse teste, o próximo passo será colocar a criação do `RemoteFrame` no `VncViewModel`, que é onde começa a ficar preparado para receber os frames do VNC.


SbsRemoteView apenas recebe o frame fora

---
$$$$

RemoteFrame
    ↓
ByteArray (pixels)
    ↓
Bitmap
    ↓
ImageBitmap
    ↓
Compose Image
    ↓
┌────────────┐  ┌────────────┐
│   LEFT     │  │   RIGHT    │
│   frame    │  │   frame    │
└────────────┘  └────────────┘

---
$$$$


Perfeito. Agora podemos começar a separar a parte de VNC da UI.

### Etapa 3.1 — Criar a camada `VncClient`

Ainda **não vamos implementar o protocolo VNC**. Vamos apenas criar a interface da camada que futuramente vai cuidar da conexão.

Crie o arquivo:

```text
app/src/main/java/com/rec/vncsbs/vnc/VncClient.kt
```

Com:

```kotlin id="45172" title="VncClient.kt"
package com.rec.vncsbs.vnc

class VncClient {

    fun connect(
        host: String,
        port: Int
    ) {
        println("VncClient: connect $host:$port")
    }

    fun disconnect() {
        println("VncClient: disconnect")
    }
}
```

Por enquanto ele **não abre socket** e não faz nenhuma comunicação. É apenas o ponto de entrada da futura implementação VNC.

### Por que separar agora?

Queremos chegar a algo assim:

```text
                 ┌─────────────────┐
                 │    VncClient    │
                 │                 │
                 │ protocolo VNC   │
                 └────────┬────────┘
                          │
                          ▼
                    RemoteFrame
                          │
                          ▼
                    VncViewModel
                          │
                          ▼
                    VncScreen
                          │
                 ┌────────┴────────┐
                 ▼                 ▼
              LEFT EYE          RIGHT EYE
```

A UI não precisa saber como o VNC funciona.

### Teste

Agora apenas:

```bash
./gradlew assembleDebug
```

Se passar, **pare aí**.

No próximo passo vamos conectar o `VncClient` ao `VncViewModel`, mas ainda sem rede. Só vamos verificar se a arquitetura está conversando corretamente antes de implementar o protocolo.

---
$$$$


Perfeito. **O primeiro teste real do VNC funcionou.** 🎯

Temos:

```text
TCP conectado
servidor RFB = RFB 003.008
```

Ou seja:

```text
Android
  ↓
TCP socket
  ↓
VNC server :5900
  ↓
RFB 003.008
```

Agora vamos avançar **um único passo**: responder ao `RFB 003.008` que o servidor enviou.

### Próximo passo: resposta de versão

Atualmente fazemos:

```kotlin
val version = String(
    versionBytes,
    Charsets.US_ASCII
)

println("VncClient: servidor RFB = $version")
```

Logo depois disso, vamos enviar a mesma versão de volta:

```kotlin
output.write(versionBytes)
output.flush()

println("VncClient: versão RFB enviada")
```

Para isso precisamos primeiro obter o `OutputStream`.

No `VncClient.kt`, troque:

```kotlin
val input = newSocket.getInputStream()
```

por:

```kotlin
val input = newSocket.getInputStream()
val output = newSocket.getOutputStream()
```

E depois de:

```kotlin
println("VncClient: servidor RFB = $version")
```

adicione:

```kotlin
output.write(versionBytes)
output.flush()

println("VncClient: versão RFB enviada")
```

Então esse trecho ficará:

```kotlin
val input = newSocket.getInputStream()
val output = newSocket.getOutputStream()

val versionBytes = ByteArray(12)

var offset = 0

while (offset < versionBytes.size) {
    val count = input.read(
        versionBytes,
        offset,
        versionBytes.size - offset
    )

    if (count < 0) {
        throw Exception("Conexão encerrada pelo servidor")
    }

    offset += count
}

val version = String(
    versionBytes,
    Charsets.US_ASCII
)

println("VncClient: servidor RFB = $version")

output.write(versionBytes)
output.flush()

println("VncClient: versão RFB enviada")
```

Compile:

```bash
./gradlew assembleDebug
```

**Só isso por enquanto.** Se compilar, me diga `foi`. A próxima etapa será ler os **Security Types** que o servidor enviar.

---
$$$$

Perfeito. O servidor respondeu corretamente.

Temos:

```text
Security Types = 19, 2
```

No RFB:

* `19` = **VeNCrypt**
* `2` = **VNC Authentication**

Então **não temos `1 = None`**. O servidor exige autenticação.

Vamos avançar com **VNC Authentication (`2`)**, que é o caminho mais simples para o primeiro teste. Ainda não vamos implementar a senha; primeiro vamos apenas selecionar o tipo `2` e ler o desafio de 16 bytes.

### 1. No `VncClient.kt`

Depois deste trecho:

```kotlin
println(
    "VncClient: Security Types = " +
        securityTypes.joinToString(", ") {
            (it.toInt() and 0xFF).toString()
        }
)
```

adicione:

```kotlin
val hasVncAuthentication = securityTypes.any {
    (it.toInt() and 0xFF) == 2
}

if (!hasVncAuthentication) {
    throw Exception("VNC Authentication (2) não disponível")
}

output.write(2)
output.flush()

println("VncClient: Security Type selecionado = 2")
```

Depois disso, o servidor deverá enviar o **challenge**, que tem exatamente 16 bytes.

Agora adicione:

```kotlin
val challenge = ByteArray(16)

var challengeOffset = 0

while (challengeOffset < challenge.size) {
    val count = input.read(
        challenge,
        challengeOffset,
        challenge.size - challengeOffset
    )

    if (count < 0) {
        throw Exception(
            "Conexão encerrada ao ler challenge"
        )
    }

    challengeOffset += count
}

println(
    "VncClient: challenge recebido = " +
        challenge.joinToString(" ") {
            "%02X".format(it.toInt() and 0xFF)
        }
)
```

O fluxo ficará:

```text
RFB 003.008
       ↓
Security Types
       ↓
19, 2
       ↓
cliente escolhe 2
       ↓
servidor envia 16 bytes
       ↓
challenge
```

### 2. Compile

```bash
./gradlew assembleDebug
```

Se der **BUILD SUCCESSFUL**, me diga `foi`.

Ainda **não vamos implementar a senha**. Primeiro vamos confirmar que conseguimos chegar até o challenge.

---
$$$$

Perfeito. **Autenticação VNC funcionou.** 🎯

O ponto decisivo é:

```text
VncClient: SecurityResult = 0
```

Agora já temos um cliente que consegue:

```text
TCP
 ↓
RFB 003.008
 ↓
Security Types
 ↓
VNC Authentication
 ↓
DES
 ↓
SecurityResult = 0
```

O próximo passo é o **ClientInit + ServerInit**, que finalmente nos dará as informações do framebuffer remoto: largura, altura e formato dos pixels.

Vamos fazer só a primeira parte agora: enviar o `ClientInit`.

Depois do `SecurityResult = 0`, o cliente precisa enviar **1 byte**:

```text
Shared Flag
```

Vamos usar `1`, permitindo que o servidor compartilhe a sessão.

### Adicione depois do `SecurityResult`

Logo após:

```kotlin
println(
    "VncClient: SecurityResult = $securityResult"
)
```

adicione:

```kotlin
if (securityResult != 0) {
    throw Exception(
        "Autenticação VNC falhou: $securityResult"
    )
}

output.write(1)
output.flush()

println(
    "VncClient: ClientInit enviado"
)
```

O fluxo será:

```text
SecurityResult = 0
        ↓
ClientInit
        ↓
Shared Flag = 1
```

Compile:

```bash
./gradlew assembleDebug
```

Se der `BUILD SUCCESSFUL`, instale e teste:

```bash
./gradlew installDebug
```

Depois me mande o log.

O que queremos ver agora é:

```text
VncClient: SecurityResult = 0
VncClient: ClientInit enviado
```

Ainda **não vamos ler o framebuffer** neste passo.

---
$$$$


Sim — e agora temos uma pista importante. A versão é **TigerVNC 1.16.2**, e o comportamento do `x0vncserver` explica por que eu não quero que continuemos adicionando tentativas às cegas.

O protocolo RFB diz que uma requisição com `incremental = 0` deve solicitar o conteúdo inteiro da área. ([GitHub][1]) O próprio TigerVNC usa uma requisição inicial desse tipo quando a conexão é inicializada. ([GitHub][2])

Além disso, o `x0vncserver` possui `CompareFB`, cujo padrão é `2` (auto), justamente para evitar atualizações desnecessárias quando não há mudança no framebuffer. ([GitHub][3])

### Então vamos fazer um teste específico do TigerVNC

No **PC que roda o `x0vncserver`**, pare o servidor atual e inicie temporariamente assim:

```bash
x0vncserver \
    -display :0 \
    -passwordfile ~/.vnc/passwd \
    -rfbport 5900 \
    -CompareFB 0
```

A diferença é:

```text
CompareFB 0
```

que desativa a comparação do framebuffer.

**Não precisamos alterar o Android agora.**

Depois, com esse servidor rodando, no Android pressione novamente **VNC CONNECT**.

Se aparecer:

```text
VncClient: mensagem recebida = 0
VncClient: rectangles = 1
```

então encontramos a causa do comportamento anterior.

Se continuar:

```text
VncClient: rectangles = 0
```

aí temos uma informação muito mais específica e podemos investigar o fluxo do TigerVNC sem ficar alterando o cliente aleatoriamente.

### Um detalhe importante

Não precisamos adicionar aquele teste de:

```kotlin
"aguardando próxima mensagem..."
```

que eu tinha sugerido antes. **Pode deixar ou remover**, mas para o próximo teste ele não é necessário.

Eu faria primeiro **somente `-CompareFB 0` no servidor**. Isso mantém nosso Android exatamente no estado atual e testa uma variável do lado do servidor.

[1]: https://github.com/svn2github/tigervnc/blob/master/rfbproto/rfbproto.rst?utm_source=chatgpt.com "tigervnc/rfbproto/rfbproto.rst at master · svn2github/tigervnc · GitHub"
[2]: https://github.com/TigerVNC/tigervnc/blob/master/common/rfb/CConnection.cxx?utm_source=chatgpt.com "tigervnc/common/rfb/CConnection.cxx at master · TigerVNC/tigervnc · GitHub"
[3]: https://github.com/TigerVNC/tigervnc/blob/master/unix/x0vncserver/x0vncserver.man?utm_source=chatgpt.com "tigervnc/unix/x0vncserver/x0vncserver.man at master · TigerVNC/tigervnc · GitHub"

---
$$$$

O que já está comprovadamente funcionando

A sequência inteira até agora é:

TCP
 ↓
RFB 003.008
 ↓
Security Types: 19, 2
 ↓
seleciona VNC Authentication (2)
 ↓
challenge 16 bytes
 ↓
DES com a senha
 ↓
SecurityResult = 0       ← senha aceita
 ↓
ClientInit
 ↓
ServerInit
 ↓
1920x1080
 ↓
PixelFormat
 ↓
SetPixelFormat
 ↓
SetEncodings = RAW
 ↓
FramebufferUpdateRequest
 ↓
FramebufferUpdate
 ↓
rectangles = 0

Portanto, não é um problema de senha.

O problema que estamos investigando agora está depois da autenticação, especificamente por que o x0vncserver está devolvendo um FramebufferUpdate com zero rectangles.

E você fez certo em perguntar: não precisamos colocar uma tela de senha agora. Primeiro vamos conseguir receber a imagem. Depois podemos decidir como a senha será configurada na interface do aplicativo.

---
$$$$


Agora temos um dado muito mais útil. Esses 16 bytes:

```text
24 00 00 00 00 07 80 00 22 00 00 00 00 33 33 33
```

mostram que **não devemos continuar tratando cada leitura isolada como uma nova mensagem RFB sem considerar o stream inteiro**.

E há uma pista forte nos bytes:

```text
00 00 00 00 07 80 00 22 00 00 00 00
```

Se começarmos a partir do segundo byte, isso tem exatamente a estrutura de um rectangle RAW:

```text
x       = 0
y       = 0
width   = 1920      // 0x0780
height  = 34        // 0x0022
encoding= 0         // RAW
```

E depois:

```text
33 33 33 ...
```

parece já ser o começo dos **pixels**.

Então temos uma hipótese muito mais concreta: **o servidor está enviando dados de framebuffer, mas nosso cliente perdeu o alinhamento ao interpretar as mensagens**.

Isso também explica por que o teste anterior mostrou:

```text
mensagem recebida = 0
rectangles = 0
```

e depois apareceu algo que parece um rectangle.

### Vamos fazer o próximo teste de forma correta

Não vamos adicionar outro request.

Vamos apenas **capturar o header inteiro da segunda resposta**, começando pela leitura que já temos, mas sem tentar interpretar ainda.

Troque o código do segundo request por este bloco:

```kotlin
val secondHeader = ByteArray(4)

readFully(
    input,
    secondHeader
)

println(
    "VncClient: segunda resposta header = " +
        secondHeader.joinToString(" ") {
            "%02X".format(it.toInt() and 0xFF)
        }
)
```

Ou seja, remova este código que colocamos anteriormente:

```kotlin
val secondMessage = ByteArray(16)

readFully(
    input,
    secondMessage
)

println(
    "VncClient: próximos 16 bytes = " +
        secondMessage.joinToString(" ") {
            "%02X".format(it.toInt() and 0xFF)
        }
)
```

### Mas tem uma coisa ainda mais importante

**Não faça outro `FramebufferUpdateRequest` depois disso.**

A sequência que temos atualmente já é suficiente para o teste.

Queremos descobrir exatamente qual é o próximo header no stream:

```text
XX XX XX XX
```

Se vier:

```text
00 00 00 01
```

temos:

```text
FramebufferUpdate
padding
1 rectangle
```

Se vier:

```text
00 00 00 00
```

é um update vazio.

Se vier algo diferente, saberemos que existe outra mensagem/estado no stream.

A especificação confirma que um `FramebufferUpdate` começa obrigatoriamente com:

```text
00
padding
U16 rectangle-count
```

e cada rectangle começa com `x`, `y`, `width`, `height` e `encoding`. ([GitHub][1])

### Um ponto importante sobre o TigerVNC

Também confirmei que o `x0vncserver` tem `CompareFB` e que `0` realmente desativa a comparação. ([GitHub][2])

Portanto, **não precisamos continuar mexendo nessa configuração do servidor**. Agora o foco deve ser o parser do nosso cliente.

Faça somente essa alteração, rode novamente:

```bash
./gradlew installDebug
```

e:

```bash
adb logcat -c
adb logcat -v threadtime | grep -E "VncClient"
```

e me passe a linha:

```text
VncClient: segunda resposta header = ...
```

A partir desses 4 bytes vamos continuar o parser **sem mais tentativas cegas**.

[1]: https://github.com/svn2github/tigervnc/blob/master/rfbproto/rfbproto.rst?utm_source=chatgpt.com "tigervnc/rfbproto/rfbproto.rst at master · svn2github/tigervnc · GitHub"
[2]: https://github.com/TigerVNC/tigervnc/blob/master/unix/x0vncserver/x0vncserver.man?utm_source=chatgpt.com "tigervnc/unix/x0vncserver/x0vncserver.man at master · TigerVNC/tigervnc · GitHub"

---
$$$$

comunicação VNC funcionando agora toca ler os 36 rectangles

---
$$$$


Sim — esse log deixa o problema **bem localizado**.

Os rectangles **30–34 estão corretos**. O parser quebra ao chegar no **35**.

### O que o log mostra

Até o rectangle 34:

```text
header 34 = 02 8F 02 E2 00 01 00 01 00 00 00 00
rectangle 34 = 655,738 1x1 encoding=0
rectangle 34 copiado para framebuffer
```

Tudo consistente:

* `x = 655`
* `y = 738`
* `width = 1`
* `height = 1`
* `encoding = 0` → Raw
* `1 × 1 × 4 = 4 bytes` de pixel

Depois:

```text
iniciando rectangle 35
header 35 = 03 00 00 00 00 00 00 68 61 64 62 20
rectangle 35 = 768,0 0x104 encoding=1633968672
```

Isso **não é um header válido de rectangle**.

O valor:

```text
68 61 64 62 20
```

é ASCII:

```text
h a d b [espaço]
```

Ou seja, o parser está lendo **dados que deveriam pertencer a outra coisa** como se fossem o header do rectangle 35.

E há outro indício muito forte:

```text
03 00 00 00 00 00 00
```

interpretado como:

```text
x = 768
y = 0
width = 0
height = 104
```

Isso obviamente não corresponde ao rectangle esperado.

## Portanto, o problema mudou de lugar

O contador está correto:

```text
00 24
```

= **36 rectangles**

E os rectangles:

```text
0 ... 29
30
31
32
33
34
```

estão sendo consumidos corretamente.

O erro acontece **antes de tentar ler o header 35**, porque o offset do stream já está errado.

### A hipótese mais provável

O rectangle 34 é:

```text
1x1
encoding=0
```

Então ele deve consumir **exatamente 4 bytes de pixel**.

Precisamos verificar se o código realmente está consumindo esses 4 bytes **e somente esses 4 bytes**.

Se houver qualquer diferença entre:

```text
bytes esperados = width * height * bytesPerPixel
```

e os bytes efetivamente lidos, o próximo `readFully()` começa no lugar errado e produz exatamente esse tipo de lixo.

---

### Eu faria agora só um teste

Não mexeria ainda na arquitetura.

No código que processa um rectangle `encoding == 0`, imediatamente **antes e depois** da leitura dos pixels do rectangle 34, coloque um log do número de bytes.

Algo conceitualmente assim:

```kotlin
val bytesPerPixel = 4
val expectedBytes = width * height * bytesPerPixel

System.out.println(
    "VncClient: rectangle $i RAW expectedBytes=$expectedBytes"
)

input.readFully(pixelBuffer)

System.out.println(
    "VncClient: rectangle $i RAW pixels lidos=$expectedBytes"
)
```

Mas quero fazer isso **no seu código real**, porque nesse ponto é importante não introduzir uma segunda variável ou alterar a forma de leitura.

Se você me mandar o trecho da função que faz:

```text
rectangle ...
encoding == 0
copiado para framebuffer
```

eu te digo **exatamente onde colocar os logs**, sem refatorar nada.

O objetivo do próximo teste é simples: **provar que o rectangle 34 deixa o cursor exatamente no primeiro byte do header 35**.

---
$$$$


primeira imagem do vnc

---
$$$$

A parte importante do VNC já está funcionando:

✅ handshake
✅ SetPixelFormat
✅ SetEncodings
✅ FramebufferUpdateRequest
✅ múltiplos rectangles
✅ montagem do framebuffer 1920×1080
✅ RAW encoding
✅ conversão correta BGRX → ARGB
✅ renderização no Compose

Eu faria só a remoção desse log agora, antes de avançarmos para a próxima funcionalidade.

---
$$$$


# adb logcat -d -v threadtime | grep "SbsRemoteView: BITMAP"

Agora ficou bem claro.

O `VncClient` **está recebendo frames muito rapidamente**:

```text
12.751
12.768   +17 ms
12.794   +26 ms
12.816   +22 ms
12.847   +31 ms
...
13.476
13.686
13.700
13.722
```

Ou seja, o servidor pode estar entregando dezenas de atualizações por segundo.

Depois aparece:

```text
13.722
15.645   +1,9 s
17.186   +1,5 s
19.377   +2,2 s
```

Isso é um forte sinal de **backpressure**: a UI não consegue acompanhar tudo que está chegando.

E temos a confirmação adicional de que cada renderização custa ~55–70 ms. Portanto, tentar renderizar **cada framebuffer recebido** não é sustentável.

### Próximo teste: limitar a UI a ~30 FPS

Não vamos mexer no VNC nem no protocolo.

Vamos apenas fazer o `VncViewModel` **não publicar um novo estado se ainda não passou tempo suficiente**.

Isso é um teste, não a solução definitiva.

No `VncViewModel.kt`, você tem:

```kotlin
private val vncClient = VncClient { frame ->

    println(
        "VncViewModel: RECEBEU FRAME " +
            "${frame.width}x${frame.height} " +
            "${frame.pixels.size} bytes"
    )

    _uiState.value = _uiState.value.copy(
        frame = frame
    )

    println(
        "VncViewModel: STATE ATUALIZADO"
    )
}
```

Vamos colocar um intervalo de **33 ms**, aproximadamente 30 FPS.

Primeiro, dentro da classe `VncViewModel`, antes de `vncClient`, adicione:

```kotlin
private var lastFrameTime = 0L
```

Depois substitua somente o callback por:

```kotlin
private val vncClient = VncClient { frame ->

    println(
        "VncViewModel: RECEBEU FRAME " +
            "${frame.width}x${frame.height} " +
            "${frame.pixels.size} bytes"
    )

    val now = System.currentTimeMillis()

    if (now - lastFrameTime >= 33) {

        lastFrameTime = now

        _uiState.value = _uiState.value.copy(
            frame = frame
        )

        println(
            "VncViewModel: STATE ATUALIZADO"
        )
    }
}
```

### O que esse teste deve fazer

O VNC pode continuar recebendo:

```text
20 frames
30 frames
40 frames
```

por segundo.

Mas o Compose receberá no máximo aproximadamente:

```text
30 frames/s
```

Isso deve diminuir bastante a pressão sobre:

```text
ByteArray.copyOf()
        ↓
StateFlow
        ↓
Compose
        ↓
IntArray
        ↓
Bitmap
        ↓
GPU
```

### Teste

```bash
./gradlew installDebug
adb shell am force-stop com.rec.vncsbs
adb shell am start -n com.rec.vncsbs/.MainActivity
```

Depois deixe rodar uns 10 segundos e:

```bash
adb logcat -d -v threadtime | grep -E "VncViewModel: (RECEBEU|STATE ATUALIZADO)|SbsRemoteView: BITMAP" | tail -60
```

O que quero verificar é se a tela fica **mais fluida e sem aquelas pausas de vários segundos**.

Não mexemos em mais nada neste teste.

---
$$$$


Sim — agora ficou bem mais claro o que está acontecendo.

O seu log mostra um ponto importante: **o VNC está recebendo os rectangles continuamente**, mas cada rectangle está levando dezenas de ms para ser lido/copied:

* rectangle 10: ~74 ms
* 11: ~46 ms
* 12: ~84 ms
* 13: ~61 ms
* 14: ~66 ms
* 15: ~50 ms
* 16: ~49 ms
* 17: ~76 ms
* etc.

E são **dezenas de rectangles para montar um único framebuffer 1920×1080**.

Então temos dois problemas acontecendo em sequência:

```text
VNC server
   ↓
RAW rectangles
   ↓
VncClient
   ↓
framebufferPixels.copyOf()   ← 8,3 MB
   ↓
RemoteFrame
   ↓
Channel
   ↓
BGRX → ARGB
   ↓
Bitmap 1920×1080
   ↓
2 Images
```

O ponto mais importante é este:

### Estamos publicando um `RemoteFrame` depois de cada rectangle

Se um framebuffer tem, por exemplo, 32 rectangles:

```text
rectangle 0 → RemoteFrame
rectangle 1 → RemoteFrame
rectangle 2 → RemoteFrame
...
rectangle 31 → RemoteFrame
```

Ou seja, estamos tentando converter **o framebuffer inteiro 32 vezes**, enquanto ele ainda está sendo montado.

Isso explica muito bem o comportamento que você descreveu:

> "o atraso vem por ciclos"

E também:

> "a tela fica vários segundos atrás"

### Portanto, o próximo teste deve ser diferente

Não devemos gerar `RemoteFrame` dentro do loop de rectangles.

Precisamos primeiro:

```text
receber todos os rectangles
        ↓
montar framebuffer completo
        ↓
SOMENTE ENTÃO
        ↓
gerar 1 RemoteFrame
```

Isso é uma mudança pequena no conceito e provavelmente muito mais importante que continuar mexendo no Compose.

**Mas não quero alterar isso ainda sem olhar o trecho exato do seu `VncClient.kt` que está fazendo o `onFrame(RemoteFrame(...))`.**

Me mande este trecho:

```bash
grep -n -B 15 -A 15 "onFrame" app/src/main/java/com/rec/vncsbs/vnc/VncClient.kt
```

Assim eu te passo **somente a alteração necessária**, preservando o restante do cliente.

---
$$$$


Perfeito. E revendo o trecho, preciso corrigir uma coisa da minha leitura anterior: **o `onFrame()` não está dentro do loop de rectangles**. Ele está depois de todos eles. Isso é bom.

Ou seja, este trecho:

```kotlin
onFrame(
    RemoteFrame(
        width = framebufferWidth,
        height = framebufferHeight,
        pixels = framebufferPixels.copyOf()
    )
)
```

já acontece **uma vez por `FramebufferUpdate`**, não uma vez por rectangle.

Então **não vamos mexer nisso**.

O seu log, porém, revela outra coisa importante: o problema parece estar **antes do `onFrame()`**.

Veja:

```text
22:27:49.933 rectangle 10 copiado
22:27:49.979 rectangle 11 copiado
22:27:50.063 rectangle 12 copiado
22:27:50.123 rectangle 13 copiado
...
22:27:50.779 rectangle 25 copiado
```

O cliente está levando aproximadamente **0,8 segundo só para processar os rectangles 10–25**. E isso é apenas parte de um framebuffer.

### O próximo teste deve ser no `VncClient`

Temos muitos `println()` dentro do processamento dos rectangles. Por exemplo:

```kotlin
println("VncClient: iniciando rectangle $rectangleIndex")
```

```kotlin
println("VncClient: header $rectangleIndex = ...")
```

```kotlin
println("VncClient: rectangle $rectangleIndex = ...")
```

```kotlin
println("VncClient: rectangle $rectangleIndex copiado para framebuffer")
```

Esses logs estão sendo executados **para cada rectangle**, enquanto estamos recebendo megabytes de RAW.

E você já observou anteriormente que os logs deixavam o aplicativo consideravelmente mais lento.

## Vamos fazer um teste bem pequeno

**Não altere a lógica do VNC.**

Vamos apenas retirar temporariamente os logs internos dos rectangles.

No `VncClient.kt`, procure:

```kotlin
println("VncClient: iniciando rectangle $rectangleIndex")
```

e remova.

Depois remova também o:

```kotlin
println(
    "VncClient: header $rectangleIndex = " +
        ...
)
```

e:

```kotlin
println(
    "VncClient: rectangle $rectangleIndex = " +
        ...
)
```

e:

```kotlin
println(
    "VncClient: rectangle $rectangleIndex copiado para framebuffer"
)
```

**Não remova estes dois:**

```kotlin
println(
    "VncClient: framebuffer completo = " +
        "${framebufferWidth}x${framebufferHeight} " +
        "${framebufferPixels.size} bytes"
)
```

e:

```kotlin
println(
    "VncClient: enviando framebuffer para UI"
)
```

Assim conseguimos continuar vendo quando um framebuffer inteiro terminou.

### Depois

Compile:

```bash
./gradlew assembleDebug
```

Se passar:

```bash
./gradlew installDebug
adb shell am start -n com.rec.vncsbs/.MainActivity
```

E rode:

```bash
adb logcat -c && adb logcat -v threadtime | grep -E "VncClient: framebuffer|VncClient: enviando"
```

Esse teste é importante porque elimina uma variável enorme: **o custo do logging por rectangle**.

Se depois disso o atraso cair bastante, achamos um gargalo concreto. Se continuar em segundos, aí vamos medir o tempo entre **fim do framebuffer → `onFrame()` → Bitmap**, sem adicionar logs por pixel/rectangle.

---
$$$$


Agora temos a causa praticamente fechada.

Veja:

```text
rectangle 0  = 68 ms
rectangle 1  = 194 ms
...
rectangle 34 = 39 ms

rectangles total = 3123 ms
```

Cada rectangle tem aproximadamente:

```text
1920 × 34 × 4 = 261120 bytes
```

Ou seja, o servidor está enviando o framebuffer como **35 pedaços horizontais**, e o cliente está esperando cada pedaço chegar pelo socket.

O problema não é `copyOf()`, nem `Bitmap`, nem Compose. O gargalo está na quantidade de dados RAW que estamos pedindo.

### O ponto crítico

Você configurou:

```kotlin
setEncodings[4] = 0
setEncodings[5] = 0
setEncodings[6] = 0
setEncodings[7] = 0
```

Isso significa:

```text
RAW
```

Com RAW, cada rectangle de `1920×34` precisa transmitir **261 KB**.

35 rectangles ≈ **9 MB por atualização**.

E seu log mostra que isso está chegando em aproximadamente **3,1 segundos**.

---

## Mas não vamos mudar encoding ainda

Temos uma informação importante antes de fazer uma alteração maior.

O servidor está aparentemente limitando cada rectangle a:

```text
1920 × 34
```

Isso sugere que ele está dividindo a tela em faixas horizontais. Pode ser comportamento do servidor/driver VNC, não necessariamente algo errado no cliente.

O próximo teste deve ser bem simples: **medir a velocidade efetiva da transmissão**.

Você já tem os dados necessários.

Nesse primeiro FramebufferUpdate:

```text
~9 MB
~3123 ms
```

Isso dá aproximadamente:

```text
9 MB / 3,1 s ≈ 2,9 MB/s
```

Então o telefone está recebendo o RAW a uma velocidade muito baixa para atualizar uma tela 1920×1080 em tempo real.

### A solução provável

Em vez de:

```text
RAW
```

vamos futuramente usar uma codificação VNC comprimida, por exemplo:

```text
ZRLE
```

ou:

```text
Hextile
```

Isso pode reduzir drasticamente a quantidade de dados transmitidos quando a tela tem regiões que não mudaram ou possuem áreas uniformes.

**Mas isso exige implementar a decodificação correspondente no cliente.** Não quero simplesmente trocar:

```kotlin
SetEncodings = RAW
```

por `ZRLE` porque seu código atualmente faz:

```kotlin
if (encoding != 0) {
    throw Exception(
        "Encoding não suportado: $encoding"
    )
}
```

e portanto quebraria imediatamente.

---

### Antes disso, vamos fazer um teste ainda mais importante

Seu cliente atualmente envia:

```kotlin
request[6] = framebufferWidth
request[7] = ...
request[8] = framebufferHeight
request[9] = ...
```

e pede **a tela inteira a cada atualização**.

O protocolo VNC incremental deveria permitir:

```text
primeiro request:
incremental = false
→ recebe tela inteira

seguintes:
incremental = true
→ recebe somente regiões alteradas
```

Você já está fazendo isso corretamente:

```kotlin
request[1] = if (incremental) 1 else 0
```

Portanto, o servidor está decidindo quais rectangles enviar.

E isso nos dá uma pista importante: **quando você move uma janela, o servidor realmente precisa transmitir grandes regiões alteradas**, e como estamos usando RAW, isso vira megabytes de dados.

---

## Próximo passo

Eu **não mudaria código ainda**.

Quero primeiro confirmar uma coisa: quando você fica completamente parado no desktop remoto, o servidor continua enviando esses rectangles grandes?

Deixe o desktop remoto parado por uns **5 segundos**, sem mover mouse/janela, e rode:

```bash
adb logcat -c && adb logcat -v threadtime | grep -E "rectangles total|FramebufferUpdateRequest"
```

Se aparecer apenas o primeiro framebuffer e depois parar, o comportamento está correto.

Se continuar aparecendo:

```text
rectangles total = ...
```

mesmo com a tela completamente parada, aí temos **outro problema no ciclo incremental** que devemos corrigir antes de pensar em compressão.

---
$$$$


Isso confirma uma coisa importante: **mesmo parado, o servidor está enviando continuamente atualizações enormes**.

A sequência foi:

```text
22:59:24  request full
22:59:27  update → 3741 ms
22:59:27  request incremental=true

22:59:32  update → 4926 ms
22:59:32  request incremental=true

22:59:35  update → 2832 ms
22:59:35  request incremental=true

22:59:38  update → 3165 ms
22:59:38  request incremental=true
```

Ou seja, **não é o `copyOf()`, Compose ou conversão para Bitmap que está causando esses ~3–5 segundos**. O gargalo está antes, na transmissão dos rectangles RAW.

### O que está acontecendo

Seu cliente pede:

```text
1920 × 1080
incremental=true
```

mas o servidor continua entregando algo próximo de um framebuffer inteiro, dividido em vários rectangles horizontais.

Como cada rectangle RAW usa:

```text
1920 × 34 × 4 ≈ 261 KB
```

e são dezenas deles, cada atualização chega a vários MB.

Então o ciclo atual é aproximadamente:

```text
cliente
   │
   │ FramebufferUpdateRequest
   ▼
servidor
   │
   │ vários rectangles RAW
   │ ~8 MB
   ▼
cliente
   │
   │ demora 3–5 s recebendo
   ▼
onFrame()
   │
   └── novo FramebufferUpdateRequest
           │
           └── servidor envia tudo novamente
```

### Portanto, o próximo passo não deve ser mexer na UI

A arquitetura de recebimento está funcionando. O problema é que **RAW é extremamente caro para essa situação**.

O próximo teste mais útil é descobrir **por que o servidor considera praticamente toda a tela modificada a cada atualização**.

Antes de implementar ZRLE/Hextile, eu faria **um único teste pequeno**: verificar quantos rectangles e quantos bytes o servidor está enviando em cada atualização.

No `VncClient.kt`, dentro do processamento do `FramebufferUpdate`, adicione apenas estes contadores:

Antes do:

```kotlin
for (rectangleIndex in 0 until rectangleCount) {
```

coloque:

```kotlin
var totalRectangleBytes = 0L
```

E depois de:

```kotlin
val pixelBytes =
    rectWidth * rectHeight * 4
```

adicione:

```kotlin
totalRectangleBytes += pixelBytes
```

Depois do `for` dos rectangles, antes de:

```kotlin
println(
    "VncClient: rectangles total = " +
        ...
)
```

adicione:

```kotlin
println(
    "VncClient: rectangles = $rectangleCount " +
        "bytes = $totalRectangleBytes"
)
```

Então rode novamente:

```bash
./gradlew installDebug
adb shell am start -n com.rec.vncsbs/.MainActivity
```

e:

```bash
adb logcat -c && adb logcat -v threadtime | grep -E "rectangles =|rectangles total"
```

Deixe a área remota **completamente parada por alguns segundos**.

O resultado que queremos saber é algo como:

```text
rectangles = 35 bytes = 8294400
rectangles total = 3741 ms

rectangles = 35 bytes = 8294400
rectangles total = 4926 ms
```

Se for isso, fica confirmado que o servidor está mandando praticamente **8,3 MB completos repetidamente**, apesar de `incremental=true`.

Aí temos uma informação muito mais precisa para decidir o próximo passo, sem mexer em várias partes do aplicativo ao mesmo tempo.

---
$$$$


Perfeito. Agora temos a confirmação definitiva do gargalo.

### O que os números mostram

A tela é:

```text
1920 × 1080 × 4 = 8.294.400 bytes
```

E o servidor está enviando:

```text
35 rectangles → 8.294.400 bytes
34 rectangles → 7.740.640 bytes
38 rectangles → 7.923.360 bytes
34 rectangles → 7.740.640 bytes
38 rectangles → 7.923.360 bytes
```

Ou seja, **a cada atualização ele está transmitindo praticamente a tela inteira**.

E isso acontece mesmo sem você mexer no desktop.

O tempo:

```text
7.7–8.3 MB
    ↓
2.4–2.8 segundos
```

bate exatamente com o que vimos anteriormente.

### Então não vamos mexer na UI

Neste momento:

* `copyOf()` → ~1–8 ms
* `onFrame()` → ~1–10 ms
* conversão Bitmap → ~50–70 ms
* recebimento RAW → **2.4–2.8 s**

O problema está claramente no caminho:

```text
VNC server
    ↓
RAW encoding
    ↓
~8 MB
    ↓
rede
    ↓
VncClient.readFully()
```

### E tem uma consequência importante

O `incremental=true` **está sendo enviado corretamente**, mas isso não significa que o servidor necessariamente vai mandar apenas as áreas alteradas.

Quem decide quais rectangles colocar no `FramebufferUpdate` é o servidor VNC.

No nosso caso, ele está efetivamente tratando quase todo o framebuffer como atualizado.

---

## Próximo passo: não implementar ZRLE ainda

Eu faria agora **um teste bem pequeno no protocolo**.

Em vez de pedir:

```text
RAW
```

vamos pedir **Hextile**, mas **sem implementar Hextile ainda**.

Isso serve para descobrir se o servidor aceita essa codificação e, principalmente, se ele passa a responder com rectangles diferentes.

Atualmente você tem:

```kotlin
val setEncodings = ByteArray(8)

setEncodings[0] = 2
setEncodings[1] = 0
setEncodings[2] = 0
setEncodings[3] = 1

setEncodings[4] = 0
setEncodings[5] = 0
setEncodings[6] = 0
setEncodings[7] = 0
```

Isso significa:

```text
number-of-encodings = 1
encoding = 0 (Raw)
```

**Não altere isso ainda.**

O próximo passo é decidir qual encoding comprimido implementar. Para esse cliente, eu sugiro **ZRLE** como próximo alvo, porque ele é bem mais eficiente para uma tela inteira com muita informação e continua relativamente simples de implementar em comparação com Tight.

Mas antes disso, há uma questão importante: **o servidor VNC que você está usando**.

Se você me disser qual servidor está rodando em `192.168.31.127` — por exemplo `x11vnc`, `TigerVNC`, `wayvnc`, `RealVNC`, etc. — podemos verificar exatamente quais encodings ele suporta e como ele está configurado.

Se você não souber, rode no servidor:

```bash
ps aux | grep -Ei 'vnc|x11vnc|tigervnc|wayvnc'
```

e me mande o resultado.

**Não precisamos alterar o Android ainda.**

---
$$$$


Agora temos um resultado **bem diferente e muito importante**.

`-CompareFB 1` mudou o comportamento do servidor. Compare:

### Antes — `-CompareFB 0`

```text
35 rectangles → 8,294,400 bytes → 2770 ms
34 rectangles → 7,740,640 bytes → 2545 ms
38 rectangles → 7,923,360 bytes → 2828 ms
```

Praticamente um framebuffer inteiro a cada atualização.

### Agora — `-CompareFB 1`

Depois do primeiro frame:

```text
49 rectangles →   832,392 bytes → 592 ms
39 rectangles →   798,080 bytes → 420 ms
38 rectangles →   645,568 bytes → 337 ms
34 rectangles →   475,904 bytes → 209 ms
45 rectangles →   312,640 bytes → 377 ms
28 rectangles →   191,840 bytes → 126 ms
22 rectangles →   188,192 bytes → 119 ms
20 rectangles →   182,624 bytes →  51 ms
...
31 rectangles →   120,256 bytes →  22 ms
31 rectangles →   120,704 bytes →  46 ms
```

Isso é uma redução de **ordem de grandeza** no volume transmitido.

Por exemplo:

```text
8,294,400 bytes
        ↓
  120,256 bytes
```

aproximadamente **69× menos dados** naquele update.

## Mas apareceu uma segunda coisa

O servidor continua enviando updates mesmo aparentemente parado, só que agora os updates são pequenos.

Isso pode ser normal dependendo do que está acontecendo no desktop — cursor, compositor, animações, etc. O ponto principal é que **o `CompareFB` resolveu o problema de enviar o framebuffer inteiro**.

### Então eu faria agora uma coisa simples

Não altere mais o código Android.

Deixe:

```bash
x0vncserver \
    -display :0 \
    -passwordfile /home/segodimo/.vnc/passwd \
    -rfbport 5900 \
    -CompareFB 1
```

como configuração do servidor.

Depois faça um teste visual no `vncsbs`:

1. Conecte.
2. Espere a imagem aparecer.
3. **Não mexa absolutamente nada por uns 10 segundos.**
4. Observe se o atraso que tínhamos de vários segundos desapareceu.
5. Depois mova uma janela ou o mouse e veja como a atualização responde.

Não precisamos mexer no `SbsRemoteView`, `VncViewModel` ou `VncClient` ainda.

**A descoberta principal foi esta: o grande gargalo não era o Android; era `x0vncserver -CompareFB 0`.**

---
$$$$
@@@@



```bash


e /home/segodimo/05android/vncsbs/readme.md
e /home/segodimo/05android/vncsbs/app/build.gradle.kts
e /home/segodimo/05android/vncsbs/gradle/libs.versions.toml
e /home/segodimo/05android/vncsbs/app/src/main/java/com/rec/vncsbs/MainActivity.kt


cd /home/segodimo/05android/vncsbs/ | term


./gradlew installDebug && adb shell am start -n com.rec.vncsbs/.MainActivity 



adb shell am start -n com.rec.vncsbs/.MainActivity && adb logcat -c && adb logcat -v threadtime | grep -E "VncClient"



adb shell am start -n com.rec.vncsbs/.MainActivity 
adb shell am force-stop com.rec.vncsbs


adb shell am force-stop com.rec.vncsbs && adb shell am start -n com.rec.vncsbs/.MainActivity 


x0vncserver -display :0 -passwordfile ~/.vnc/passwd -rfbport 5900            

./gradlew installDebug && adb shell am start -n com.rec.vncsbs/.MainActivity && adb logcat -c && adb logcat -v threadtime | grep -E "VncClient"
adb shell am start -n com.rec.vncsbs/.MainActivity && adb logcat -c && adb logcat -v threadtime | grep -E "VncClient"
adb logcat -c && adb logcat -v threadtime | grep -E "PixelFormat|primeiros pixels|rectangle 0"
"VncClient"


adb logcat -d -v threadtime | grep -E "framebuffer completo|rectangle 3[0-9] copiado"
adb logcat -d -v threadtime | grep -E "iniciando rectangle|rectangle 3[0-9]|framebuffer completo"
adb logcat -d -v threadtime | grep -E "header 3[0-5]|rectangle 3[0-5]"
adb logcat -d -v threadtime | grep -E "rectangleCount bytes|rectangles =|header 3[0-5]|rectangle 3[0-5]"

adb logcat -c
adb logcat -v threadtime | grep -E "VncClient: (rectangles|iniciando rectangle|enviando framebuffer)"

adb logcat -c && adb logcat -v threadtime | grep -E "VncScreen|VncClient"
adb logcat -c && adb logcat -v threadtime | grep -E "VncViewModel|VncClient"

grep -E "SbsRemoteView|VncViewModel|VncClient"

adb logcat -d -v threadtime | grep "SbsRemoteView" | tail -30

adb logcat -d -v threadtime | grep "SbsRemoteView: BITMAP"


adb shell am force-stop com.rec.vncsbs && adb shell am start -n com.rec.vncsbs/.MainActivity 
adb shell am force-stop com.rec.vncsbs && adb shell am start -n com.rec.vncsbs/.MainActivity 

adb logcat -d -v threadtime | grep -E "VncViewModel: (RECEBEU|STATE ATUALIZADO)|SbsRemoteView: BITMAP" | tail -60
adb logcat -c && adb logcat -v threadtime | grep -E "VncClient|VncViewModel"
adb logcat -c && adb logcat -v threadtime | grep -E "framebuffer completo|enviando framebuffer"
adb logcat -c && adb logcat -v threadtime | grep -E "rectangles total|FramebufferUpdateRequest"


```

x0vncserver \
    -display :0 \
    -passwordfile /home/segodimo/.vnc/passwd \
    -rfbport 5900 \
    -CompareFB 1


---
$$$$
@@@@
