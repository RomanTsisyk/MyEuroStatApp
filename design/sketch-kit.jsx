/* eslint-disable */
// Eurostat wireframe — reusable sketch primitives.
// All low-fi: dashed boxes, squiggly chart strokes, hand-drawn iconography.

const SK = {
  paper: '#faf8f3',
  paper2: '#f3efe6',
  ink: '#1f1d1a',
  ink2: '#3a352e',
  muted: '#6b6359',
  muted2: '#a39a8d',
  accent: '#2f4969',
  accentSoft: '#c6d0dc',
  warn: '#b06a3a',
};

// ── Tiny hand-drawn icon set ─────────────────────────────────────
function SkIcon({ name, size = 18, stroke = SK.ink, weight = 1.6 }) {
  const sw = weight;
  const common = { width: size, height: size, viewBox: '0 0 24 24', fill: 'none',
    stroke, strokeWidth: sw, strokeLinecap: 'round', strokeLinejoin: 'round' };
  switch (name) {
    case 'home':
      return <svg {...common}><path d="M3 11l9-7 9 7v9a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z" /></svg>;
    case 'coin':
      return <svg {...common}><circle cx="12" cy="12" r="8" /><path d="M9 9c0-1.5 1.5-2 3-2s3 .5 3 2-1 2-3 2-3 1-3 2.2 1.5 2 3 2 3-.5 3-2M12 5v2M12 17v2" /></svg>;
    case 'people':
      return <svg {...common}><circle cx="9" cy="8" r="3" /><circle cx="17" cy="9" r="2.4" /><path d="M3 19c0-3 2.5-5 6-5s6 2 6 5M14.5 19c.5-2 2-3.5 4-3.5s3 .8 3.5 2.5" /></svg>;
    case 'leaf':
      return <svg {...common}><path d="M20 4c-9 0-16 4-16 12 0 2 1 4 3 4 7 0 13-6 13-13" /><path d="M4 20c4-5 8-7 13-9" /></svg>;
    case 'dots':
      return <svg {...common}><circle cx="6" cy="12" r="1.4" /><circle cx="12" cy="12" r="1.4" /><circle cx="18" cy="12" r="1.4" /></svg>;
    case 'search':
      return <svg {...common}><circle cx="11" cy="11" r="6" /><path d="M16 16l4 4" /></svg>;
    case 'close':
      return <svg {...common}><path d="M6 6l12 12M18 6L6 18" /></svg>;
    case 'chev-r':
      return <svg {...common}><path d="M9 6l6 6-6 6" /></svg>;
    case 'chev-l':
      return <svg {...common}><path d="M15 6l-6 6 6 6" /></svg>;
    case 'chev-d':
      return <svg {...common}><path d="M6 9l6 6 6-6" /></svg>;
    case 'plus':
      return <svg {...common}><path d="M12 5v14M5 12h14" /></svg>;
    case 'check':
      return <svg {...common}><path d="M4 12l5 5 11-12" /></svg>;
    case 'arr-up':
      return <svg {...common}><path d="M12 19V5M5 12l7-7 7 7" /></svg>;
    case 'arr-dn':
      return <svg {...common}><path d="M12 5v14M5 12l7 7 7-7" /></svg>;
    case 'refresh':
      return <svg {...common}><path d="M3 12a9 9 0 0 1 16-5l2-2M21 12a9 9 0 0 1-16 5l-2 2M19 5v4h-4M5 19v-4h4" /></svg>;
    case 'globe':
      return <svg {...common}><circle cx="12" cy="12" r="8" /><path d="M4 12h16M12 4c3 3 3 13 0 16M12 4c-3 3-3 13 0 16" /></svg>;
    case 'star':
      return <svg {...common}><path d="M12 4l2.5 5 5.5.8-4 4 1 5.5L12 16.8 7 19.3l1-5.5-4-4 5.5-.8z" /></svg>;
    case 'filter':
      return <svg {...common}><path d="M4 5h16l-6 8v6l-4-2v-4z" /></svg>;
    case 'wifi-off':
      return <svg {...common}><path d="M2 9a16 16 0 0 1 20 0M5 13a10 10 0 0 1 14 0M8 17a5 5 0 0 1 8 0" /><path d="M3 3l18 18" stroke={SK.warn} /></svg>;
    case 'cloud':
      return <svg {...common}><path d="M7 18a4 4 0 0 1-1-7.9A6 6 0 0 1 17.7 10a3.5 3.5 0 0 1 .3 7H7z" /></svg>;
    default:
      return <svg {...common}><rect x="4" y="4" width="16" height="16" rx="2" /></svg>;
  }
}

