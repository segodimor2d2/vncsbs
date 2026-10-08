# Benchmark VNC via USB com vídeo — 2026-10-08

Servidor: 192.168.31.127:5900. Android Motorola Edge 70 Pro, USB serial 0091166762. Framebuffer 1920×1080.
Caminho: Android 127.0.0.1:15900 → adb reverse por USB → ponte TCP no computador → servidor VNC local.
Usuário informou que iniciaria o vídeo; aguardados 15 segundos antes da rodada. 10 segundos por modo após 2 segundos de aquecimento.

| Modo | MB recebidos | Mbit/s | Atualizações/s | Economia versus RAW dos pixels recebidos |
|---|---:|---:|---:|---:|
| RAW | 267.21 | 213.77 | 7.2 | -0.0% |
| Tight-0 | 11.06 | 8.85 | 67.2 | 98.9% |
| Tight-6 | 24.60 | 19.68 | 114.1 | 95.9% |
| Tight-9 | 146.17 | 116.93 | 10.6 | 70.9% |

Todos os modos passaram; Tight sem fallback RAW.
Atualizações/s são updates completos do protocolo, não FPS do vídeo ou da tela do app. O benchmark decodifica mas não renderiza na UI.
Bytes incluem cabeçalhos RFB; excluem overhead TCP/IP e ADB.
Qualidade 0: maior compressão (98,9% versus bytes RAW equivalentes dos retângulos recebidos). Qualidade 6: maior taxa de updates nesta rodada.
Amostras consecutivas com conteúdo não repetido; não comparar diretamente com a rodada Wi-Fi anterior, cuja carga foi diferente.
Esta é uma medição de tráfego e recepção/decodificação do cliente, não de capacidade máxima do cabo nem de latência de interação.
