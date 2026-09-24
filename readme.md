
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

```bash


e /home/segodimo/05android/vncsbs/readme.md
e /home/segodimo/05android/vncsbs/app/build.gradle.kts
e /home/segodimo/05android/vncsbs/gradle/libs.versions.toml
e /home/segodimo/05android/vncsbs/app/src/main/java/com/rec/vncsbs/MainActivity.kt


cd /home/segodimo/05android/vncsbs/ | term


./gradlew installDebug && adb shell am start -n com.rec.vncsbs/.MainActivity 

```

---
$$$$