// ── Sketch primitives ────────────────────────────────────────────
function SkBox({ children, style, dashed = true, thick = false, tight = false, ...p }) {
  return (
    <div className={`sk-box${dashed ? '' : ' solid'}${thick ? ' thick' : ''}${tight ? ' tight' : ''}`} style={style} {...p}>
      {children}
    </div>
  );
}

function SkTag({ children }) { return <span className="sk-tag">{children}</span>; }

function SkPill({ children, variant }) {
  const cls = `sk-pill${variant ? ' ' + variant : ''}`;
  return <span className={cls}>{children}</span>;
}

function SkBtn({ children, primary, full, style }) {
  return (
    <span className={`sk-btn${primary ? ' primary' : ''}`} style={{ width: full ? '100%' : undefined, justifyContent: full ? 'center' : undefined, ...style }}>{children}</span>
  );
}

// ── Bottom tab bar with the brief's 5 modules ────────────────────
function SkTabBar({ active = 'Overview', dark }) {
  const tabs = [
    { id: 'Overview', label: 'Overview', icon: 'home' },
    { id: 'Economy',  label: 'Economy',  icon: 'coin' },
    { id: 'People',   label: 'People',   icon: 'people' },
    { id: 'Climate',  label: 'Climate',  icon: 'leaf' },
    { id: 'More',     label: 'More',     icon: 'dots' },
  ];
  return (
    <div className="sk-tabs" style={{ background: SK.paper }}>
      {tabs.map(t => (
        <div key={t.id} className={`sk-tab${t.id === active ? ' active' : ''}`}>
          <SkIcon name={t.icon} size={18} stroke="currentColor" weight={t.id === active ? 2 : 1.5} />
          <span>{t.label}</span>
        </div>
      ))}
    </div>
  );
}

// ── Squiggly line chart ──────────────────────────────────────────
function SkLineChart({ height = 110, lines = 1, sparse = false, gaps = false, accent = SK.ink, accent2 = SK.accent, accent3 = SK.warn, label, hideAxis = false, points }) {
  // Generate a hand-jittered path
  const W = 280, H = height;
  const inner = { l: 28, r: 8, t: 12, b: 22 };
  const seed = (s) => { let x = s; return () => { x = (x * 9301 + 49297) % 233280; return x / 233280; }; };
  const makePath = (s, jitter = 6, drift = 30) => {
    const r = seed(s);
    const n = 12;
    const xs = Array.from({ length: n }, (_, i) => inner.l + (W - inner.l - inner.r) * (i / (n - 1)));
    const ys = Array.from({ length: n }, (_, i) => H - inner.b - (H - inner.t - inner.b) * (0.2 + r() * 0.5 + (i / n) * (drift / 100)));
    if (gaps) { ys[4] = null; ys[5] = null; }
    let d = '';
    let last = null;
    for (let i = 0; i < n; i++) {
      const y = ys[i];
      if (y == null) { last = null; continue; }
      if (last == null) d += `M${xs[i].toFixed(1)} ${y.toFixed(1)} `;
      else d += `L${xs[i].toFixed(1)} ${y.toFixed(1)} `;
      last = y;
    }
    return d;
  };
  const strokes = [];
  const colors = [accent, accent2, accent3];
  for (let i = 0; i < lines; i++) strokes.push({ d: makePath(11 + i * 23, 5, 20 + i * 8), color: colors[i % colors.length] });

  return (
    <div className="sk-chart" style={{ width: '100%', height }}>
      <svg viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="none" style={{ width: '100%', height: '100%', display: 'block' }}>
        {/* dashed gridlines */}
        {!hideAxis && [0.25, 0.5, 0.75].map(g => (
          <line key={g} x1={inner.l} x2={W - inner.r} y1={inner.t + (H - inner.t - inner.b) * g} y2={inner.t + (H - inner.t - inner.b) * g}
            stroke={SK.ink} strokeOpacity="0.15" strokeDasharray="2 4" />
        ))}
        {/* axis */}
        {!hideAxis && <line x1={inner.l} y1={H - inner.b} x2={W - inner.r} y2={H - inner.b} stroke={SK.ink} strokeWidth="1" />}
        {strokes.map((s, i) => (
          <path key={i} d={s.d} stroke={s.color} strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round"
            style={{ filter: `url(#sk-rough-${i % 3})` }} />
        ))}
        {/* x labels */}
        {!hideAxis && (
          <g fontFamily="JetBrains Mono, ui-monospace" fontSize="9" fill={SK.muted}>
            <text x={inner.l} y={H - 6}>'00</text>
            <text x={(W - inner.r) - 16} y={H - 6}>'24</text>
          </g>
        )}
      </svg>
      {label && <div style={{ position: 'absolute', top: 6, left: 10, fontFamily: 'var(--marker)', fontSize: 14, color: SK.muted }}>{label}</div>}
    </div>
  );
}

