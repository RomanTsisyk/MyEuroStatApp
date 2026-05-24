/* eslint-disable */
// Shared bits for the 8 feature-module screens.

const MOD_META = {
  Population:  { accent: '#5e6b58', tabIcon: 'people', dataset: 'demo_pjan',           year: '2024', tagline: 'demography · age · sex' },
  Economy:     { accent: '#2f4969', tabIcon: 'coin',   dataset: 'nama_10_gdp · +2',    year: '2024', tagline: 'GDP · inflation · deficit' },
  Environment: { accent: '#6b8a76', tabIcon: 'leaf',   dataset: 'env_air_gge · +2',    year: '2022', tagline: 'emissions · energy · SDG' },
  Trade:       { accent: '#7a5c46', tabIcon: 'globe',  dataset: 'ext_lt_intratrd',     year: '2024', tagline: 'intra-EU exports & imports' },
  Transport:   { accent: '#4a4a55', tabIcon: 'dots',   dataset: 'road_pa_buscoa · +1', year: '2022', tagline: 'road · air · (sea disabled)' },
  Tourism:     { accent: '#b06a3a', tabIcon: 'star',   dataset: 'tour_occ_ninat · +1', year: '2024', tagline: 'nights · trips · residence' },
  Social:      { accent: '#7d5e76', tabIcon: 'people', dataset: 'ilc_li02 · +2',       year: '2023', tagline: 'poverty · at-risk · health' },
  Science:     { accent: '#4a6b7a', tabIcon: 'star',   dataset: 'rd_e_gerdtot · +2',   year: '2023', tagline: 'R&D · internet · education' },
};

const MOD_ORDER = ['Population', 'Economy', 'Environment', 'Trade', 'Transport', 'Tourism', 'Social', 'Science'];

// Top tab strip — emulates Compose's ScrollableTabRow.
function ModuleTabStrip({ active }) {
  const meta = MOD_META[active];
  return (
    <div style={{ marginTop: 4, position: 'relative' }}>
      <div style={{
        display: 'flex', gap: 6, whiteSpace: 'nowrap', overflow: 'hidden',
        paddingLeft: 14, paddingRight: 14,
        maskImage: 'linear-gradient(to right, transparent 0, black 16px, black calc(100% - 24px), transparent 100%)',
        WebkitMaskImage: 'linear-gradient(to right, transparent 0, black 16px, black calc(100% - 24px), transparent 100%)',
      }}>
        {MOD_ORDER.map(m => {
          const isActive = m === active;
          return (
            <div key={m} style={{
              padding: '3px 10px',
              border: '1.5px solid var(--ink)',
              borderRadius: 999,
              background: isActive ? meta.accent : SK.paper,
              color: isActive ? SK.paper : SK.ink,
              fontFamily: 'var(--hand)',
              fontSize: 12,
              flexShrink: 0,
              lineHeight: 1.3,
            }}>{m}</div>
          );
        })}
      </div>
      <div style={{ height: 0, borderBottom: '1.5px dashed var(--ink)', marginTop: 6, opacity: 0.4 }} />
    </div>
  );
}

// Top app bar — short.
function ModuleAppBar({ name, accent }) {
  return (
    <div style={{ padding: '6px 14px 0', display: 'flex', alignItems: 'center', gap: 8 }}>
      <SkIcon name="chev-l" size={18} />
      <div style={{ fontFamily: 'var(--marker)', fontSize: 22, lineHeight: 1 }}>{name}</div>
      <span style={{ width: 6, height: 6, borderRadius: 3, background: accent, marginLeft: 2 }} />
      <div style={{ flex: 1 }} />
      <SkIcon name="search" size={18} />
      <div style={{ width: 6 }} />
      <SkIcon name="refresh" size={18} />
    </div>
  );
}

