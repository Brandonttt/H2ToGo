# Genera htogo-mockups/index.html consolidando las pantallas de htogo-mockups/screens.
# Uso desde la raiz del repo:  powershell -File htogo-mockups/build.ps1

$ErrorActionPreference = 'Stop'

$screensDir = Join-Path $PSScriptRoot 'screens'
$outFile    = Join-Path $PSScriptRoot 'index.html'

if (-not (Test-Path $screensDir)) {
  throw "No se encontro la carpeta de pantallas: $screensDir"
}

function Get-Category($name) {
  switch -Regex ($name) {
    '^01-'                       { 'General';      return }
    '^0[234][a-z]?-'             { 'Auth';         return }
    '^(05-|06-|07-|08-|09-|1[0-3]-)' { 'Cliente';      return }
    '^(14-|15-|16-|17-|18-|19-)' { 'Repartidor';   return }
    '^(20-|21-)'                 { 'Purificadora'; return }
    '^22-'                       { 'Admin';        return }
    default                      { 'Otros' }
  }
}

function Get-Type($name, $content) {
  # Tipo "web" = sin frame phone (layout amplio); "phone" = frame Android.
  if ($name -match '^22-' -or $content -notmatch 'class="device"') { return 'web' }
  return 'phone'
}

function Get-NiceTitle($file, $rawTitle) {
  if ($rawTitle) {
    $t = $rawTitle -replace '^HToGo\s*[·•\-]\s*', ''
    return $t.Trim()
  }
  return [System.IO.Path]::GetFileNameWithoutExtension($file)
}

$screens = Get-ChildItem -Path $screensDir -Filter *.html | Sort-Object Name

$entries = New-Object System.Collections.Generic.List[object]
foreach ($s in $screens) {
  $content = [System.IO.File]::ReadAllText($s.FullName, [System.Text.Encoding]::UTF8)
  $title = $null
  if ($content -match '<title>([^<]+)</title>') { $title = $matches[1] }
  $bytes = [System.Text.Encoding]::UTF8.GetBytes($content)
  $b64   = [Convert]::ToBase64String($bytes)
  $entries.Add([PSCustomObject]@{
    file     = $s.Name
    title    = (Get-NiceTitle $s.Name $title)
    category = (Get-Category $s.Name)
    type     = (Get-Type $s.Name $content)
    b64      = $b64
  }) | Out-Null
}

$jsonData = $entries | ConvertTo-Json -Compress -Depth 4