// ── Squiggly bar chart ───────────────────────────────────────────
function SkBars({ n = 6, height = 110, label, horizontal = false, accent = SK.ink }) {
  const W = 280, H = height;
  const seed = 11;
  let s = seed;
  const rnd = () => { s = (s * 9301 + 49297) % 233280; return s / 233280; };
  const vals = Array.from({ length: n }, () => 0.25 + rnd() * 0.7);
  const inner = { l: horizontal ? 60 : 22, r: 8, t: 10, b: 22 };
  return (
    <div className="sk-chart" style={{ width: '100%', height }}>
      <svg viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="none" style={{ width: '100%', height: '100%', display: 'block' }}>
        {!horizontal ? vals.map((v, i) => {
          const w = (W - inner.l - inner.r) / n - 6;
          const x = inner.l + i * ((W - inner.l - inner.r) / n) + 3;
          const h = (H - inner.t - inner.b) * v;
          return <rect key={i} x={x} y={H - inner.b - h} width={w} height={h} fill={i === 0 ? accent : SK.paper} stroke={SK.ink} strokeWidth="1.4" />;
        }) : vals.map((v, i) => {
          const rowH = (H - inner.t - inner.b) / n - 4;
          const y = inner.t + i * ((H - inner.t - inner.b) / n) + 2;
          const w = (W - inner.l - inner.r) * v;
          return <g key={i}>
            <text x={6} y={y + rowH * 0.72} fontFamily="Patrick Hand" fontSize="11" fill={SK.muted}>{['DE','FR','IT','ES','NL','BE','PL','SE'][i] || 'XX'}</text>
            <rect x={inner.l} y={y} width={w} height={rowH} fill={i === 0 ? accent : SK.paper} stroke={SK.ink} strokeWidth="1.4" />
          </g>;
        })}
        <line x1={inner.l} y1={H - inner.b} x2={W - inner.r} y2={H - inner.b} stroke={SK.ink} strokeWidth="1" />
      </svg>
      {label && <div style={{ position: 'absolute', top: 6, left: 10, fontFamily: 'var(--marker)', fontSize: 14, color: SK.muted }}>{label}</div>}
    </div>
  );
}

