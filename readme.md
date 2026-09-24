
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


x0vncserver -display :0 -passwordfile ~/.vnc/passwd -rfbport 5900            

./gradlew installDebug && adb shell am start -n com.rec.vncsbs/.MainActivity && adb logcat -c && adb logcat -v threadtime | grep -E "VncClient"
adb shell am start -n com.rec.vncsbs/.MainActivity && adb logcat -c && adb logcat -v threadtime | grep -E "VncClient"
adb logcat -c && adb logcat -v threadtime | grep -E "VncClient"


adb logcat -d -v threadtime | grep -E "framebuffer completo|rectangle 3[0-9] copiado"
adb logcat -d -v threadtime | grep -E "iniciando rectangle|rectangle 3[0-9]|framebuffer completo"
adb logcat -d -v threadtime | grep -E "header 3[0-5]|rectangle 3[0-5]"
adb logcat -d -v threadtime | grep -E "rectangleCount bytes|rectangles =|header 3[0-5]|rectangle 3[0-5]"

```

---
$$$$
@@@@
