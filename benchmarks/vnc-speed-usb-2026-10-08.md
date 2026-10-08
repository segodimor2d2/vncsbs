# Benchmark VNC via USB — 2026-10-08

Transporte: ADB USB, serial 0091166762. Motorola Edge 70 Pro. Framebuffer 1920×1080.
Caminho: Android 127.0.0.1:15900 → adb reverse USB → ponte TCP local → servidor 192.168.31.127:5900.
O servidor escuta no IP LAN, mas o trecho no computador é local e não passa pelo rádio Wi-Fi.
10 segundos por modo após 2 segundos de aquecimento. Conteúdo não controlado.

| Modo | MB recebidos | Mbit/s | Atualizações/s | Pixels RAW equivalentes (MB) |
|---|---:|---:|---:|---:|
| RAW | 0.34 | 0.27 | 11.7 | 0.33 |
| Tight-0 | 1.80 | 1.44 | 78.3 | 16.65 |
| Tight-6 | 1.46 | 1.17 | 65.9 | 8.65 |
| Tight-9 | 2.40 | 1.92 | 6.2 | 9.93 |

Todos os modos conectaram e o teste passou. Modos Tight receberam apenas retângulos Tight, sem fallback RAW.
Atualizações/s são updates completos recebidos pelo cliente; não são FPS de vídeo nem FPS exibidos. O teste não renderiza na UI.
Volume de pixels alterados muito menor que na rodada Wi-Fi. Não comparar diretamente os totais ou inferir capacidade máxima do USB.
Para comparar USB com Wi-Fi, reproduzir o mesmo trecho de vídeo em loop e repetir as duas rodadas sob a mesma carga.
Bytes incluem cabeçalhos RFB e excluem overhead TCP/IP e ADB. Updates ainda incompletos no fim da janela não entram na contagem.