// ── Stacked area chart (sketch) ──────────────────────────────────
function SkArea({ height = 110, label }) {
  const W = 280, H = height;
  const inner = { l: 24, r: 8, t: 10, b: 22 };
  const baseline = H - inner.b;
  const top = inner.t;
  const layers = [0.25, 0.55, 0.8];
  const colors = ['rgba(47,73,105,0.6)', 'rgba(176,106,58,0.45)', 'rgba(31,29,26,0.18)'];
  const path = (frac, jitter) => {
    let d = `M${inner.l} ${baseline} `;
    for (let i = 0; i <= 10; i++) {
      const x = inner.l + (W - inner.l - inner.r) * (i / 10);
      const y = baseline - (baseline - top) * frac * (0.7 + 0.3 * Math.sin(i + jitter));
      d += `L${x.toFixed(1)} ${y.toFixed(1)} `;
    }
    d += `L${W - inner.r} ${baseline} Z`;
    return d;
  };
  return (
    <div className="sk-chart" style={{ width: '100%', height }}>
      <svg viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="none" style={{ width: '100%', height: '100%', display: 'block' }}>
        {layers.map((f, i) => (
          <path key={i} d={path(f, i * 1.2)} fill={colors[i]} stroke={SK.ink} strokeWidth="1.2" />
        ))}
        <line x1={inner.l} y1={baseline} x2={W - inner.r} y2={baseline} stroke={SK.ink} strokeWidth="1" />
      </svg>
      {label && <div style={{ position: 'absolute', top: 6, left: 10, fontFamily: 'var(--marker)', fontSize: 14, color: SK.muted }}>{label}</div>}
    </div>
  );
}