// Country chips — common to every module.
function CountryChips({ countries = ['DE', 'FR', 'PL', 'EU27'], active = 'DE' }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 5, padding: '8px 14px 0', flexWrap: 'wrap' }}>
      {countries.map(c => {
        const isActive = c === active;
        return (
          <div key={c} style={{
            display: 'flex', alignItems: 'center', gap: 4,
            padding: '3px 8px',
            border: '1.5px solid var(--ink)',
            borderRadius: 999,
            background: isActive ? SK.ink : SK.paper,
            color: isActive ? SK.paper : SK.ink,
            fontFamily: 'var(--hand)', fontSize: 12, lineHeight: 1.2,
          }}>
            <span style={{ width: 14, height: 10, background: 'var(--paper-2)', border: `1px solid ${isActive ? SK.paper : SK.ink}`, opacity: isActive ? 0.7 : 1 }} />
            {c}
          </div>
        );
      })}
      <div style={{
        padding: '3px 8px', border: '1.5px dashed var(--ink)', borderRadius: 999,
        fontFamily: 'var(--hand)', fontSize: 12, color: SK.muted, display: 'flex', alignItems: 'center', gap: 3,
      }}>+ <span>add</span></div>
    </div>
  );
}

// Year range slider — compact.
function YearScrubber({ from = 2010, to = 2024 }) {
  return (
    <div style={{ padding: '8px 14px 0' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', fontFamily: 'var(--mono)', fontSize: 10, color: SK.muted, marginBottom: 4 }}>
        <span>2000</span><span className="sk-num" style={{ fontSize: 11, color: SK.ink }}>{from} — {to}</span><span>'24</span>
      </div>
      <div style={{ height: 16, position: 'relative' }}>
        <div style={{ position: 'absolute', left: 0, right: 0, top: 7, height: 2, background: SK.muted2 }} />
        <div style={{ position: 'absolute', left: '42%', right: '0%', top: 7, height: 2, background: SK.ink }} />
        <div style={{ position: 'absolute', left: '42%', top: 1, width: 12, height: 12, borderRadius: 6, background: SK.paper, border: '1.5px solid var(--ink)' }} />
        <div style={{ position: 'absolute', right: '0%', top: 1, width: 12, height: 12, borderRadius: 6, background: SK.ink, border: '1.5px solid var(--ink)' }} />
      </div>
    </div>
  );
}

// Bottom: a per-module tab bar customised to highlight the module's quadrant.
// We reuse the 5-tab pattern but show the active group.
function FeatTabBar({ active = 'Overview' }) { return <SkTabBar active={active} />; }

// Source footer.
function SourceFooter({ dataset, isStale = false }) {
  return (
    <div style={{ padding: '6px 14px 6px', fontFamily: 'var(--mono)', fontSize: 9, color: SK.muted2, display: 'flex', alignItems: 'center', gap: 6 }}>
      <span style={{ width: 6, height: 6, borderRadius: 3, background: isStale ? SK.warn : SK.muted2 }} />
      <span>eurostat · {dataset} · {isStale ? 'cached 14h ago' : 'fresh · 4h ago'}</span>
    </div>
  );
}

// Stale banner.
function StaleBanner() {
  return (
    <div style={{
      margin: '6px 14px 0', padding: '4px 8px',
      border: `1.5px dashed ${SK.warn}`, borderRadius: 8,
      background: 'rgba(176,106,58,0.08)',
      fontFamily: 'var(--hand)', fontSize: 11, color: SK.warn, lineHeight: 1.3,
    }}>
      showing cached data · refreshing…
    </div>
  );
}

// Sketchy segmented control — used by Economy.
function SkSegmented({ items, active, accent }) {
  return (
    <div style={{ display: 'flex', border: '1.5px solid var(--ink)', borderRadius: 8, padding: 2, background: SK.paper }}>
      {items.map((it, i) => {
        const isActive = it === active;
        return (
          <div key={it} style={{
            flex: 1, textAlign: 'center', padding: '5px 4px',
            background: isActive ? (accent || SK.ink) : 'transparent',
            color: isActive ? SK.paper : SK.ink,
            borderRadius: 6,
            fontFamily: 'var(--hand)', fontSize: 12, lineHeight: 1.2,
          }}>{it}</div>
        );
      })}
    </div>
  );
}

// Chip row — used by Environment.
function SkChipRow({ items, active, accent }) {
  return (
    <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
      {items.map(it => {
        const isActive = it === active;
        return (
          <div key={it} style={{
            padding: '4px 10px',
            border: `1.5px solid ${isActive ? (accent || SK.ink) : SK.muted2}`,
            borderRadius: 999,
            background: isActive ? (accent || SK.ink) : SK.paper,
            color: isActive ? SK.paper : SK.ink,
            fontFamily: 'var(--hand)', fontSize: 12, lineHeight: 1.2,
          }}>{it}</div>
        );
      })}
    </div>
  );
}

// Underline-tab row — used by Trade.
function SkTabs({ items, active, accent }) {
  return (
    <div style={{ display: 'flex', gap: 14, borderBottom: '1.5px solid var(--ink)' }}>
      {items.map(it => {
        const isActive = it === active;
        return (
          <div key={it} style={{
            padding: '4px 0 6px',
            position: 'relative',
            fontFamily: 'var(--hand)', fontSize: 13,
            color: isActive ? SK.ink : SK.muted,
            fontWeight: isActive ? 600 : 400,
          }}>
            {it}
            {isActive && <div style={{ position: 'absolute', left: 0, right: 0, bottom: -1, height: 3, background: accent || SK.ink, borderRadius: 2 }} />}
          </div>
        );
      })}
    </div>
  );
}

// Dropdown pill — used by Environment (metric picker).
function SkDropdown({ label, value }) {
  return (
    <div style={{
      display: 'inline-flex', alignItems: 'center', gap: 4,
      padding: '3px 8px',
      border: '1.5px solid var(--ink)', borderRadius: 8,
      background: SK.paper, fontFamily: 'var(--hand)', fontSize: 11,
    }}>
      <span style={{ color: SK.muted }}>{label}</span>
      <span>{value}</span>
      <SkIcon name="chev-d" size={12} />
    </div>
  );
}

// Pill-toggle — used by Transport (ROAD | AIR).
function SkPillToggle({ items, active, accent }) {
  return (
    <div style={{ display: 'inline-flex', border: '1.5px solid var(--ink)', borderRadius: 999, padding: 2, background: SK.paper }}>
      {items.map(it => {
        const isActive = it === active;
        return (
          <div key={it} style={{
            padding: '3px 12px',
            background: isActive ? (accent || SK.ink) : 'transparent',
            color: isActive ? SK.paper : SK.ink,
            borderRadius: 999,
            fontFamily: 'var(--hand)', fontSize: 12, lineHeight: 1.2,
          }}>{it}</div>
        );
      })}
    </div>
  );
}

// Three-tile selector — used by Social.
function SkTileSelector({ items, active, accent }) {
  return (
    <div style={{ display: 'grid', gridTemplateColumns: `repeat(${items.length}, 1fr)`, gap: 6 }}>
      {items.map(it => {
        const isActive = it.id === active;
        return (
          <div key={it.id} style={{
            padding: '6px 8px',
            border: `1.5px solid ${isActive ? (accent || SK.ink) : SK.muted2}`,
            borderRadius: 8,
            background: isActive ? 'rgba(125,94,118,0.12)' : SK.paper,
            position: 'relative',
          }}>
            <div style={{ fontFamily: 'var(--hand)', fontSize: 11, color: SK.muted, lineHeight: 1.1 }}>{it.label}</div>
            <div className="sk-num" style={{ fontSize: 17, marginTop: 2, color: isActive ? (accent || SK.ink) : SK.ink, lineHeight: 1 }}>{it.value}</div>
            <div style={{ fontFamily: 'var(--mono)', fontSize: 9, color: SK.muted, marginTop: 1 }}>{it.unit}</div>
            {isActive && <span style={{ position: 'absolute', top: 4, right: 4, width: 6, height: 6, borderRadius: 3, background: accent || SK.ink }} />}
          </div>
        );
      })}
    </div>
  );
}

// Radar chart — used by Science.
function SkRadar({ size = 220, metrics = ['R&D', 'Internet', 'Education'], series, label }) {
  const cx = size / 2, cy = size / 2 + 8;
  const r = size / 2 - 32;
  const n = metrics.length;
  const angleFor = (i) => (i / n) * Math.PI * 2 - Math.PI / 2;
  const ringPath = (frac) => {
    let d = '';
    for (let i = 0; i <= n; i++) {
      const a = angleFor(i % n);
      const x = cx + Math.cos(a) * r * frac;
      const y = cy + Math.sin(a) * r * frac;
      d += (i === 0 ? 'M' : 'L') + x.toFixed(1) + ' ' + y.toFixed(1) + ' ';
    }
    return d + 'Z';
  };
  const seriesPath = (vals) => {
    let d = '';
    for (let i = 0; i < n; i++) {
      const a = angleFor(i);
      const x = cx + Math.cos(a) * r * vals[i];
      const y = cy + Math.sin(a) * r * vals[i];
      d += (i === 0 ? 'M' : 'L') + x.toFixed(1) + ' ' + y.toFixed(1) + ' ';
    }
    return d + 'Z';
  };
  return (
    <div style={{ width: '100%', position: 'relative' }}>
      <svg viewBox={`0 0 ${size} ${size + 12}`} style={{ width: '100%', display: 'block' }}>
        {[0.25, 0.5, 0.75, 1].map(f => (
          <path key={f} d={ringPath(f)} fill="none" stroke={SK.ink} strokeOpacity={f === 1 ? 0.4 : 0.18} strokeDasharray="2 3" strokeWidth="1" />
        ))}
        {metrics.map((m, i) => {
          const a = angleFor(i);
          const x2 = cx + Math.cos(a) * r;
          const y2 = cy + Math.sin(a) * r;
          const lx = cx + Math.cos(a) * (r + 14);
          const ly = cy + Math.sin(a) * (r + 14);
          return (
            <g key={m}>
              <line x1={cx} y1={cy} x2={x2} y2={y2} stroke={SK.ink} strokeOpacity={0.25} strokeWidth="1" />
              <text x={lx} y={ly + 4} fontFamily="Patrick Hand" fontSize="11" fill={SK.ink} textAnchor="middle">{m}</text>
            </g>
          );
        })}
        {series.map((s, i) => (
          <g key={i}>
            <path d={seriesPath(s.vals)} fill={s.color} fillOpacity="0.18" stroke={s.color} strokeWidth="2" strokeLinejoin="round" />
            {s.vals.map((v, j) => {
              const a = angleFor(j);
              const x = cx + Math.cos(a) * r * v;
              const y = cy + Math.sin(a) * r * v;
              return <circle key={j} cx={x} cy={y} r="3" fill={SK.paper} stroke={s.color} strokeWidth="1.5" />;
            })}
          </g>
        ))}
      </svg>
      {label && <div style={{ position: 'absolute', top: 4, left: 8, fontFamily: 'var(--marker)', fontSize: 13, color: SK.muted }}>{label}</div>}
    </div>
  );
}

// Diverging bar chart — used by Trade.
function SkDivergingBars({ height = 160, label, accent = SK.accent, warn = SK.warn }) {
  const W = 280, H = height;
  const inner = { l: 28, r: 8, t: 16, b: 24 };
  const n = 8;
  const seed = (s) => { let x = s; return () => { x = (x * 9301 + 49297) % 233280; return x / 233280; }; };
  const r = seed(33);
  const exp = Array.from({ length: n }, () => 0.4 + r() * 0.5);
  const imp = Array.from({ length: n }, () => 0.35 + r() * 0.45);
  const baseline = inner.t + (H - inner.t - inner.b) / 2;
  const half = (H - inner.t - inner.b) / 2 - 2;
  const bw = (W - inner.l - inner.r) / n - 3;
  return (
    <div className="sk-chart" style={{ width: '100%', height }}>
      <svg viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="none" style={{ width: '100%', height: '100%', display: 'block' }}>
        <line x1={inner.l} y1={baseline} x2={W - inner.r} y2={baseline} stroke={SK.ink} strokeWidth="1.2" />
        <text x={6} y={baseline - half + 4} fontFamily="Caveat" fontSize="11" fill={SK.muted}>exp</text>
        <text x={6} y={baseline + half - 2} fontFamily="Caveat" fontSize="11" fill={SK.muted}>imp</text>
        {exp.map((v, i) => {
          const x = inner.l + i * ((W - inner.l - inner.r) / n) + 2;
          return <rect key={'e' + i} x={x} y={baseline - half * v} width={bw} height={half * v} fill={accent} fillOpacity="0.7" stroke={SK.ink} strokeWidth="1" />;
        })}
        {imp.map((v, i) => {
          const x = inner.l + i * ((W - inner.l - inner.r) / n) + 2;
          return <rect key={'i' + i} x={x} y={baseline} width={bw} height={half * v} fill={warn} fillOpacity="0.55" stroke={SK.ink} strokeWidth="1" />;
        })}
        <g fontFamily="JetBrains Mono" fontSize="8" fill={SK.muted}>
          {[0, 3, 7].map(i => {
            const x = inner.l + i * ((W - inner.l - inner.r) / n) + bw / 2;
            return <text key={i} x={x} y={H - 6} textAnchor="middle">{`'${(17 + i).toString().padStart(2, '0')}`}</text>;
          })}
        </g>
      </svg>
      {label && <div style={{ position: 'absolute', top: 4, left: 10, fontFamily: 'var(--marker)', fontSize: 13, color: SK.muted }}>{label}</div>}
    </div>
  );
}

// Stacked bars — used by Tourism.
function SkStackedBars({ height = 130, label, accent = SK.accent }) {
  const W = 280, H = height;
  const inner = { l: 26, r: 8, t: 14, b: 22 };
  const n = 9;
  const seed = 17;
  let s = seed; const rnd = () => { s = (s * 9301 + 49297) % 233280; return s / 233280; };
  const dom = Array.from({ length: n }, () => 0.3 + rnd() * 0.3);
  const fgn = Array.from({ length: n }, () => 0.25 + rnd() * 0.4);
  const max = Math.max(...dom.map((d, i) => d + fgn[i]));
  const bw = (W - inner.l - inner.r) / n - 3;
  return (
    <div className="sk-chart" style={{ width: '100%', height }}>
      <svg viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="none" style={{ width: '100%', height: '100%', display: 'block' }}>
        {dom.map((d, i) => {
          const f = fgn[i];
          const usable = H - inner.t - inner.b;
          const hd = (d / max) * usable;
          const hf = (f / max) * usable;
          const x = inner.l + i * ((W - inner.l - inner.r) / n) + 2;
          const y0 = H - inner.b;
          return (
            <g key={i}>
              <rect x={x} y={y0 - hd} width={bw} height={hd} fill={accent} fillOpacity="0.7" stroke={SK.ink} strokeWidth="1" />
              <rect x={x} y={y0 - hd - hf} width={bw} height={hf} fill={accent} fillOpacity="0.3" stroke={SK.ink} strokeWidth="1" />
            </g>
          );
        })}
        <line x1={inner.l} y1={H - inner.b} x2={W - inner.r} y2={H - inner.b} stroke={SK.ink} strokeWidth="1" />
        <g fontFamily="JetBrains Mono" fontSize="8" fill={SK.muted}>
          {[0, 4, 8].map(i => {
            const x = inner.l + i * ((W - inner.l - inner.r) / n) + bw / 2;
            return <text key={i} x={x} y={H - 6} textAnchor="middle">{`'${(16 + i).toString().padStart(2, '0')}`}</text>;
          })}
        </g>
      </svg>
      {label && <div style={{ position: 'absolute', top: 4, left: 10, fontFamily: 'var(--marker)', fontSize: 13, color: SK.muted }}>{label}</div>}
    </div>
  );
}

// Small-multiples panel — used by Transport (ROAD + AIR side-by-side).
function SkSmallMultiples({ height = 140, panels = [{ label: 'ROAD bn', accent: SK.accent }, { label: 'AIR mn', accent: SK.warn }] }) {
  return (
    <div style={{ display: 'grid', gridTemplateColumns: `repeat(${panels.length}, 1fr)`, gap: 8 }}>
      {panels.map((p, i) => (
        <div key={i} style={{ position: 'relative' }}>
          <SkLineChart height={height} accent={p.accent} hideAxis />
          <div style={{ position: 'absolute', top: 6, left: 8, fontFamily: 'var(--marker)', fontSize: 12, color: SK.muted }}>{p.label}</div>
        </div>
      ))}
    </div>
  );
}

// Multi-line with one highlighted — used by Social.
function SkMultiLineHighlighted({ height = 130, highlightIdx = 0 }) {
  const W = 280, H = height;
  const inner = { l: 26, r: 8, t: 14, b: 22 };
  const seed = (s) => { let x = s; return () => { x = (x * 9301 + 49297) % 233280; return x / 233280; }; };
  const makePath = (s, baseY) => {
    const rnd = seed(s);
    const n = 12;
    let d = '';
    for (let i = 0; i < n; i++) {
      const x = inner.l + (W - inner.l - inner.r) * (i / (n - 1));
      const y = baseY + (rnd() - 0.5) * 16 - i * 0.4;
      d += (i === 0 ? 'M' : 'L') + x.toFixed(1) + ' ' + y.toFixed(1) + ' ';
    }
    return d;
  };
  const lines = [
    { d: makePath(31, 38), color: '#7d5e76' },
    { d: makePath(57, 60), color: SK.muted2 },
    { d: makePath(91, 88), color: SK.muted2 },
  ];
  return (
    <div className="sk-chart" style={{ width: '100%', height }}>
      <svg viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="none" style={{ width: '100%', height: '100%', display: 'block' }}>
        {[0.33, 0.66].map(g => <line key={g} x1={inner.l} x2={W - inner.r} y1={inner.t + (H - inner.t - inner.b) * g} y2={inner.t + (H - inner.t - inner.b) * g} stroke={SK.ink} strokeOpacity="0.12" strokeDasharray="2 3" />)}
        {lines.map((l, i) => (
          <path key={i} d={l.d}
            stroke={i === highlightIdx ? l.color : SK.muted2}
            strokeWidth={i === highlightIdx ? 2.4 : 1.2}
            strokeOpacity={i === highlightIdx ? 1 : 0.6}
            fill="none" strokeLinecap="round" strokeLinejoin="round" />
        ))}
        <line x1={inner.l} y1={H - inner.b} x2={W - inner.r} y2={H - inner.b} stroke={SK.ink} strokeWidth="1" />
      </svg>
    </div>
  );
}

Object.assign(window, {
  MOD_META, MOD_ORDER,
  ModuleTabStrip, ModuleAppBar, CountryChips, YearScrubber, FeatTabBar, SourceFooter, StaleBanner,
  SkSegmented, SkChipRow, SkTabs, SkDropdown, SkPillToggle, SkTileSelector,
  SkRadar, SkDivergingBars, SkStackedBars, SkSmallMultiples, SkMultiLineHighlighted,
});