$template = @'
<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8" />
<title>HToGo - Mockups</title>
<meta name="viewport" content="width=device-width,initial-scale=1" />
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Roboto:wght@400;500;600;700&display=swap" rel="stylesheet">
<link href="https://fonts.googleapis.com/icon?family=Material+Icons" rel="stylesheet">
<style>
  :root{
    --primary:#0077B6; --primary-light:#00B4D8; --primary-dark:#03045E;
    --primary-soft:#CAF0F8;
    --bg:#0b1220; --surface:#111a2e; --surface-2:#0f172a;
    --text:#E2E8F0; --text-2:#94A3B8; --text-3:#64748B;
    --outline:#1e293b;
    --frame-w:464px;  /* 412 device + 24*2 padding del body interno */
    --frame-h:920px;  /* 868 device + 24*2 padding del body interno */
    --scale:0.46;
    --web-w:1280px;   /* viewport simulado para layouts web (admin) */
    --web-h:820px;
    --web-scale:0.30;
  }
  *{box-sizing:border-box}
  html,body{margin:0;padding:0;background:var(--bg);color:var(--text);
    font-family:'Roboto',system-ui,sans-serif;}
  a{color:var(--primary-light);text-decoration:none}
  a:hover{text-decoration:underline}

  header{
    position:sticky;top:0;z-index:5;
    background:rgba(11,18,32,.85);backdrop-filter:blur(12px);
    border-bottom:1px solid var(--outline);
  }
  .header-inner{
    max-width:1400px;margin:0 auto;padding:18px 28px;
    display:flex;align-items:center;gap:16px;flex-wrap:wrap;
  }
  .brand{display:flex;align-items:center;gap:10px;}
  .brand-mark{
    width:36px;height:36px;border-radius:50%;background:var(--primary);
    display:flex;align-items:center;justify-content:center;color:#fff;
  }
  .brand-mark .material-icons{font-size:20px}
  .brand-name{font-weight:700;font-size:20px;letter-spacing:-.2px;color:#fff;}
  .brand-sub{font-size:12px;color:var(--text-2);margin-left:4px;}

  .filters{display:flex;gap:8px;flex-wrap:wrap;margin-left:auto;}
  .filter{
    border:1px solid var(--outline);background:transparent;color:var(--text-2);
    padding:8px 14px;border-radius:99px;font:600 13px/1 'Roboto',sans-serif;
    cursor:pointer;transition:all .15s;
  }
  .filter:hover{color:#fff;border-color:#334155;}
  .filter.active{
    background:var(--primary);border-color:var(--primary);color:#fff;
    box-shadow:0 4px 14px rgba(0,119,182,.35);
  }
  .filter .count{
    margin-left:6px;background:rgba(255,255,255,.18);
    padding:1px 7px;border-radius:99px;font-size:11px;
  }
  .filter:not(.active) .count{background:rgba(255,255,255,.06);}

  main{max-width:1400px;margin:0 auto;padding:28px;}
  .intro{
    color:var(--text-2);font-size:14px;line-height:1.6;
    margin-bottom:24px;
  }
  .intro strong{color:#fff;font-weight:600;}

  .grid{
    display:grid;
    grid-template-columns:repeat(auto-fill,minmax(calc(var(--frame-w) * var(--scale) + 32px),1fr));
    gap:24px;
  }
  .card{
    background:var(--surface);border:1px solid var(--outline);
    border-radius:18px;padding:14px;cursor:pointer;
    transition:transform .15s, border-color .15s, box-shadow .15s;
    display:flex;flex-direction:column;gap:10px;
  }
  /* Cards de tipo "web" ocupan el ancho de 2 columnas y son apaisadas */
  .card[data-type="web"]{
    grid-column:span 2;
  }
  @media (min-width: 1100px){
    .card[data-type="web"]{ grid-column:span 3; }
  }
  .card:hover{
    transform:translateY(-3px);border-color:#334155;
    box-shadow:0 12px 28px rgba(0,0,0,.45);
  }
  .meta{display:flex;align-items:center;justify-content:space-between;}
  .cat{
    font-size:11px;font-weight:600;letter-spacing:.4px;text-transform:uppercase;
    padding:3px 9px;border-radius:99px;
    background:rgba(0,180,216,.12);color:var(--primary-light);
  }
  .cat.Auth        {background:rgba(245,158,11,.14);color:#FBBF24;}
  .cat.Cliente     {background:rgba(0,180,216,.12);color:#7DD3FC;}
  .cat.Repartidor  {background:rgba(16,185,129,.14);color:#6EE7B7;}
  .cat.Purificadora{background:rgba(139,92,246,.16);color:#C4B5FD;}
  .cat.Admin       {background:rgba(239,68,68,.14);color:#FCA5A5;}
  .cat.General     {background:rgba(148,163,184,.14);color:#CBD5E1;}
  .num{font-size:11px;color:var(--text-3);font-variant-numeric:tabular-nums;}

  .thumb{
    width:calc(var(--frame-w) * var(--scale));
    height:calc(var(--frame-h) * var(--scale));
    position:relative;overflow:hidden;border-radius:14px;
    margin:0 auto;background:#0f172a;
  }
  .thumb iframe{
    width:var(--frame-w);height:var(--frame-h);border:0;
    transform:scale(var(--scale));transform-origin:top left;
    pointer-events:none;
  }
  /* Variante web: thumbnail apaisada (sin frame phone) */
  .card[data-type="web"] .thumb{
    width:100%;
    aspect-ratio:1280/820;
    height:auto;
    border-radius:8px;
    border:1px solid #1e293b;
  }
  .card[data-type="web"] .thumb iframe{
    width:var(--web-w);height:var(--web-h);
    transform:scale(var(--web-scale));
    transform-origin:top left;
  }
  /* Recalcula la escala web para llenar el ancho disponible del thumb */
  .card[data-type="web"] .thumb{ container-type:inline-size; }
  @container (min-width: 1px){
    .card[data-type="web"] .thumb iframe{
      transform:scale(calc(100cqw / var(--web-w)));
    }
  }
  .thumb .skeleton{
    position:absolute;inset:0;display:flex;align-items:center;justify-content:center;
    background:linear-gradient(180deg,#0f172a,#0b1220);color:var(--text-3);
    font-size:12px;
  }
  .thumb .skeleton .material-icons{font-size:36px;opacity:.4;animation:pulse 1.4s ease-in-out infinite;}
  @keyframes pulse{0%,100%{opacity:.2}50%{opacity:.6}}

  .title{
    font-size:14px;font-weight:600;color:#fff;
    text-align:center;line-height:1.35;
    overflow:hidden;text-overflow:ellipsis;white-space:nowrap;
  }
  .file{
    font-size:11px;color:var(--text-3);text-align:center;font-family:'Roboto Mono',ui-monospace,monospace;
  }

  /* Modal */
  .modal{
    position:fixed;inset:0;background:rgba(2,6,23,.88);
    display:none;align-items:center;justify-content:center;z-index:50;
    padding:16px;backdrop-filter:blur(8px);
  }
  .modal.open{display:flex;}
  .modal-card{
    position:relative;display:flex;flex-direction:column;align-items:center;gap:10px;
    max-height:100%;
  }
  .modal-head{
    display:flex;align-items:center;gap:14px;color:#fff;
    background:rgba(15,23,42,.7);padding:8px 14px;border-radius:99px;
    border:1px solid var(--outline);
  }
  .modal-head .t{font-weight:600;font-size:13px;}
  .modal-head .f{font-size:12px;color:var(--text-2);}
  /* Espacio reservado para chrome del modal (head + gaps + padding) */
  .modal{ --chrome-h:80px; }
  /* Wrap por defecto = phone. Se ajusta a la altura disponible. */
  .modal-frame-wrap{
    --nat-w:464; --nat-h:920;
    height:min(calc(var(--nat-h) * 1px), calc(100vh - var(--chrome-h)));
    aspect-ratio:var(--nat-w) / var(--nat-h);
    border-radius:46px;overflow:hidden;position:relative;
    box-shadow:0 30px 80px rgba(0,0,0,.6);
    background:#0f172a;
  }
  .modal-frame-wrap iframe{
    width:calc(var(--nat-w) * 1px);height:calc(var(--nat-h) * 1px);
    border:0;display:block;transform-origin:top left;
  }
  /* Modal tipo web: laptop frame apaisado */
  .modal[data-type="web"] .modal-frame-wrap{
    --nat-w:1280; --nat-h:820;
    height:auto;
    width:min(calc(var(--nat-w) * 1px), calc(100vw - 32px), calc((100vh - var(--chrome-h)) * 1280 / 820));
    border-radius:14px;
    border:1px solid #1e293b;
    background:#fff;
    box-shadow:0 30px 80px rgba(0,0,0,.6);
  }
  .modal-close{
    position:absolute;top:-12px;right:-12px;
    width:40px;height:40px;border-radius:50%;border:0;cursor:pointer;
    background:#fff;color:#0f172a;font-size:20px;
    display:flex;align-items:center;justify-content:center;
    box-shadow:0 6px 18px rgba(0,0,0,.4);
  }
  .modal-nav{
    position:absolute;top:50%;transform:translateY(-50%);
    width:48px;height:48px;border-radius:50%;border:0;cursor:pointer;
    background:rgba(255,255,255,.12);color:#fff;
    display:flex;align-items:center;justify-content:center;
    backdrop-filter:blur(6px);
    transition:background .15s;
  }
  .modal-nav:hover{background:rgba(255,255,255,.22);}
  .modal-nav.prev{left:-72px;}
  .modal-nav.next{right:-72px;}

  footer{
    max-width:1400px;margin:0 auto;padding:24px 28px 40px;
    color:var(--text-3);font-size:12px;text-align:center;
    border-top:1px solid var(--outline);margin-top:32px;
  }

  @media (max-width: 720px){
    :root{ --scale:0.42; }
    .header-inner{padding:14px 16px;}
    main{padding:16px;}
    .filters{width:100%;order:2;flex-wrap:wrap;padding-bottom:4px;}
    .filter{flex:0 0 auto;}
    .grid{grid-template-columns:1fr;}
    .card[data-type="web"]{ grid-column:auto; }
    .modal-nav.prev{left:8px;}
    .modal-nav.next{right:8px;}
  }
</style>
</head>
<body>
  <header>
    <div class="header-inner">
      <div class="brand">
        <div class="brand-mark"><span class="material-icons">water_drop</span></div>
        <div>
          <div class="brand-name">HToGo <span class="brand-sub">Mockups</span></div>
        </div>
      </div>
      <div class="filters" id="filters"></div>
    </div>
  </header>

  <main>
    <p class="intro">
      Galer&iacute;a de pantallas de <strong>HToGo</strong> (marketplace de garrafones),
      portadas desde el c&oacute;digo Kotlin/Compose de la app Android. Cada tarjeta es la
      pantalla real renderizada; haz clic para verla a tama&ntilde;o completo. Filtra por
      rol arriba.
    </p>
    <div class="grid" id="grid"></div>
  </main>

  <footer>
    Desarrollada por Gael Alejandro M&aacute;rquez Rodr&iacute;guez y Brandon Trejo Hern&aacute;ndez
  </footer>

  <div class="modal" id="modal" role="dialog" aria-modal="true">
    <div class="modal-card">
      <button class="modal-nav prev" id="mPrev" aria-label="Anterior"><span class="material-icons">chevron_left</span></button>
      <div class="modal-head">
        <span class="t" id="mTitle"></span>
        <span class="f" id="mFile"></span>
      </div>
      <div class="modal-frame-wrap"><iframe id="mFrame" sandbox="allow-scripts allow-same-origin"></iframe></div>
      <button class="modal-close" id="mClose" aria-label="Cerrar">&times;</button>
      <button class="modal-nav next" id="mNext" aria-label="Siguiente"><span class="material-icons">chevron_right</span></button>
    </div>
  </div>

<script id="screens-data" type="application/json">__SCREENS_DATA__</script>
<script>
  const SCREENS = JSON.parse(document.getElementById('screens-data').textContent);

  // Decodifica base64 (UTF-8 safe)
  function decodeB64(b64){
    const bin = atob(b64);
    const bytes = new Uint8Array(bin.length);
    for (let i=0;i<bin.length;i++) bytes[i] = bin.charCodeAt(i);
    return new TextDecoder('utf-8').decode(bytes);
  }

  // Construye el grid
  const grid = document.getElementById('grid');
  const cards = [];
  SCREENS.forEach((s, i) => {
    const card = document.createElement('div');
    card.className = 'card';
    card.dataset.category = s.category;
    card.dataset.type     = s.type || 'phone';
    card.dataset.index = i;
    const skelIcon = card.dataset.type === 'web' ? 'desktop_windows' : 'smartphone';
    card.innerHTML = `
      <div class="meta">
        <span class="cat ${s.category}">${s.category}</span>
        <span class="num">${String(i+1).padStart(2,'0')} / ${SCREENS.length}</span>
      </div>
      <div class="thumb">
        <div class="skeleton"><span class="material-icons">${skelIcon}</span></div>
        <iframe sandbox="allow-scripts allow-same-origin" loading="lazy" title="${s.title}"></iframe>
      </div>
      <div class="title" title="${s.title}">${s.title}</div>
      <div class="file">${s.file}</div>
    `;
    card.addEventListener('click', () => openModal(i));
    grid.appendChild(card);
    cards.push(card);
  });

  // Carga progresiva: pinta los iframes en chunks para no bloquear el render inicial.
  // Los recursos compartidos (Roboto, Material Icons, Tailwind) se cachean tras la primera carga.
  function loadIframe(card){
    const idx = +card.dataset.index;
    const iframe = card.querySelector('iframe');
    if (iframe.dataset.loaded) return;
    iframe.dataset.loaded = '1';
    iframe.addEventListener('load', () => {
      const sk = card.querySelector('.skeleton');
      if (sk) sk.style.display = 'none';
    }, { once:true });
    iframe.srcdoc = decodeB64(SCREENS[idx].b64);
  }
  (function loadInChunks(i){
    if (i >= cards.length) return;
    const end = Math.min(i + 4, cards.length);
    for (let j = i; j < end; j++) loadIframe(cards[j]);
    setTimeout(() => loadInChunks(end), 80);
  })(0);

  // Filtros
  const cats = ['Todas', ...new Set(SCREENS.map(s => s.category))];
  const filtersEl = document.getElementById('filters');
  cats.forEach(cat => {
    const btn = document.createElement('button');
    btn.className = 'filter' + (cat === 'Todas' ? ' active' : '');
    btn.dataset.cat = cat;
    const count = cat === 'Todas' ? SCREENS.length : SCREENS.filter(s => s.category === cat).length;
    btn.innerHTML = `${cat}<span class="count">${count}</span>`;
    btn.addEventListener('click', () => {
      document.querySelectorAll('.filter').forEach(b => b.classList.toggle('active', b === btn));
      cards.forEach(c => {
        const show = cat === 'Todas' || c.dataset.category === cat;
        c.style.display = show ? '' : 'none';
      });
    });
    filtersEl.appendChild(btn);
  });

  // Modal
  const modal      = document.getElementById('modal');
  const mTitle     = document.getElementById('mTitle');
  const mFile      = document.getElementById('mFile');
  const mFrame     = document.getElementById('mFrame');
  const mClose     = document.getElementById('mClose');
  const mPrev      = document.getElementById('mPrev');
  const mNext      = document.getElementById('mNext');
  let modalIndex   = -1;

  // Calcula la escala que el iframe necesita para llenar el wrap (manteniendo proporcion)
  function applyModalScale(){
    if (!modal.classList.contains('open')) return;
    const isWeb = modal.dataset.type === 'web';
    const natW = isWeb ? 1280 : 464;
    const natH = isWeb ? 820  : 920;
    const wrap = mFrame.parentElement;
    const w = wrap.clientWidth, h = wrap.clientHeight;
    if (!w || !h) return;
    const sc = Math.min(w / natW, h / natH);
    mFrame.style.transform = `scale(${sc})`;
  }
  function openModal(i){
    modalIndex = i;
    const s = SCREENS[i];
    mTitle.textContent = s.title;
    mFile.textContent  = s.file;
    mFrame.srcdoc      = decodeB64(s.b64);
    modal.dataset.type = s.type || 'phone';
    modal.classList.add('open');
    document.body.style.overflow = 'hidden';
    requestAnimationFrame(applyModalScale);
  }
  function closeModal(){
    modal.classList.remove('open');
    mFrame.srcdoc = '';
    mFrame.style.transform = '';
    document.body.style.overflow = '';
  }
  window.addEventListener('resize', applyModalScale);
  function nav(delta){
    if (modalIndex < 0) return;
    const visible = cards.filter(c => c.style.display !== 'none')
                         .map(c => +c.dataset.index);
    if (!visible.length) return;
    const pos = visible.indexOf(modalIndex);
    const next = visible[(pos + delta + visible.length) % visible.length];
    openModal(next);
  }
  mClose.addEventListener('click', closeModal);
  mPrev .addEventListener('click', () => nav(-1));
  mNext .addEventListener('click', () => nav(+1));
  modal.addEventListener('click', (e) => { if (e.target === modal) closeModal(); });
  document.addEventListener('keydown', (e) => {
    if (!modal.classList.contains('open')) return;
    if (e.key === 'Escape')      closeModal();
    if (e.key === 'ArrowLeft')   nav(-1);
    if (e.key === 'ArrowRight')  nav(+1);
  });
</script>
</body>
</html>
'@

# Evita que un eventual "</script>" dentro del JSON cierre el script tag.
# JSON.parse acepta "<\/" como "</".
$jsonSafe = $jsonData -replace '</', '<\/'
$html = $template.Replace('__SCREENS_DATA__', $jsonSafe)

[System.IO.File]::WriteAllText($outFile, $html, [System.Text.UTF8Encoding]::new($false))

Write-Host ("OK: " + $outFile)
Write-Host ("Pantallas: " + $entries.Count)
foreach ($e in $entries) {
  Write-Host ("  [{0,-12}] {1,-40} {2}" -f $e.category, $e.file, $e.title)
}