// ── Donut ────────────────────────────────────────────────────────
function SkDonut({ size = 90, label }) {
  const r = size / 2 - 6;
  const cx = size / 2, cy = size / 2;
  const arcs = [
    { from: 0,   to: 90,  fill: SK.accent },
    { from: 90,  to: 200, fill: 'rgba(176,106,58,0.5)' },
    { from: 200, to: 290, fill: 'rgba(31,29,26,0.2)' },
    { from: 290, to: 360, fill: 'rgba(47,73,105,0.25)' },
  ];
  const toXY = (a, R) => [cx + R * Math.cos((a - 90) * Math.PI / 180), cy + R * Math.sin((a - 90) * Math.PI / 180)];
  const ring = arcs.map((a, i) => {
    const [x1, y1] = toXY(a.from, r);
    const [x2, y2] = toXY(a.to, r);
    const large = a.to - a.from > 180 ? 1 : 0;
    return <path key={i} d={`M${cx} ${cy} L${x1} ${y1} A${r} ${r} 0 ${large} 1 ${x2} ${y2} Z`} fill={a.fill} stroke={SK.ink} strokeWidth="1.2" />;
  });
  return (
    <div style={{ width: size, height: size, position: 'relative' }}>
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>{ring}
        <circle cx={cx} cy={cy} r={r * 0.55} fill={SK.paper} stroke={SK.ink} strokeWidth="1.2" />
      </svg>
      {label && <div style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', fontFamily: 'var(--marker)', fontSize: 14, color: SK.muted }}>{label}</div>}
    </div>
  );
}

// ── Demographic pyramid ──────────────────────────────────────────
function SkPyramid({ height = 220, width = '100%', label }) {
  const W = 280, H = height;
  const cx = W / 2;
  const bars = 11;
  const rowH = (H - 28) / bars;
  const seed = 7;
  let s = seed;
  const rnd = () => { s = (s * 9301 + 49297) % 233280; return s / 233280; };
  const widths = Array.from({ length: bars }, (_, i) => {
    const base = 0.85 - i * 0.05;
    return { l: base * (0.7 + rnd() * 0.3), r: base * (0.7 + rnd() * 0.3) };
  });
  const maxBar = (W / 2) - 36;
  return (
    <div style={{ width, position: 'relative' }}>
      <svg viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="none" style={{ width: '100%', height, display: 'block' }}>
        {/* gender labels */}
        <text x={cx - 70} y={14} fontFamily="Caveat" fontSize="14" fill={SK.muted} textAnchor="middle">men</text>
        <text x={cx + 70} y={14} fontFamily="Caveat" fontSize="14" fill={SK.muted} textAnchor="middle">women</text>
        {/* center axis */}
        <line x1={cx} y1={20} x2={cx} y2={H - 4} stroke={SK.ink} strokeWidth="1" />
        {widths.map((w, i) => {
          const y = 22 + i * rowH;
          return (
            <g key={i}>
              <rect x={cx - maxBar * w.l - 2} y={y} width={maxBar * w.l} height={rowH - 3}
                fill={i === 5 ? SK.accent : 'rgba(47,73,105,0.45)'} stroke={SK.ink} strokeWidth="1" />
              <rect x={cx + 2} y={y} width={maxBar * w.r} height={rowH - 3}
                fill={i === 5 ? SK.warn : 'rgba(176,106,58,0.45)'} stroke={SK.ink} strokeWidth="1" />
              {i % 2 === 0 && <text x={W - 4} y={y + rowH * 0.7} fontFamily="JetBrains Mono" fontSize="8" fill={SK.muted} textAnchor="end">{80 - i * 10}+</text>}
            </g>
          );
        })}
      </svg>
      {label && <div style={{ position: 'absolute', top: 4, left: 8, fontFamily: 'var(--marker)', fontSize: 14, color: SK.muted }}>{label}</div>}
    </div>
  );
}

// ── Choropleth (very hand-drawn Europe blob) ─────────────────────
function SkChoropleth({ height = 240, highlight = 'DE', label }) {
  // simplified blobby Europe — clusters of country-ish blobs
  const blobs = [
    { id: 'IS', d: 'M30 38 q8 -10 18 -2 q4 8 -6 14 q-12 4 -16 -4 z', t: 'IS' },
    { id: 'UK', d: 'M60 70 q12 -16 26 -6 q4 18 -8 30 q-22 6 -22 -10 z', t: 'UK' },
    { id: 'IE', d: 'M42 84 q8 -8 18 0 q2 12 -8 14 q-14 2 -12 -10 z', t: 'IE' },
    { id: 'NO', d: 'M120 16 q14 -10 28 4 q8 22 -6 38 q-18 6 -24 -6 q-10 -22 2 -36 z', t: 'NO' },
    { id: 'SE', d: 'M152 30 q14 -6 22 8 q4 32 -10 42 q-18 4 -20 -16 q-2 -22 8 -34 z', t: 'SE' },
    { id: 'FI', d: 'M188 26 q14 -2 18 16 q-2 28 -18 32 q-12 -2 -10 -22 q0 -18 10 -26 z', t: 'FI' },
    { id: 'DK', d: 'M122 86 q10 -6 16 4 q-2 12 -12 12 q-10 -2 -4 -16 z', t: 'DK' },
    { id: 'NL', d: 'M104 110 q10 -4 14 6 q-2 10 -10 12 q-12 -2 -4 -18 z', t: 'NL' },
    { id: 'BE', d: 'M98 130 q8 -4 14 4 q0 10 -10 12 q-10 0 -4 -16 z', t: 'BE' },
    { id: 'DE', d: 'M126 110 q22 -6 28 12 q6 22 -10 32 q-26 4 -28 -16 q-4 -16 10 -28 z', t: 'DE' },
    { id: 'FR', d: 'M84 152 q22 -10 32 6 q6 28 -14 36 q-26 6 -30 -16 q-4 -16 12 -26 z', t: 'FR' },
    { id: 'ES', d: 'M64 200 q18 -10 36 0 q8 22 -10 30 q-32 6 -36 -12 q-4 -10 10 -18 z', t: 'ES' },
    { id: 'PT', d: 'M48 208 q8 -4 12 6 q-2 22 -10 22 q-12 -2 -10 -16 q0 -8 8 -12 z', t: 'PT' },
    { id: 'IT', d: 'M138 162 q12 -2 16 14 q4 28 -14 36 q-12 0 -12 -20 q-2 -22 10 -30 z', t: 'IT' },
    { id: 'CH', d: 'M118 152 q10 -2 14 6 q-2 10 -10 10 q-10 0 -4 -16 z', t: 'CH' },
    { id: 'AT', d: 'M154 150 q14 -2 18 8 q-2 10 -16 12 q-10 0 -2 -20 z', t: 'AT' },
    { id: 'PL', d: 'M164 110 q22 -6 28 8 q4 18 -10 24 q-22 2 -24 -12 q-2 -12 6 -20 z', t: 'PL' },
    { id: 'CZ', d: 'M158 132 q12 -2 16 8 q-2 10 -14 12 q-12 0 -2 -20 z', t: 'CZ' },
    { id: 'HU', d: 'M180 148 q14 -2 18 8 q-2 10 -16 12 q-12 0 -2 -20 z', t: 'HU' },
    { id: 'RO', d: 'M204 148 q18 -2 22 12 q0 14 -16 18 q-18 0 -10 -28 z', t: 'RO' },
    { id: 'GR', d: 'M188 192 q14 -2 18 10 q-2 16 -14 18 q-16 0 -4 -28 z', t: 'GR' },
    { id: 'EE', d: 'M198 76 q10 -2 14 6 q-2 10 -12 12 q-10 0 -2 -18 z', t: 'EE' },
    { id: 'LV', d: 'M198 96 q12 -2 16 8 q-2 10 -14 12 q-12 0 -2 -20 z', t: 'LV' },
    { id: 'LT', d: 'M194 116 q12 -2 16 8 q-2 10 -14 12 q-12 0 -2 -20 z', t: 'LT' },
  ];
  // shade map — derived from seed
  const shade = (id) => {
    const v = Array.from(id).reduce((a, c) => a + c.charCodeAt(0), 0) % 5;
    const opacities = [0.1, 0.25, 0.4, 0.55, 0.7];
    return `rgba(47,73,105,${opacities[v]})`;
  };
  return (
    <div style={{ position: 'relative', width: '100%' }}>
      <svg viewBox="0 0 256 240" style={{ width: '100%', height, display: 'block' }}>
        {blobs.map(b => {
          const m = b.d.match(/M\s*([\d.-]+)\s+([\d.-]+)/);
          const cx = m ? parseFloat(m[1]) + 6 : 0;
          const cy = m ? parseFloat(m[2]) + 14 : 0;
          return (
            <g key={b.id}>
              <path d={b.d} fill={b.id === highlight ? SK.accent : shade(b.id)} stroke={SK.ink} strokeWidth="1" />
              {b.id === highlight && (
                <text x={cx} y={cy} fontFamily="Caveat" fontSize="14" fill={SK.ink}>{b.t}</text>
              )}
            </g>
          );
        })}
      </svg>
      {label && <div style={{ position: 'absolute', top: 4, left: 10, fontFamily: 'var(--marker)', fontSize: 14, color: SK.muted }}>{label}</div>}
    </div>
  );
}

// ── Sankey-ish flow ──────────────────────────────────────────────
function SkSankey({ height = 130, label }) {
  const W = 280, H = height;
  const left = ['DE', 'FR', 'NL'];
  const right = ['US', 'CN', 'UK', 'CH'];
  const flows = [
    { from: 0, to: 0, w: 18 },
    { from: 0, to: 1, w: 12 },
    { from: 1, to: 2, w: 14 },
    { from: 1, to: 0, w: 8 },
    { from: 2, to: 3, w: 10 },
    { from: 2, to: 1, w: 6 },
  ];
  const lx = 36, rx = W - 36;
  const lY = (i) => 20 + i * ((H - 40) / left.length) + ((H - 40) / left.length) / 2;
  const rY = (i) => 20 + i * ((H - 40) / right.length) + ((H - 40) / right.length) / 2;
  return (
    <div className="sk-chart" style={{ width: '100%', height }}>
      <svg viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="none" style={{ width: '100%', height: '100%', display: 'block' }}>
        {flows.map((f, i) => {
          const y1 = lY(f.from), y2 = rY(f.to);
          const d = `M${lx} ${y1} C${(lx + rx) / 2} ${y1}, ${(lx + rx) / 2} ${y2}, ${rx} ${y2}`;
          return <path key={i} d={d} fill="none" stroke={SK.accent} strokeOpacity="0.5" strokeWidth={f.w} strokeLinecap="round" />;
        })}
        {left.map((t, i) => (
          <g key={t}>
            <rect x={lx - 14} y={lY(i) - 12} width={14} height={24} fill={SK.paper} stroke={SK.ink} strokeWidth="1.2" />
            <text x={lx - 18} y={lY(i) + 4} fontFamily="Patrick Hand" fontSize="12" fill={SK.ink} textAnchor="end">{t}</text>
          </g>
        ))}
        {right.map((t, i) => (
          <g key={t}>
            <rect x={rx} y={rY(i) - 12} width={14} height={24} fill={SK.paper} stroke={SK.ink} strokeWidth="1.2" />
            <text x={rx + 18} y={rY(i) + 4} fontFamily="Patrick Hand" fontSize="12" fill={SK.ink} textAnchor="start">{t}</text>
          </g>
        ))}
      </svg>
      {label && <div style={{ position: 'absolute', top: 6, left: 10, fontFamily: 'var(--marker)', fontSize: 14, color: SK.muted }}>{label}</div>}
    </div>
  );
}

// ── Heatmap (seasonality) ────────────────────────────────────────
function SkHeatmap({ rows = 5, cols = 12, label }) {
  const seed = 4;
  let s = seed; const rnd = () => { s = (s * 9301 + 49297) % 233280; return s / 233280; };
  return (
    <div className="sk-chart" style={{ width: '100%', padding: 8, height: 'auto' }}>
      <div style={{ display: 'grid', gridTemplateColumns: `repeat(${cols}, 1fr)`, gap: 3 }}>
        {Array.from({ length: rows * cols }).map((_, i) => {
          const v = rnd();
          const op = 0.08 + v * 0.6;
          return <div key={i} style={{ aspectRatio: '1', background: `rgba(47,73,105,${op})`, border: '1px solid rgba(31,29,26,0.25)' }} />;
        })}
      </div>
      {label && <div style={{ fontFamily: 'var(--marker)', fontSize: 12, color: SK.muted, marginTop: 4 }}>{label}</div>}
    </div>
  );
}

// ── Flag placeholder (mini striped square) ───────────────────────
function SkFlag({ code }) {
  // hand-drawn three-band flag — colors irrelevant in wireframe (kept neutral)
  return (
    <div style={{ width: 26, height: 18, position: 'relative', flexShrink: 0 }}>
      <div style={{ position: 'absolute', inset: 0, border: '1.2px solid var(--ink)', borderRadius: 2, overflow: 'hidden', display: 'flex' }}>
        <div style={{ flex: 1, background: SK.paper2 }} />
        <div style={{ flex: 1, background: SK.muted2 }} />
        <div style={{ flex: 1, background: SK.paper }} />
      </div>
      <div style={{ position: 'absolute', bottom: -10, left: 0, right: 0, textAlign: 'center', fontFamily: 'JetBrains Mono', fontSize: 8, color: SK.muted }}>{code}</div>
    </div>
  );
}

// ── Stat tile ────────────────────────────────────────────────────
function SkStat({ label, value, delta, big, accent }) {
  return (
    <div style={{ flex: 1, minWidth: 0 }}>
      <div style={{ fontFamily: 'var(--hand)', fontSize: 12, color: SK.muted }}>{label}</div>
      <div className="sk-num" style={{ fontSize: big ? 28 : 20, color: accent ? SK.accent : SK.ink, lineHeight: 1.1, marginTop: 2 }}>{value}</div>
      {delta && <div style={{ fontFamily: 'var(--mono)', fontSize: 11, color: SK.muted, marginTop: 2 }}>{delta}</div>}
    </div>
  );
}

// ── Status-bar overlay tint helpers ──────────────────────────────
// We don't render status bars — the device frames do. Our content goes
// inside `children` of the frame.

Object.assign(window, {
  SK, SkIcon, SkBox, SkTag, SkPill, SkBtn, SkTabBar,
  SkLineChart, SkBars, SkArea, SkDonut, SkPyramid, SkChoropleth, SkSankey, SkHeatmap,
  SkFlag, SkStat,
});
