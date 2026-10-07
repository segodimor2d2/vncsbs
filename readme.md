
---
$$$$

No Ubuntu, o `x0vncserver` também vem no pacote `tigervnc-standalone-server`:

```bash
sudo apt update
sudo apt install tigervnc-standalone-server
```

Depois, crie a senha VNC (separada da senha do seu usuário):

```bash
mkdir -p ~/.vnc
vncpasswd
```
(no TigerVNC do Ubuntu, o `vncpasswd` já salva automaticamente em `~/.vnc/passwd`)

E rode o servidor apontando pro display ativo:

```bash
x0vncserver -display :0 -passwordfile ~/.vnc/passwd
```

Pra confirmar qual display está ativo, se `:0` não funcionar:
```bash
echo $DISPLAY
```

**Atenção com Wayland:** desde o Ubuntu 22.04, o GNOME roda em **Wayland por padrão**, e o `x0vncserver` não funciona nesse caso (depende de X11). Verifique com:
```bash
echo $XDG_SESSION_TYPE
```
Se retornar `wayland`, você tem duas opções:
1. Trocar a sessão pra **"Ubuntu on Xorg"** na tela de login (ícone de engrenagem antes de logar) — aí o X11 volta a rodar e o `x0vncserver` funciona normal.
2. Usar o **compartilhamento de tela nativo do GNOME** (Configurações > Compartilhamento > Compartilhamento de Tela), que já usa um protocolo compatível com Wayland por baixo dos panos — não precisa instalar nada extra nesse caso.

**Rodar automaticamente com systemd (opcional):**
```bash
mkdir -p ~/.config/systemd/user
cat > ~/.config/systemd/user/x0vncserver.service << 'EOF'
[Unit]
Description=x0vncserver

[Service]
ExecStart=/usr/bin/x0vncserver -display :0 -passwordfile %h/.vnc/passwd
Restart=on-failure

[Install]
WantedBy=default.target
EOF

systemctl --user daemon-reload
systemctl --user enable --now x0vncserver.service
```

Depois é só conectar de outra máquina com um cliente VNC (Remmina, TigerVNC Viewer, etc.) no IP da máquina Ubuntu, porta `5900`.

---
$$$$

No Arch, `x0vncserver` vem junto com o pacote `tigervnc` (está nos repositórios oficiais, não precisa de AUR):

```bash
sudo pacman -S tigervnc
```

Depois de instalado, pra usar você precisa de um arquivo de senha VNC (separado da sua senha de usuário do sistema):

```bash
# cria a senha VNC (vai pedir pra digitar e confirmar)
mkdir -p ~/.vnc
vncpasswd ~/.vnc/passwd
```

E então roda o servidor apontando pro seu display ativo (geralmente `:0`):

```bash
x0vncserver -display :0 -passwordfile ~/.vnc/passwd
```

Pra descobrir qual display está ativo, se `:0` não funcionar:
```bash
echo $DISPLAY
```

**Se você estiver usando Wayland** (comum em instalações novas de Arch com GNOME/KDE), o `x0vncserver` **não vai funcionar**, porque ele depende do X11. Nesse caso, confirme primeiro com:
```bash
echo $XDG_SESSION_TYPE
```
Se retornar `wayland`, você vai precisar do `wayvnc` em vez disso (também disponível via `pacman -S wayvnc`), que tem uso bem parecido.

**Deixar rodando automaticamente (opcional):**
Se quiser que o servidor suba sozinho com o sistema, dá pra criar um serviço systemd de usuário:

```bash
mkdir -p ~/.config/systemd/user
cat > ~/.config/systemd/user/x0vncserver.service << 'EOF'
[Unit]
Description=x0vncserver

[Service]
ExecStart=/usr/bin/x0vncserver -display :0 -passwordfile %h/.vnc/passwd
Restart=on-failure

[Install]
WantedBy=default.target
EOF

systemctl --user daemon-reload
systemctl --user enable --now x0vncserver.service
```

Depois disso, de qualquer outro computador você conecta com um cliente VNC (ex: `TigerVNC Viewer`, `Remmina`) apontando pro IP da máquina na porta `5900`.


---
$$$$

Aqui tem um porém importante: **o `x0vncserver` não existe pra Windows** — ele depende do X11, que é o sistema de janelas do Linux/Unix. O Windows não tem X11 nativamente, então esse binário específico simplesmente não roda lá.

O que existe no Windows é o **TigerVNC Server para Windows**, mas ele usa outro componente chamado **`winvnc.exe`**, que cumpre a mesma função (compartilhar a tela real do Windows via VNC), só que é implementado em cima da API gráfica do Windows em vez de X11.

**Como instalar:**

1. Baixe o instalador do TigerVNC pra Windows:
   - Site oficial: https://tigervnc.org/ → seção de downloads → `.exe` ou `.msi` mais recente pra Windows.

2. Rode o instalador e selecione o componente **"TigerVNC Server"** (o pacote Windows geralmente deixa escolher servidor, viewer, ou ambos).

3. Depois de instalado, procure por **"VNC Server"** no menu Iniciar (ele instala como serviço do Windows, chamado `winvnc`).

4. Configure a senha e as opções pela interface gráfica que abre (TigerVNC no Windows tem um painel de configuração visual, bem mais simples que editar arquivo de config como no Linux).

5. Se preferir rodar como serviço (iniciar com o Windows automaticamente), a própria instalação geralmente já oferece essa opção durante o setup, ou você ativa depois em **Serviços do Windows** (`services.msc`) procurando por "TigerVNC Server".

**Resumindo a equivalência entre plataformas:**
| Sistema | Componente |
|---|---|
| Linux (X11) | `x0vncserver` |
| Linux (Wayland) | `wayvnc` |
| Windows | `winvnc.exe` (TigerVNC Server) |

Depois de instalado e rodando, você conecta de qualquer outro computador do mesmo jeito: cliente VNC (TigerVNC Viewer, Remmina, etc.) apontando pro IP da máquina Windows, porta `5900`.

---
$$$$

# Iniciar TigerVNC


```bash

x0vncserver -display :0 -passwordfile /home/segodimo/.vnc/passwd -rfbport 5900 -CompareFB 1

```


```bash

  x0vncserver \
    -display :0 \
    -passwordfile /home/segodimo/.vnc/passwd \
    -rfbport 5900 \
    -CompareFB 1

```

# VIA LOCAL

```bash

  x0vncserver \
    -display :0 \
    -passwordfile /home/segodimo/.vnc/passwd \
    -rfbport 5900 \
    -localhost \
    -CompareFB 1 \
    -PollingCycle 5 \
    -MaxProcessorUsage 100 \
    -FrameRate 60 

```
# -CompareFB 1 \
# -PollingCycle 1 \
# -FrameRate 120

# VIA USB

```bash

  x0vncserver \
    -display :0 \
    -passwordfile /home/segodimo/.vnc/passwd \
    -rfbport 5900 \
    -localhost \
    -CompareFB 1 \
    -PollingCycle 5 \
    -MaxProcessorUsage 100 \
    -FrameRate 30 \

```
